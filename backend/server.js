// FPL name-search service — maps manager/team names -> FPL team IDs.
// Zero runtime dependencies. Serves /search, /register, /health and, by default,
// also crawls the public FPL "Overall" league in the background to grow the index
// (so a single deployed process is fully self-sufficient).
//
// Endpoints:
//   GET  /health              -> { ok, managers, crawledPage }
//   GET  /search?q=&limit=    -> { results: [{ teamId, managerName, teamName, rank }] }
//   POST /register            -> body { teamId, managerName, teamName }  (upsert)
//
// Env:
//   PORT        (default 8080)
//   DATA_FILE   (default ./data.json)
//   CRAWL       set to "off" to disable the background crawler
//   CRAWL_DELAY_MS (default 250) delay between standings pages

const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { URL } = require('url');

const PORT = Number(process.env.PORT) || 8080;
const DATA_FILE = process.env.DATA_FILE || path.join(__dirname, 'data.json');
const CRAWL_ENABLED = process.env.CRAWL !== 'off';
const CRAWL_DELAY_MS = Number(process.env.CRAWL_DELAY_MS) || 250;
const OVERALL_LEAGUE_ID = 314;

let db = { managers: [], meta: { crawledPage: 0 } };
let loadedMtimeMs = 0;

function loadFromDisk() {
  try {
    const raw = fs.readFileSync(DATA_FILE, 'utf8');
    const parsed = JSON.parse(raw);
    const managers = Array.isArray(parsed.managers) ? parsed.managers : [];
    // If no crawl metadata exists (e.g. an index built by crawl.js), estimate it.
    let crawledPage = parsed.meta && Number(parsed.meta.crawledPage) > 0 ? Number(parsed.meta.crawledPage) : Math.round(managers.length / 50);
    db = { managers, meta: { crawledPage } };
    loadedMtimeMs = fs.statSync(DATA_FILE).mtimeMs;
  } catch {
    db = { managers: [], meta: { crawledPage: 0 } };
  }
}

function ensureFresh() {
  try {
    const mtime = fs.statSync(DATA_FILE).mtimeMs;
    if (mtime !== loadedMtimeMs) loadFromDisk();
  } catch {
    /* keep current in-memory data */
  }
}

function save() {
  fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2));
  loadedMtimeMs = fs.statSync(DATA_FILE).mtimeMs;
}

function normalize(s) {
  return String(s || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

function score(m, q) {
  const name = normalize(m.managerName);
  const team = normalize(m.teamName);
  const words = q.split(/\s+/).filter(Boolean);

  if (name === q) return 100;
  if (name.startsWith(q)) return 90;
  if (name.includes(q)) return 80;
  if (team === q) return 70;
  if (team.startsWith(q)) return 60;
  if (team.includes(q)) return 50;
  if (words.length >= 2 && words.every((w) => name.includes(w))) return 75;
  if (words.some((w) => name.includes(w))) return 65;
  if (words.some((w) => team.includes(w))) return 40;
  return -1;
}

function send(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  });
  res.end(body);
}

function readBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', (c) => (body += c));
    req.on('end', () => resolve(body));
  });
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

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

let crawling = false;

async function crawlOnce() {
  if (crawling) return;
  crawling = true;
  try {
    let page = db.meta.crawledPage + 1;
    let added = 0;
    for (;; page++) {
      const url = `https://fantasy.premierleague.com/api/leagues-classic/${OVERALL_LEAGUE_ID}/standings/?page_standings=${page}`;
      let rows;
      let hasNext;
      try {
        const json = await fetchJson(url);
        rows = (json && json.standings && json.standings.results) || [];
        hasNext = !!(json && json.standings && json.standings.has_next);
      } catch (e) {
        console.log(`crawl page ${page} failed: ${e.message}`);
        break;
      }
      if (rows.length === 0) break;
      const map = new Map(db.managers.map((m) => [m.teamId, m]));
      for (const r of rows) {
        if (!r.entry) continue;
        map.set(r.entry, {
          teamId: r.entry,
          managerName: r.player_name || '',
          teamName: r.entry_name || '',
          rank: r.rank || 0,
        });
        added++;
      }
      db.managers = Array.from(map.values()).sort((a, b) => (a.rank || 1e9) - (b.rank || 1e9));
      db.meta.crawledPage = page;
      if (page % 50 === 0) save();
      if (!hasNext) {
        db.meta.crawledPage = page;
        save();
        break;
      }
      await sleep(CRAWL_DELAY_MS);
    }
    console.log(`crawl pass done at page ${db.meta.crawledPage} (${db.managers.length} managers)`);
  } finally {
    crawling = false;
  }
}

function startCrawler() {
  if (!CRAWL_ENABLED) return;
  crawlOnce().catch((e) => console.log('crawler error:', e.message));
  // Re-check periodically (new season / updated standings).
  setInterval(() => {
    crawlOnce().catch((e) => console.log('crawler error:', e.message));
  }, 60 * 60 * 1000);
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const route = url.pathname;

  if (req.method === 'OPTIONS') {
    send(res, 204, {});
    return;
  }

  if (req.method === 'GET' && route === '/health') {
    ensureFresh();
    send(res, 200, { ok: true, managers: db.managers.length, crawledPage: db.meta.crawledPage });
    return;
  }

  if (req.method === 'GET' && route === '/search') {
    ensureFresh();
    const q = normalize(url.searchParams.get('q') || '').trim();
    const limit = Math.min(Number(url.searchParams.get('limit')) || 20, 50);
    if (!q) {
      send(res, 200, { results: [] });
      return;
    }
    const results = db.managers
      .map((m) => ({ m, s: score(m, q) }))
      .filter((x) => x.s >= 0)
      .sort((a, b) => (b.s - a.s) || ((a.m.rank || 1e9) - (b.m.rank || 1e9)))
      .slice(0, limit)
      .map((x) => x.m);
    send(res, 200, { results });
    return;
  }

  if (req.method === 'POST' && route === '/register') {
    ensureFresh();
    const raw = await readBody(req);
    try {
      const data = JSON.parse(raw || '{}');
      const teamId = Number(data.teamId);
      const managerName = String(data.managerName || '').trim();
      if (!teamId || !managerName) {
        send(res, 400, { error: 'teamId and managerName are required' });
        return;
      }
      db.managers = db.managers.filter((m) => m.teamId !== teamId);
      db.managers.push({
        teamId,
        managerName,
        teamName: String(data.teamName || '').trim(),
        rank: data.rank != null ? Number(data.rank) : undefined,
      });
      save();
      send(res, 200, { ok: true });
    } catch {
      send(res, 400, { error: 'invalid JSON body' });
    }
    return;
  }

  send(res, 404, { error: 'not found' });
});

loadFromDisk();
server.listen(PORT, () => {
  console.log(`fpl-name-search listening on :${PORT} (${db.managers.length} managers indexed)`);
  startCrawler();
});
