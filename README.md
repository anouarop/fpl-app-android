# FPL Live

A read-only Fantasy Premier League companion app for Android — Kotlin, Jetpack Compose, Material 3.
Search any player, browse squads, watch live points tick during a gameweek, and explore fixtures,
gameweeks, standings, transfers and history — with offline caching.

## Features

- **Home dashboard** — current gameweek, live-match status, top-scoring players, quick links.
- **Players** — browse the full pool with position filters and sorting (points/price/form/selected).
- **Fixtures** — per-gameweek fixtures with difficulty and live scores.
- **Gameweeks** — deadlines, average scores, and per-gameweek fixtures.
- **Squad** — starting XI, bench, captains, live points, next opponent.
- **Player details** — season stats, live match stats, upcoming fixtures, last-5 form.
- **Standings / History** — overall rank progression, per-gameweek history, past seasons.
- **Transfers** — read-only transfer history.
- **Settings** — default team, poll interval, dark mode, clear cache.

## Architecture

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and [`docs/REALTIME.md`](docs/REALTIME.md).
Key points: API DTOs / Room entities / domain models / Compose UI are kept separate; the repository
uses cache-aside (memory TTL → Room → network) with a single-flight guard and stale-data fallback.

## Build

Requires JDK 17 and an Android SDK (see `env.sh` for the local toolchain).

```bash
source env.sh
./gradlew assembleDebug
```

## Test

```bash
./gradlew testDebugUnitTest
```

Tests cover mappers, entity round-trips, pure logic (search/player-list/TTL), ViewModels (fake
repository), and Room + repository cache-aside behavior (Robolectric).

## Environment variables

None required — the FPL API is public and unauthenticated. See `.env.example` for optional
overrides (e.g. `FPL_API_BASE_URL` for a self-hosted proxy).

## External services

- Fantasy Premier League public API — `https://fantasy.premierleague.com/api/`

## Known limitations / TODO-VERIFY

- **Making transfers** — needs an authenticated FPL session; the public API only exposes transfer
  history. See `TransfersScreen`.
- **Private leagues** — classic/H2H standings require authentication; not available publicly.
  See `LeaguesScreen`.
- **Notifications** — reminders are scheduled locally via WorkManager
  (`WorkManagerReminderScheduler` schedules a periodic `GameweekReminderWorker` that posts a
  notification within 24 hours of a gameweek deadline). Requires the `POST_NOTIFICATIONS`
  runtime permission on Android 13+.
