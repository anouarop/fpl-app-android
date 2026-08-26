# Name-search backend plan

## Problem

The public FPL API identifies a manager only by a numeric **team ID** (`/api/entry/{id}/`). There
is **no** endpoint to search a manager by name. To let users "enter their name" we need a server
that maps *name → team ID*.

## Why this can't live in the app

There is no global "list every manager" endpoint. The only public sources of *name → ID* pairs are
league standings endpoints (e.g. `/api/leagues-classic/{id}/standings/`), which cover one league at
a time. Building a complete index requires crawling a large graph of leagues — only feasible on a
server that runs continuously, not on a phone.

## Recommended design

A small hosted service with two jobs:

```
                 ┌──────────────────────────────────────────────┐
                 │              Backend (hosted)                │
                 │                                              │
  FPL public     │   ingester (cron)          Postgres         │
  API ────────►  │   crawls league standings  ──► managers table│
  (crawl)        │   + self-registrations     ──► (name, team_id)│
                 │                                              │
                 │   GET /search?q=<name>  ──► JSON results     │
                 └──────────────────┬───────────────────────────┘
                                    │
                            Android app: "Search by name" screen
```

### 1. Index building (two sources)

- **Self-registration (primary).** When a user enters their team ID in the app, the app also sends
  `{ teamId, managerName, teamName }` to `POST /register`. Over time this builds a crowd-sourced
  index with no crawling cost and no ToS concerns.
- **League crawl (optional booster).** A cron job walks a seed list of popular league IDs, calls
  `/leagues-classic/{id}/standings/`, and upserts every `entry`/`entry_name` pair. It can fan out by
  following each entry's own leagues. Rate-limited to be polite to the public API.

### 2. Search endpoint

```
GET /search?q=haaland
→ [{ teamId: 9166708, managerName: "Erling H", teamName: "Neto Bangers" }, ...]
```

Normalize (lowercase, strip accents), search by prefix/substring, cap results at ~20, cache
responses for a few minutes.

### 3. Stack (small, cheap)

| Component | Suggestion |
|-----------|------------|
| API | Node (Fastify) / Go / Python (FastAPI) |
| DB | Postgres (Supabase/Railway/Neon) |
| Search | Postgres `ILIKE '%q%'` + a `pg_trgm` GIN index; Elasticsearch only if it grows |
| Crawler | A cron/scheduled function calling the same API client |

## App integration

1. Add a `NameSearchRepository` that calls the backend's `/search` endpoint.
2. On the onboarding screen, offer "search by name" as an alternative to entering the team ID.
3. Results list shows manager name + team name; tapping one completes onboarding with that team ID.

## Costs / caveats

- Hosting a free/cheap tier (e.g. Fly.io/Railway free, Neon free) is enough for low traffic.
- **Rate-limit** the crawler and respect FPL's public API; do not hammer it.
- The index is only as complete as its registrations/crawled leagues — set expectations in the UI
  ("not everyone is indexed yet; you can also paste your team ID").
- Store no personal data beyond a manager's public FPL display name and team ID.

## Out of scope (v1)

- Real-time syncing of the entire manager population (not possible via public API).
- OAuth/authenticated sign-in (FPL has no public auth endpoint).
