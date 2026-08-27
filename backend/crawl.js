// Crawler: populates the manager index (Postgres if DATABASE_URL is set, otherwise
// data.json) with FPL managers from the public "Overall" league (id 314). The
// standings endpoint is paginated (50 per page) and unauthenticated. Run alongside
// `node server.js` only when not using the server's built-in crawler.
//
// Usage:  node crawl.js [startPage] [endPage] [dataFile]
//   e.g.  node crawl.js 1 2000        # crawl pages 1..2000 (~100k teams)
//         node crawl.js                # defaults: pages 1..1000

'use strict';

const path = require('path');
const { fetchJson, dbLoadAll, dbUpsert, dbSaveMeta, sortManagers } = require('./lib');

const LEAGUE_ID = 314;
const DATA_FILE = process.argv[4] || path.join(__dirname, 'data.json');

const start = Number(process.argv[2]) || 1;
const end = Number(process.argv[3]) || 1000;

async function crawlPage(page) {
  const url = `https://fantasy.premierleague.com/api/leagues-classic/${LEAGUE_ID}/standings/?page_standings=${page}`;
  const json = await fetchJson(url);
  const rows = (json && json.standings && json.standings.results) || [];
  const hasNext = !!(json && json.standings && json.standings.hasNext);
  return { rows, hasNext };
}

async function main() {
  const db = await dbLoadAll(DATA_FILE);
  const byId = new Map(db.managers.map((m) => [m.teamId, m]));

  let added = 0;
  let lastPage = start;
  let pending = [];
  for (let page = start; page <= end; page++) {
    try {
      const { rows, hasNext } = await crawlPage(page);
      for (const r of rows) {
        if (!r.entry) continue;
        const m = {
          teamId: r.entry,
          managerName: r.player_name || '',
          teamName: r.entry_name || '',
          rank: r.rank || 0,
        };
        byId.set(r.entry, m);
        pending.push(m);
        added++;
      }
      lastPage = page;
      process.stdout.write(`\rpage ${page}/${end}  (${byId.size} teams)`);
      if (page % 50 === 0 || !hasNext) {
        db.managers = sortManagers(Array.from(byId.values()));
        db.meta = { crawledPage: page };
        await dbUpsert(pending);
        await dbSaveMeta(page);
        pending = [];
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
  await dbUpsert(pending);
  await dbSaveMeta(lastPage);
  process.stdout.write(`\ndone — ${byId.size} teams processed (${added} added)\n`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
