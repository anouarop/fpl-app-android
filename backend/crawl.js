// Crawler: populates data.json with FPL managers from the public "Overall" league
// (id 314). The standings endpoint is paginated (50 per page) and unauthenticated.
// The server reloads data.json on change, so run this alongside `node server.js`.
// Crawl progress is recorded in data.json's `meta.crawledPage`, so the server's own
// background crawler resumes from where this left off instead of restarting at page 1.
//
// Usage:  node crawl.js [startPage] [endPage] [dataFile]
//   e.g.  node crawl.js 1 2000        # crawl pages 1..2000 (~100k teams)
//         node crawl.js                # defaults: pages 1..1000

'use strict';

const path = require('path');
const { fetchJson, loadDb, sortManagers, saveDb } = require('./lib');

const LEAGUE_ID = 314;
const DATA_FILE = process.argv[4] || path.join(__dirname, 'data.json');

const start = Number(process.argv[2]) || 1;
const end = Number(process.argv[3]) || 1000;

async function crawlPage(page) {
  const url = `https://fantasy.premierleague.com/api/leagues-classic/${LEAGUE_ID}/standings/?page_standings=${page}`;
  const json = await fetchJson(url);
  const rows = (json && json.standings && json.standings.results) || [];
  const hasNext = !!(json && json.standings && json.standings.has_next);
  return { rows, hasNext };
}

async function main() {
  const db = loadDb(DATA_FILE);
  const byId = new Map(db.managers.map((m) => [m.teamId, m]));

  let added = 0;
  let lastPage = start;
  for (let page = start; page <= end; page++) {
    try {
      const { rows, hasNext } = await crawlPage(page);
      for (const r of rows) {
        if (!r.entry) continue;
        byId.set(r.entry, {
          teamId: r.entry,
          managerName: r.player_name || '',
          teamName: r.entry_name || '',
          rank: r.rank || 0,
        });
        added++;
      }
      lastPage = page;
      process.stdout.write(`\rpage ${page}/${end}  (${byId.size} teams)`);
      if (page % 50 === 0 || !hasNext) {
        db.managers = sortManagers(Array.from(byId.values()));
        db.meta = { crawledPage: page };
        saveDb(db, DATA_FILE);
      }
      if (!hasNext) {
        process.stdout.write('\nreached end of standings\n');
        break;
      }
    } catch (e) {
      process.stdout.write(`\npage ${page} failed: ${e.message} — stopping\n`);
      break;
    }
  }

  db.managers = sortManagers(Array.from(byId.values()));
  db.meta = { crawledPage: lastPage };
  saveDb(db, DATA_FILE);
  process.stdout.write(`\ndone — ${db.managers.length} teams saved to ${DATA_FILE} (${added} added)\n`);
}

main();
