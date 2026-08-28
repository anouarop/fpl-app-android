// Shared helpers for the FPL name-search service and its crawler.
// Zero runtime dependencies unless DATABASE_URL is set, in which case `pg` is used.

'use strict';

const fs = require('fs');
const https = require('https');
const zlib = require('zlib');
const dns = require('dns');

// Some hosts (e.g. Render) have no IPv6 egress. When a Postgres hostname
// resolves to both families, force IPv4 first so `pg`/net.connect doesn't
// pick an unreachable IPv6 address (ENETUNREACH).
dns.setDefaultResultOrder('ipv4first');

/** Lowercase + strip diacritics so "Álvarez" matches "alvarez". */
function normalize(s) {
  return String(s || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

/** Fetch JSON over HTTPS (gzip-aware). Rejects on network/parse errors. */
function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https
      .get(
        url,
        {
          headers: {
            'User-Agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36',
            'Accept-Encoding': 'gzip',
          },
        },
        (res) => {
          const chunks = [];
          res.on('data', (c) => chunks.push(c));
          res.on('end', () => {
            try {
              let buf = Buffer.concat(chunks);
              if (res.headers['content-encoding'] === 'gzip') buf = zlib.gunzipSync(buf);
              resolve(JSON.parse(buf.toString('utf8')));
            } catch (e) {
              reject(e);
            }
          });
        },
      )
      .on('error', reject);
  });
}

/**
 * Load the manager index from disk, tolerating a missing/corrupt file. Returns
 * `{ managers, meta }` where meta defaults to `{ crawledPage: 0 }`.
 */
function loadDb(dataFile) {
  try {
    const parsed = JSON.parse(fs.readFileSync(dataFile, 'utf8'));
    return {
      managers: Array.isArray(parsed.managers) ? parsed.managers : [],
      meta: parsed.meta && Number(parsed.meta.crawledPage) > 0 ? parsed.meta : { crawledPage: 0 },
    };
  } catch {
    return { managers: [], meta: { crawledPage: 0 } };
  }
}

/** Sort managers by overall rank ascending in place; unknown ranks sort last. */
function sortManagers(managers) {
  managers.sort((a, b) => (a.rank || 1e9) - (b.rank || 1e9));
  return managers;
}

/**
 * Persist `db` to `dataFile` atomically (write a temp file, then rename over the
 * target) using compact JSON. Returns the new mtime so callers can keep their
 * freshness watermark in sync.
 */
function saveDb(db, dataFile) {
  const tmp = `${dataFile}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify(db));
  fs.renameSync(tmp, dataFile);
  return fs.statSync(dataFile).mtimeMs;
}

// ---------------------------------------------------------------------------
// Optional Postgres storage (used when DATABASE_URL is set, e.g. Supabase free
// tier). This survives redeploys, unlike the ephemeral Render disk.
// ---------------------------------------------------------------------------

let _pool = null;
let _poolInit = false;

// Resolved at startup by prepareStorage(): the DATABASE_URL with its host
// rewritten to an IPv4 literal, so `pg`/net.connect never attempts the
// unreachable IPv6 address (Render has no IPv6 egress -> ENETUNREACH).
let _connectionString = null;
// Set true when DATABASE_URL is configured but Supabase is unreachable, so we
// transparently fall back to the bundled data.json instead of erroring.
let _useFile = false;

// Quick TCP probe: can we actually reach the DB host:5432? Used to decide
// between Postgres and the local file fallback on IPv6-only hosts (Render).
function probeReachable(host, port) {
  const net = require('net');
  return new Promise((resolve) => {
    const sock = net.connect({ host, port });
    let done = false;
    const finish = (ok) => {
      if (!done) {
        done = true;
        sock.destroy();
        resolve(ok);
      }
    };
    sock.setTimeout(4000);
    sock.on('connect', () => finish(true));
    sock.on('timeout', () => finish(false));
    sock.on('error', () => finish(false));
  });
}

/**
 * Decide storage backend at startup. If DATABASE_URL is set, resolve the host
 * to IPv4 and probe reachability; if unreachable (e.g. Supabase only has IPv6
 * and Render has no IPv6 egress), fall back to the local data.json file.
 */
async function prepareStorage() {
  const url = process.env.DATABASE_URL;
  if (!url) return; // no DB configured -> file fallback
  const host = new URL(url).hostname;
  let ipv4 = null;
  try {
    ipv4 = await new Promise((resolve, reject) =>
      dns.lookup(host, { family: 4 }, (err, address) =>
        err ? reject(err) : resolve(address),
      ),
    );
  } catch (_) {
    /* no A record — likely IPv6-only */
  }
  const target = ipv4 || host;
  const reachable = await probeReachable(target, 5432);
  if (reachable) {
    const u = new URL(url);
    u.hostname = target;
    _connectionString = u.toString();
    console.log(`using Postgres at ${target}`);
  } else {
    _useFile = true;
    console.log(
      `Supabase unreachable (${target}) — Render has no IPv6 egress; falling back to local data.json`,
    );
  }
}

/** Returns a pg Pool when DATABASE_URL is configured & reachable, else null (file fallback). */
function getPool() {
  if (_poolInit) return _pool;
  _poolInit = true;
  if (_useFile) {
    _pool = null;
    return null;
  }
  const url = _connectionString || process.env.DATABASE_URL;
  if (!url) {
    _pool = null;
    return null;
  }
  try {
    const { Pool } = require('pg');
    _pool = new Pool({
      connectionString: url,
      ssl: { rejectUnauthorized: false },
      max: 5,
      // Fail fast instead of hanging if Supabase is unreachable (a hung
      // connection at startup would block server.listen and cause a 502).
      connectionTimeoutMillis: 5000,
      // Abort stalled queries (e.g. free-tier connection limits) instead of
      // hanging the request handler forever.
      options: '-c statement_timeout=8000 -c idle_in_transaction_session_timeout=8000',
    });
    // Never let a transient Postgres error crash the whole process.
    _pool.on('error', (e) => console.log('pg pool error:', e.message));
  } catch (e) {
    console.log('pg unavailable, falling back to file storage:', e.message);
    _pool = null;
  }
  return _pool;
}

/** Create the tables if they don't exist. Safe to call on every startup. */
async function ensureSchema() {
  const pool = getPool();
  if (!pool) return;
  await pool.query(`
    CREATE TABLE IF NOT EXISTS managers (
      team_id INTEGER PRIMARY KEY,
      manager_name TEXT NOT NULL DEFAULT '',
      team_name TEXT NOT NULL DEFAULT '',
      rank INTEGER NOT NULL DEFAULT 0
    )
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS crawl_meta (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL DEFAULT ''
    )
  `);
}

/** Load the full index from Postgres, or from disk when no DATABASE_URL is set. */
async function dbLoadAll(dataFile) {
  const pool = getPool();
  if (pool) {
    const { rows } = await pool.query(
      'SELECT team_id AS "teamId", manager_name AS "managerName", team_name AS "teamName", rank FROM managers',
    );
    let crawledPage = 0;
    try {
      const r = await pool.query("SELECT value FROM crawl_meta WHERE key = 'crawledPage'");
      if (r.rows[0]) crawledPage = Number(r.rows[0].value) || 0;
    } catch {
      /* no meta yet */
    }
    return { managers: rows, meta: { crawledPage } };
  }
  return loadDb(dataFile);
}

/** Batch upsert managers (max 500/query) into Postgres. No-op on file fallback. */
async function dbUpsert(managers) {
  const pool = getPool();
  if (!pool || !managers || managers.length === 0) return;
  const BATCH = 500;
  for (let i = 0; i < managers.length; i += BATCH) {
    const chunk = managers.slice(i, i + BATCH);
    const values = [];
    const placeholders = [];
    let p = 1;
    for (const m of chunk) {
      placeholders.push(`($${p}, $${p + 1}, $${p + 2}, $${p + 3})`);
      values.push(m.teamId, m.managerName || '', m.teamName || '', m.rank || 0);
      p += 4;
    }
    const sql = `
      INSERT INTO managers (team_id, manager_name, team_name, rank) VALUES ${placeholders.join(',')}
      ON CONFLICT (team_id) DO UPDATE SET
        manager_name = EXCLUDED.manager_name,
        team_name = EXCLUDED.team_name,
        rank = EXCLUDED.rank
    `;
    await pool.query(sql, values);
  }
}

/** Persist crawl progress (last crawled page). No-op on file fallback. */
async function dbSaveMeta(page) {
  const pool = getPool();
  if (!pool) return;
  await pool.query(
    `INSERT INTO crawl_meta (key, value) VALUES ('crawledPage', $1)
     ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value`,
    [String(page)],
  );
}

module.exports = {
  normalize,
  fetchJson,
  loadDb,
  sortManagers,
  saveDb,
  getPool,
  prepareStorage,
  ensureSchema,
  dbLoadAll,
  dbUpsert,
  dbSaveMeta,
};
