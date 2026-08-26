# Name-search backend

A zero-dependency Node.js service that maps manager/team **names → FPL team IDs**. The public FPL
API has no name-search endpoint, so this service maintains an index. By default it **crawls FPL's
public Overall league** in the background to grow the index, and the app registers every team whose
owner completes onboarding.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | `{ ok, managers, crawledPage }` — liveness + index size |
| `GET` | `/search?q=<name>&limit=20` | `{ results: [{ teamId, managerName, teamName, rank }] }` |
| `POST` | `/register` | body `{ teamId, managerName, teamName }` — upsert an entry |

## Run locally

```bash
node server.js            # defaults: PORT=8080, DATA_FILE=./data.json
```

The background crawler runs automatically (disable with `CRAWL=off`). To crawl manually:

```bash
node crawl.js 1 2000      # crawl pages 1..2000 (~100k teams, ~50 per page)
```

## Deploy to Render (free)

1. Push this repo to GitHub (the `backend/` folder + `render.yaml` at the repo root).
2. Sign up at [render.com](https://render.com), then **New → Blueprint**, and connect the repo.
   Render provisions the `fpl-name-search` web service from `render.yaml`.
3. Once deployed, note the public URL (e.g. `https://fpl-name-search.onrender.com`).
4. Put that URL in the app's `gradle.properties`:

   ```properties
   nameSearchBaseUrl=https://fpl-name-search.onrender.com
   ```

   …and rebuild the app.

Alternatively, deploy manually: **New → Web Service** → connect repo → *Root Directory* = `backend`,
*Build command* = `npm install`, *Start command* = `npm start`.

> Railway also works: deploy from GitHub (it auto-detects the `package.json` start script).

## Important caveats

- **The index grows over time.** A full crawl of ~12M managers takes many hours; popular/high-ranked
  teams appear first. Re-running is safe (upserts by team ID) and the crawler resumes automatically.
- **Persistence.** Render's free tier has no persistent disk, so `data.json` resets on redeploy and
  the crawler restarts. For a durable index, attach a disk (paid) or move storage to a free Postgres
  (Supabase/Neon) — reach out if you want help with that.
- Data stored is only a manager's public FPL display name, team name, and team ID.
