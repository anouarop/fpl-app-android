// Shared helpers for the FPL live-events service.
// Zero runtime dependencies; requires Node 18+.

'use strict';

const https = require('https');
const zlib = require('zlib');

const USER_AGENT =
  'Mozilla/5.0 (Linux; Android 13; SM-A057F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36';

/** Fetch JSON over HTTPS (gzip-aware). Rejects on network/parse errors. */
function fetchJson(url, headers = {}) {
  return new Promise((resolve, reject) => {
    https
      .get(
        url,
        { headers: { 'User-Agent': USER_AGENT, 'Accept-Encoding': 'gzip', ...headers } },
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

/** Lowercase + strip diacritics + drop apostrophes/full-stops so "Nott'm Forest" -> "nottm forest". */
function normalize(s) {
  return String(s || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[''.]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

/** Levenshtein distance (bounded) for fuzzy name matching. */
function levenshtein(a, b, max = 3) {
  if (Math.abs(a.length - b.length) > max) return max + 1;
  const dp = Array.from({ length: a.length + 1 }, (_, i) => [i]);
  for (let j = 0; j <= b.length; j++) dp[0][j] = j;
  for (let i = 1; i <= a.length; i++) {
    let rowMin = dp[i][0];
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      dp[i][j] = Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost);
      rowMin = Math.min(rowMin, dp[i][j]);
    }
    if (rowMin > max) return max + 1;
  }
  return dp[a.length][b.length];
}

module.exports = { fetchJson, normalize, levenshtein };
