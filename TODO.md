# TODO — Resume plan for FPL Live

Last updated: 2026-08-26. Read this when you resume to know exactly where things stand and what to do next.

---

## 0. What this project is

An Android app (Kotlin / Jetpack Compose) — a read-only Fantasy Premier League companion — plus two
small zero-dependency Node.js backends.

| Piece | Path | Purpose | Status |
|-------|------|---------|--------|
| Android app | `app/` | Players, fixtures, squad, standings, history, transfers, leagues, settings, onboarding, **Live tab** | working, not published |
| Name-search backend | `backend/` | crawls FPL Overall league → maps name → team ID (`/search`, `/register`) | working, 537k managers indexed |
| Live-events backend | `backend-live/` | scrapes SofaScore + FPL live → real-time goals/cards (`/live`) | working, port 8081 |

**Key fact that unblocks everything:** the app works with *only* the FPL public API. The two
backends are enhancements and already degrade gracefully (they show an error/unavailable message
when down). So you can ship without them — do not treat them as blockers.

---

## 1. Current uncommitted work (do NOT lose this)

Everything below is uncommitted on top of the single `Initial commit`. **Commit it first.**

- `app/` — new Live tab (`ui/live/`, `data/live/`, `domain/model/LiveFeed.kt`), name search in
  Settings, squad-players-under-fixtures feature, removal of the dead sign-in/auth screens,
  `Card` component Box→Column change, squad "appears after deadline" note in Fixtures.
- `backend/` — `lib.js` (new), refactored `server.js` + `crawl.js` (atomic saves, faster search,
  crawl-progress tracking).
- `backend-live/` — new service.
- `render.yaml` — now provisions **both** backends.
- `gradle.properties` — added `liveBaseUrl`.

---

## 2. Step-by-step to finish (do in this order)

### Step 1 — Commit everything ✅ DONE
Committed in clean chunks (f5596d8, 85da012, 1d27ab7, 932870f). `testDebugUnitTest` passes.

### Step 2 — Create a GitHub repo and push ✅ DONE
Repo `github.com/anouarop/fpl-app-android` exists; pushed `main` (932870f).

### Step 3 — Deploy the two backends (manual, ~15 min)
- Render → **New → Blueprint** → connect the GitHub repo.
- `render.yaml` (already in repo root) provisions `fpl-name-search` and `fpl-live-events` for free,
  each with an HTTPS `*.onrender.com` URL.

### Step 4 — Point the app at production URLs + build release
In `gradle.properties`, replace the dev-machine IPs with the Render URLs:
```properties
nameSearchBaseUrl=https://fpl-name-search.onrender.com
liveBaseUrl=https://fpl-live-events.onrender.com
```
Then build:
```bash
source env.sh
./gradlew bundleRelease      # or assembleRelease for a test APK
```
Note: the release manifest blocks cleartext HTTP, so these **must** be HTTPS.

Build validated locally: `bundleRelease` succeeds and produces
`app/build/outputs/bundle/release/app-release.aab` (release-signed). Still points at the dev
HTTP URLs until Step 3 URLs are known — rebuild after updating `gradle.properties`.

### Step 5 — Publish to Google Play (manual, one-time setup)
1. Create a **Google Play Console** account ($25 one-time).
2. Upload the release AAB (release signing is already configured via `keystore.properties` +
   `release.jks`).
3. Fill the store listing — content already drafted in `docs/STORE_LISTING.md`.
4. Privacy policy — content in `docs/PRIVACY_POLICY.md`; host it (GitHub Pages or a tiny Render
   page) and paste the URL.
5. Data-safety form — the app collects no personal data, so answer "No" throughout.
6. Content rating + submit for review.

### Step 6 — Post-launch hardening (only later, optional)
- **Name-search persistence:** Render free tier has ephemeral disk, so `data.json` re-crawls on
  every redeploy. Fix with a persistent disk (paid) or Postgres (Supabase/Neon free).
- **Live data source:** SofaScore scraping is unofficial/fragile. Swap in a licensed provider
  (API-Football / Sportmonks) behind the same `/live` contract — no app change needed.
- **Bonus / clean-sheet accuracy:** live `bonus`/`clean_sheets` still come from FPL's delayed feed;
  the fast path only covers goals/assists/cards for now.

---

## 3. How to run everything locally (dev)

```bash
# App
source env.sh
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # test
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Name-search backend (port 8080)
node backend/server.js

# Live-events backend (port 8081)
node backend-live/server.js
```

`gradle.properties` now points at the Render hosts (`https://fpl-name-search.onrender.com` /
`https://fpl-live-events.onrender.com`), so the app no longer depends on any laptop. To develop
against local backends, temporarily set those two properties back to `http://<lan-ip>:8080/8081`
(the debug manifest allows cleartext, so local HTTP works in debug builds only).

---

## 4. Known caveats / gotchas

- **FPL picks endpoint** returns `"Not found"` for future gameweeks and for a brand-new team with
  no completed gameweek — that's why squad players under fixtures are empty for a fresh team
  (we now show a note instead of a blank area).
- **Player name matching** (SofaScore → FPL) is fuzzy; a failed match just skips that player.
- **Wireless debugging** (adb) ports rotate every time the phone reconnects — re-pair each session.
- **`backend/data.json` is ~70 MB / 537k managers** and is NOT committed (see `.gitignore`) — keep it
  out of git.

---

## 5. File map (quick reference)

- App entry / DI: `app/src/main/java/com/shellanddeploy/fpllive/FplApp.kt`, `MainActivity.kt`
- Live feature: `ui/live/`, `data/live/LiveRepository.kt`, `domain/model/LiveFeed.kt`
- Settings name search: `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`
- Backends: `backend/server.js`, `backend/crawl.js`, `backend/lib.js`, `backend-live/server.js`
- Deploy: `render.yaml` (root)
- Docs: `docs/ARCHITECTURE.md`, `docs/REALTIME.md`, `docs/NAME_SEARCH_BACKEND.md`,
  `docs/STORE_LISTING.md`, `docs/PRIVACY_POLICY.md`
