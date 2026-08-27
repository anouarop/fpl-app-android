// FPL name-search service — maps manager/team names -> FPL team IDs.
// Zero runtime dependencies (Node 18+). Serves /search, /register, /health and, by
// default, also crawls the public FPL "Overall" league in the background to grow the
// index (so a single deployed process is fully self-sufficient).
//
// Endpoints:
//   GET  /health              -> { ok, managers, crawledPage }
//   GET  /search?q=&limit=    -> { results: [{ teamId, managerName, teamName, rank }] }
//   POST /register            -> body { teamId, managerName, teamName }  (upsert)
//
// Env:
//   PORT           (default 8080)
//   DATA_FILE      (default ./data.json)
//   CRAWL          set to "off" to disable the background crawler
//   CRAWL_DELAY_MS (default 250) delay between standings pages

'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const { URL } = require('url');
const { normalize, fetchJson, loadDb, sortManagers, saveDb } = require('./lib');

const PORT = Number(process.env.PORT) || 8080;
const DATA_FILE = process.env.DATA_FILE || path.join(__dirname, 'data.json');
const CRAWL_ENABLED = process.env.CRAWL !== 'off';
const CRAWL_DELAY_MS = Number(process.env.CRAWL_DELAY_MS) || 250;
const OVERALL_LEAGUE_ID = 314;

const SEARCH_CACHE_TTL_MS = 60_000;
const SEARCH_CACHE_MAX = 200;

let db = { managers: [], meta: { crawledPage: 0 } };
let index = []; // parallel search entries: [{ m, nameKey, teamKey }] derived from db.managers
let loadedMtimeMs = 0;
const searchCache = new Map(); // q -> { results, expires }

// Derive the in-memory search index from db.managers. Normalizing once here is
// what keeps /search fast (it avoids re-normalizing every manager on every query).
function rebuildIndex() {
  index = db.managers.map((m) => ({
    m,
    nameKey: normalize(m.managerName),
    teamKey: normalize(m.teamName),
  }));
}

function loadFromDisk() {
  db = loadDb(DATA_FILE);
  // If no crawl metadata exists (e.g. an index built by an older crawl.js), estimate it.
  if (!(db.meta && Number(db.meta.crawledPage) > 0)) {
    db.meta = { crawledPage: Math.round(db.managers.length / 50) };
  }
  try {
    loadedMtimeMs = fs.statSync(DATA_FILE).mtimeMs;
  } catch {
    loadedMtimeMs = 0;
  }
  rebuildIndex();
}

// Reload from disk if an external writer (e.g. crawl.js) has touched the file.
function ensureFresh() {
  try {
    const mtime = fs.statSync(DATA_FILE).mtimeMs;
    if (mtime !== loadedMtimeMs) loadFromDisk();
  } catch {
    /* keep current in-memory data */
  }
}

function save() {
  sortManagers(db.managers);
  loadedMtimeMs = saveDb(db, DATA_FILE);
  rebuildIndex();
}

function score(entry, q, words) {
  const { nameKey: name, teamKey: team, m } = entry;

  // A pure number is treated as a team ID search (id === 95, prefix 85).
  if (words.length === 1 && /^\d+$/.test(words[0])) {
    const id = String(m.teamId);
    if (id === words[0]) return 95;
    if (id.startsWith(words[0])) return 85;
    return -1;
  }

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

function search(q, limit) {
  const now = Date.now();
  const cached = searchCache.get(q);
  if (cached && cached.expires > now) return cached.results;

  const words = q.split(/\s+/).filter(Boolean);
  const results = index
    .map((e) => ({ e, s: score(e, q, words) }))
    .filter((x) => x.s >= 0)
    .sort((a, b) => (b.s - a.s) || ((a.e.m.rank || 1e9) - (b.e.m.rank || 1e9)))
    .slice(0, limit)
    .map((x) => x.e.m);

  if (searchCache.size >= SEARCH_CACHE_MAX) {
    const oldest = searchCache.keys().next().value;
    searchCache.delete(oldest);
  }
  searchCache.set(q, { results, expires: now + SEARCH_CACHE_TTL_MS });
  return results;
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

let crawling = false;

async function crawlOnce() {
  if (crawling) return;
  crawling = true;
  try {
    const map = new Map(db.managers.map((m) => [m.teamId, m]));
    let page = (Number(db.meta.crawledPage) || 0) + 1;
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
      for (const r of rows) {
        if (!r.entry) continue;
        map.set(r.entry, {
          teamId: r.entry,
          managerName: r.player_name || '',
          teamName: r.entry_name || '',
          rank: r.rank || 0,
        });
      }
      db.meta.crawledPage = page;
      if (page % 50 === 0) {
        db.managers = Array.from(map.values());
        save();
      }
      if (!hasNext) {
        db.managers = Array.from(map.values());
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
    send(res, 200, { results: search(q, limit) });
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
