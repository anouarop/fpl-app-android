// FPL live-events service.
//
// Combines two sources so managers see goals / assists / cards the moment they
// happen instead of waiting for FPL's delayed feed:
//   - SofaScore `events/live` + `event/{id}/incidents`  -> real-time match events
//   - FPL `bootstrap-static` + `event/{id}/live`         -> player mapping + authoritative points
//
// Endpoints:
//   GET /health  -> { ok, event, matches, updated }
//   GET /live    -> { event, updated, matches: [...], players: { <elementId>: {...} } }
//
// Env:
//   PORT          (default 8081)
//   POLL_MS       (default 15000) SofaScore refresh cadence

'use strict';

const http = require('http');
const { URL } = require('url');
const { fetchJson, normalize, levenshtein } = require('./lib');

const PORT = Number(process.env.PORT) || 8081;
const POLL_MS = Number(process.env.POLL_MS) || 15000;

const FPL = 'https://fantasy.premierleague.com/api/';
const SOFA = 'https://api.sofascore.com/api/v1/sport/football/';
const EPL_UNIQUE_TOURNAMENT_ID = 17;

// FPL element_type -> points per goal (GK=1, DEF=2, MID=3, FWD=4).
const GOAL_POINTS = { 1: 6, 2: 6, 3: 5, 4: 4 };
const ASSIST_POINTS = 3;
const YELLOW_POINTS = -1;
const RED_POINTS = -3;
const SECOND_YELLOW_POINTS = -4; // yellow + red in one match
const OWN_GOAL_POINTS = -2;

let bootstrap = null;
let bootstrapAt = 0;
let snapshot = { event: 0, updated: 0, matches: [], players: {} };

// Canonical team key so "Man City" (FPL) matches "Manchester City" (SofaScore).
function canonicalTeam(name) {
  const k = normalize(name);
  const aliases = {
    'man city': 'manchester city',
    'man utd': 'manchester united',
    'manchester utd': 'manchester united',
    'newcastle': 'newcastle united',
    'nottm forest': 'nottingham forest',
    'nottingham forest': 'nottingham forest',
    'spurs': 'tottenham',
    'tottenham hotspur': 'tottenham',
    'wolves': 'wolverhampton',
    'wolverhampton wanderers': 'wolverhampton',
  };
  return aliases[k] || k;
}

async function getBootstrap() {
  if (bootstrap && Date.now() - bootstrapAt < 10 * 60 * 1000) return bootstrap;
  bootstrap = await fetchJson(FPL + 'bootstrap-static/');
  bootstrapAt = Date.now();
  return bootstrap;
}

function currentEventId(b) {
  const events = b.events || [];
  return (events.find((e) => e.is_current) || events.find((e) => e.is_next) || events[0])?.id || 0;
}

async function getFixtures(eventId) {
  if (!eventId) return [];
  return fetchJson(FPL + 'fixtures/?event=' + eventId);
}

async function getFplLive(eventId) {
  if (!eventId) return {};
  const json = await fetchJson(FPL + 'event/' + eventId + '/live/');
  const players = {};
  for (const e of json.elements || []) {
    players[e.id] = e.stats || {};
  }
  return players;
}

async function getSofaLiveEpl() {
  const json = await fetchJson(SOFA + 'events/live');
  const byKey = new Map();
  for (const e of json.events || []) {
    if ((e.tournament?.uniqueTournament?.id ?? -1) !== EPL_UNIQUE_TOURNAMENT_ID) continue;
    const home = e.homeTeam?.name || '';
    const away = e.awayTeam?.name || '';
    const key = `${canonicalTeam(home)}|${canonicalTeam(away)}`;
    byKey.set(key, {
      id: e.id,
      homeScore: e.homeScore?.current ?? 0,
      awayScore: e.awayScore?.current ?? 0,
      status: e.status?.description || '',
      homeName: home,
      awayName: away,
    });
  }
  return byKey;
}

async function getIncidents(sofascoreEventId) {
  const json = await fetchJson(SOFA + `event/${sofascoreEventId}/incidents`);
  return json.incidents || [];
}

// teamId -> [player]
function buildPlayerIndex(b) {
  const byTeam = new Map();
  for (const p of b.elements || []) {
    if (!byTeam.has(p.team)) byTeam.set(p.team, []);
    byTeam.get(p.team).push({
      id: p.id,
      display: p.web_name,
      webName: normalize(p.web_name),
      fullName: normalize(`${p.first_name} ${p.second_name}`),
      lastName: normalize(p.second_name),
      pos: p.element_type,
    });
  }
  return byTeam;
}

function matchPlayer(index, teamId, sofascoreName) {
  const candidates = index.get(teamId) || [];
  const q = normalize(sofascoreName);
  if (!q) return null;
  for (const c of candidates) if (c.fullName === q) return c;
  for (const c of candidates) if (c.lastName === q || c.webName === q) return c;
  const lastToken = q.split(' ').pop();
  for (const c of candidates) if (c.lastName === lastToken || c.webName === lastToken) return c;
  let best = null;
  let bestDist = 2;
  for (const c of candidates) {
    const d = Math.min(
      levenshtein(q, c.fullName, 2),
      levenshtein(q, c.lastName, 2),
      levenshtein(q, c.webName, 2),
    );
    if (d < bestDist) {
      bestDist = d;
      best = c;
    }
  }
  return best;
}

function goalFor(pos) {
  return GOAL_POINTS[pos] || 5;
}

function processIncidents(incidents, index, homeTeamId, awayTeamId) {
  const events = [];
  for (const inc of incidents) {
    const type = inc.incidentType;
    const isHome = inc.isHome;
    const team = isHome ? 'home' : 'away';
    const teamFplId = isHome ? homeTeamId : awayTeamId;

    if (type === 'goal') {
      const scorer = matchPlayer(index, teamFplId, inc.player?.name);
      if (!scorer) continue;
      if (inc.incidentClass === 'ownGoal') {
        events.push({
          minute: inc.time,
          type: 'ownGoal',
          team: isHome ? 'away' : 'home', // an own goal credits the *other* team
          player: { fplId: scorer.id, name: scorer.display, points: OWN_GOAL_POINTS },
        });
      } else {
        const assist = matchPlayer(index, teamFplId, inc.assist1?.name);
        events.push({
          minute: inc.time,
          type: 'goal',
          team,
          player: { fplId: scorer.id, name: scorer.display, points: goalFor(scorer.pos) },
          assist: assist ? { fplId: assist.id, name: assist.display, points: ASSIST_POINTS } : null,
          detail: inc.incidentClass === 'penalty' ? 'penalty' : undefined,
        });
      }
    } else if (type === 'card') {
      const player = matchPlayer(index, teamFplId, inc.player?.name);
      if (!player) continue;
      const cls = inc.incidentClass;
      const points = cls === 'red' ? RED_POINTS : cls === 'yellowRed' ? SECOND_YELLOW_POINTS : YELLOW_POINTS;
      events.push({
        minute: inc.time,
        type: cls === 'yellowRed' ? 'secondYellow' : cls === 'red' ? 'red' : 'yellow',
        team,
        player: { fplId: player.id, name: player.display, points },
      });
    }
  }
  return events;
}

function mapTeams(b) {
  const m = new Map();
  for (const t of b.teams || []) m.set(t.id, { id: t.id, name: t.name, short: t.short_name });
  return m;
}

async function refresh() {
  const b = await getBootstrap();
  const eventId = currentEventId(b);
  const teams = mapTeams(b);
  const index = buildPlayerIndex(b);

  const [fixtures, sofMatches, fplPlayers] = await Promise.all([
    getFixtures(eventId),
    getSofaLiveEpl(),
    getFplLive(eventId),
  ]);

  const matches = [];
  for (const f of fixtures) {
    const home = teams.get(f.team_h);
    const away = teams.get(f.team_a);
    const key = `${canonicalTeam(home?.name)}|${canonicalTeam(away?.name)}`;
    const sof = sofMatches.get(key);

    let homeScore = f.team_h_score ?? 0;
    let awayScore = f.team_a_score ?? 0;
    let status = f.finished ? 'FT' : f.started ? 'LIVE' : 'upcoming';
    let live = false;
    let events = [];

    if (sof) {
      live = true;
      homeScore = sof.homeScore ?? homeScore;
      awayScore = sof.awayScore ?? awayScore;
      status = sof.status || status;
      const incidents = await getIncidents(sof.id);
      events = processIncidents(incidents, index, f.team_h, f.team_a);
    }

    matches.push({
      id: f.id,
      kickoff: f.kickoff_time,
      home: home || { id: f.team_h, name: '?', short: '?' },
      away: away || { id: f.team_a, name: '?', short: '?' },
      homeScore,
      awayScore,
      status,
      live,
      events,
    });
  }

  snapshot = { event: eventId, updated: Date.now(), matches, players: fplPlayers };
}

function send(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  });
  res.end(body);
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  if (req.method === 'OPTIONS') return send(res, 204, {});

  if (req.method === 'GET' && url.pathname === '/health') {
    return send(res, 200, {
      ok: true,
      event: snapshot.event,
      matches: snapshot.matches.length,
      updated: snapshot.updated,
    });
  }

  if (req.method === 'GET' && url.pathname === '/live') {
    return send(res, 200, snapshot);
  }

  send(res, 404, { error: 'not found' });
});

async function loop() {
  for (;;) {
    try {
      await refresh();
    } catch (e) {
      console.log('refresh failed:', e.message);
    }
    await new Promise((r) => setTimeout(r, POLL_MS));
  }
}

refresh().catch((e) => console.log('initial refresh failed:', e.message));
server.listen(PORT, () => console.log(`fpl-live-events listening on :${PORT}`));
loop();
