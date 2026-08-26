# Architecture

FPL Live is a single-module Android app (Kotlin, Jetpack Compose, Material 3) that acts as a
read-only companion to the Fantasy Premier League API. It follows a layered architecture with a
clean separation between API DTOs, the local database, domain models, and Compose UI.

## Module / package layout

Everything lives in the `:app` module under `com.shellanddeploy.fpllive`:

```
app/src/main/java/com/shellanddeploy/fpllive/
├── FplApp.kt                 # Application subclass: manual composition root (DI)
├── MainActivity.kt           # Single activity + Navigation Compose graph
├── data/
│   ├── api/                  # Retrofit API, OkHttp client, repository interface + impl, TTL cache
│   ├── db/                   # Room entities, DAOs, database, entity<->domain mappers
│   ├── datastore/            # SettingsRepository (DataStore Preferences)
│   ├── mapper/               # DTO -> domain mappers
│   └── model/                # Kotlinx-serialization API DTOs
├── domain/model/             # Domain models (Player, Team, Fixture, Entry, ...)
├── notifications/            # ReminderScheduler + WorkManager worker for gameweek deadline alerts
├── ui/                       # One package per screen + shared components + theme
└── util/                     # Pure, unit-tested logic (search, player-list, formatting)
```

## Layers

1. **API DTOs** (`data/model`) — `@Serializable` classes mirroring the FPL JSON schema. Only the
   `data.mapper` package and `FplApi` touch these.
2. **Domain models** (`domain/model`) — typed, UI-facing models (`Player.form` is a `Double`, not a
   string). Compose never sees a DTO.
3. **Database entities** (`data/db`) — Room `@Entity` classes used for the offline cache.
4. **UI state** — each screen owns a small immutable `*UiState` data class produced by its ViewModel.

Mappers are explicit extension functions (`DtoMappers.kt`, `EntityMappers.kt`) so no model leaks
across a layer boundary.

## Data flow

- **Repository interface** `FplRepository` is what ViewModels depend on; the implementation
  (`FplRepositoryImpl`) owns the networking + caching strategy.
- **Cache-aside** read path: in-memory TTL → Room (if fresh) → network (persist to Room).
- Network failures fall back to the most recent cached copy, flagged `stale = true` via the
  `FetchResult` sealed class (`Success`/`Error`).
- `observeBootstrap()` exposes a Room-backed `Flow<Bootstrap?>` so the Home and Players screens
  update reactively without polling.

## Dependency injection

Manual DI: `FplApp` constructs the `FplApi` (OkHttp/Retrofit), the Room `FplDatabase`, the
`FplRepositoryImpl`, and `SettingsRepository`. ViewModels are created through the small factory
helpers in `di/ViewModelDi.kt` (`fplViewModel`, `fplViewModelWithArgs`).

## Dependencies

- Compose (BOM 2024.10.01) + Material 3 + material-icons-extended
- Navigation Compose
- Retrofit + kotlinx-serialization converter, OkHttp (disk cache + User-Agent)
- Room (runtime/ktx/compiler via KSP) for the offline cache
- DataStore Preferences for settings
- kotlinx-coroutines / Flow

## Testing

- Pure JVM tests: mappers, entity mappers, `SearchLogic`, `PlayerListLogic`, `TtlCache`.
- ViewModel tests with a fake `FplRepository` and `kotlinx-coroutines-test`.
- Robolectric tests for the Room DAOs and the repository cache-aside behavior.
