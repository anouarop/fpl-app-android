// Crawler: populates data.json with FPL managers from the public "Overall" league
// (id 314). The standings endpoint is paginated (50 per page) and unauthenticated.
// The server reloads data.json on change, so run this alongside `node server.js`.
//
// Usage:  node crawl.js [startPage] [endPage] [dataFile]
//   e.g.  node crawl.js 1 2000        # crawl pages 1..2000 (~100k teams)
//         node crawl.js                # defaults: pages 1..1000

const fs = require('fs');
const path = require('path');
const https = require('https');
const zlib = require('zlib');

const LEAGUE_ID = 314;
const DATA_FILE = process.argv[4] || path.join(__dirname, 'data.json');

const start = Number(process.argv[2]) || 1;
const end = Number(process.argv[3]) || 1000;

function load() {
  try {
    return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
  } catch {
    return { managers: [] };
  }
}

function save(db) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2));
}

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
              if (res.headers['content-encoding'] === 'gzip') {
                buf = zlib.gunzipSync(buf);
              }
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

async function crawlPage(page) {
  const url = `https://fantasy.premierleague.com/api/leagues-classic/${LEAGUE_ID}/standings/?page_standings=${page}`;
  const json = await fetchJson(url);
  const rows = (json && json.standings && json.standings.results) || [];
  const hasNext = !!(json && json.standings && json.standings.has_next);
  return { rows, hasNext };
}

async function main() {
  const db = load();
  const byId = new Map(db.managers.map((m) => [m.teamId, m]));

  let added = 0;
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
      process.stdout.write(`\rpage ${page}/${end}  (${byId.size} teams)`);
      if (page % 50 === 0) {
        db.managers = Array.from(byId.values()).sort((a, b) => (a.rank || 1e9) - (b.rank || 1e9));
        save(db);
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

  db.managers = Array.from(byId.values()).sort((a, b) => (a.rank || 1e9) - (b.rank || 1e9));
  save(db);
  process.stdout.write(`\ndone — ${db.managers.length} teams saved to ${DATA_FILE}\n`);
}

main();
