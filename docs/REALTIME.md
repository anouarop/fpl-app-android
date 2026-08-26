# Real-time data

FPL Live surfaces live gameweek data without hammering the public API. The FPL API is
unauthenticated and rate-sensitive, so every fetch is deliberately bounded.

## Caching layers

1. **OkHttp disk cache** (10 MB) — `FplClient` serves `bootstrap`, `fixtures`, `entry`,
   `element-summary`, and `history` with `Cache-Control: max-age=600`. The live endpoint
   (`/event/{id}/live`) is forced to `no-store` so live data is never served stale from disk.
2. **Room cache** — persistent offline copy of the bootstrap index, fixtures per gameweek, entry,
   and per-gameweek scores. `cache_meta` records when each key was last refreshed.
3. **In-memory TTL cache** (`TtlCache`) — short-lived, shared across the app, bounding re-fetch
   cadence for rapidly-changing data (live stats) and deduplicating repeated reads.

## Cadence (TTL constants in `FplRepositoryImpl`)

| Resource            | TTL        |
|---------------------|------------|
| bootstrap           | 10 minutes |
| fixtures            | 10 minutes |
| entry               | 10 minutes |
| history             | 10 minutes |
| element-summary     | 10 minutes |
| transfers           | 10 minutes |
| live / live fixtures| 20 seconds |

## Behavior

- **Cache-aside**: memory TTL → Room (if fresh) → network. Writes to Room happen in a single
  transaction (`db.withTransaction`), and `cache_meta` is updated so subsequent reads are fresh.
- **Stale data**: when a network call fails but a cached copy exists, the repository returns the
  cached data with `stale = true`. The UI surfaces this via the "updated X ago — offline data"
  label and by keeping content visible instead of showing a blank error screen.
- **Network failures**: only surface a hard error when there is no cached copy at all.
- **Duplicate requests**: a per-key `Mutex` coalesces concurrent requests so a resource is only
  fetched from the network once at a time; the second caller re-reads the cache under the lock.
- **Rate limits**: TTLs and the single-flight guard are the primary protection. Live polling is
  user-configurable (20/30/60s) and only runs while the screen is foregrounded and a match is in
  progress (`LiveBadge` screens), never in the background.

## Live UI

- `TeamScreen` and `PlayerDetailScreen` poll `live()` and `liveFixtures()` while `ON_START`/`ON_STOP`
  are active and only when a current match is live, updating `livePoints`/`liveStats` reactively.
- `HomeScreen` observes `observeBootstrap()` (Room Flow) and refreshes on pull-to-refresh.

## Synchronization failure recovery

- Failed refreshes leave the previous data intact and mark it stale; the next successful refresh
  (poll tick or manual pull-to-refresh) overwrites it.
- `clearCache()` (Settings → "Clear cache & search history") clears both the Room cache and the
  in-memory TTL layer in one transaction, then re-fetches lazily.
