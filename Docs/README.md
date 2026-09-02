# Docs/ index

- `plans/` — persistent implementation plans (one per major body of work). The active
  one is pointed to by `sessions/CURRENT.md`.
- `decisions/` — short ADRs (Architecture Decision Records) for choices worth recording
  independently of the plan they came from (dependency choices, minSdk/targetSdk, module
  boundaries, etc.).
- `sessions/CURRENT.md` — single always-up-to-date pointer to "what's the active plan,
  what's the exact next action." Read this first when resuming work.
- `setup/` — external setup guides (Firebase, RevenueCat, AdMob, signing) — the steps a
  human must do outside this repository, with exactly what remains unverified until they do.
- `modules/` — one doc per core/feature module: purpose, public API, what it depends on,
  what must never depend on it.
- `testing/` — testing strategy, fakes catalog, module-combination matrix, CI notes.
- `release/` — build/release/signing/Play Console/store-listing documentation.
- `troubleshooting/` — known issues and their resolutions.
- `analytics/events.md` — the analytics event catalog (name, properties, trigger,
  business meaning) for every typed event the factory defines.
- `INFINITYFORGE_TRACKING.md` — status of the InfinityForge Tracking integration in
  this specific app (not the factory template): what's implemented vs. planned, across
  the contract, Kotlin implementation, Android test app, and Firebase provider layers.
