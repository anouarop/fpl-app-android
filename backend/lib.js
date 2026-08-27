// Shared helpers for the FPL name-search service and its crawler.
// Zero runtime dependencies; requires Node 18+.

'use strict';

const fs = require('fs');
const https = require('https');
const zlib = require('zlib');

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

module.exports = { normalize, fetchJson, loadDb, sortManagers, saveDb };
