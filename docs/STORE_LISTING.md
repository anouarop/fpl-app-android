# Play Store listing

> Before you publish, rename the app to avoid the "FPL" / "Fantasy Premier League" trademark.
> The current label is "FPL Pulse" (`app/src/main/res/values/strings.xml`) and the package is
> `com.shellanddeploy.fpllive`. Consider a neutral name and description (see below). You can
> change the visible app name in `strings.xml` without changing the package name.

## Recommended name

**Fantasy Football Companion** — or an original brand name you own. Avoid "FPL", "Fantasy Premier
League", or the official logo.

## Short description (≤ 80 characters)

> Live points, ranks, squads and fixtures for your Fantasy Premier League team.

## Full description

```
Track your Fantasy Premier League team with live points, ranks, fixtures and history.

FEATURES
• Home dashboard — current gameweek, live-match status and top-scoring players.
• Your squad — starting XI, bench, captains and live points, with next opponent.
• Players — browse the full player pool with position filters and sorting.
• Fixtures & gameweeks — deadlines, average scores and difficulty ratings.
• Standings & history — overall rank, per-gameweek history and past seasons.
• Transfers — read-only transfer history.
• Works offline — data is cached locally for fast, offline access.

GETTING STARTED
Enter your team ID (the number in your team's URL on the official site) and you're ready.

NOTE
This is an unofficial companion app. It is not affiliated with or endorsed by the official
Fantasy Premier League. Data is read from the public FPL API; some features such as making
transfers and private leagues are not available.
```

## Things you must supply in Play Console

1. **Privacy policy URL** — host `docs/PRIVACY_POLICY.md` (e.g. GitHub Pages) and paste the link.
   Required because the app uses network access and notifications.
2. **App icon** (512×512) — the current icon is a placeholder.
3. **Feature graphic** (1024×500) and **screenshots** (phone + optionally tablet).
4. **Content rating questionnaire** — answer that the app has no user-generated content, no
   purchases, no location. 
5. **Data safety form** — declare "No data collected or shared" (the app stores data only on-device
   and sends your team ID only to the FPL API you already interact with). 
6. **Target audience / ads** — mark "no ads".

## Signing & upload

- A release keystore already exists at `release.jks` (see `keystore.properties`, both gitignored).
- Build the upload bundle with `./gradlew bundleRelease`.
- **Recommended:** enable **Play App Signing** in Play Console and upload the `release.jks` as the
  upload key; Google will generate and hold the app-signing key for you.

## Checklist before submitting

- [ ] Renamed the app in `strings.xml` (trademark-safe).
- [ ] Replaced the placeholder launcher icon.
- [ ] Added a real support/contact email.
- [ ] Hosted the privacy policy and pasted the URL.
- [ ] Filled the Data safety form.
- [ ] `./gradlew bundleRelease` produces `app/build/outputs/bundle/release/app-release.aab`.
