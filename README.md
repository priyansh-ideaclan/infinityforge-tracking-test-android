# InfinityForge Tracking Test (Android)

This repository is the **InfinityForge Tracking** integration test app for
Android/Kotlin — a dedicated Android application, configured from the [Native Android
App Factory](#native-android-app-factory) below, whose purpose is to prove that
`Android/Kotlin app -> Kotlin Tracking implementation -> Firebase provider -> Firebase
Analytics -> Firebase DebugView` works, using the same platform-agnostic contract
defined in `infinityforge-tracking-module` that the existing React Native and Swift
integration test apps already verify.

- **Central contract** (what every platform must implement identically — events,
  metrics, identity, validation, privacy, versioning): lives in
  `infinityforge-tracking-module`, not in this repository.
- **Kotlin Tracking implementation** (this app's implementation of that contract): not
  yet written. This repository currently only carries app identity/configuration
  ("Phase 6A"); the Kotlin Tracking module itself is a later phase ("Phase 6B").
- **Android test application** (this repo's UI/DI/build wiring): the factory-provided
  Home/Settings/navigation/DI scaffold described below, configured with this app's own
  identity. It does not yet contain any tracking-specific screens or calls.
- **Firebase provider**: not yet configured. No `google-services.json`, no Firebase
  project, no credentials exist in this repository yet (see
  `Docs/setup/firebase.md`).
- **Runtime Firebase DebugView verification**: not yet performed. Nothing in this
  repository has been run against a real Firebase project.

The rest of this document (below) is the underlying factory's own documentation and
still accurately describes how this app was generated and how it can be reconfigured.

## Native Android App Factory

A production-quality Kotlin/Jetpack Compose Android **template + factory**. Clone this
repository, describe your app in `APP_SPEC.yaml`, run one script, and get a configured,
buildable Android app with authentication, networking, local storage, ads, in-app
purchases, and analytics already wired through clean, testable boundaries.

> Start here if you're new: read `AGENTS.md` (rules), then `ARCHITECTURE.md` (how it's
> built), then this file's "Quick start" below.

## What you get

- Kotlin + Jetpack Compose + Material 3, Gradle Kotlin DSL, version-catalog-driven.
- Hilt DI, Coroutines/Flow, Navigation Compose (type-safe routes).
- Retrofit/OkHttp networking, Room database, DataStore preferences.
- **Optional** Firebase Authentication (none / email-password / Google / anonymous)
  behind an app-owned `AuthRepository` — swap providers without touching feature UI.
- **Optional** Google Mobile Ads (AdMob) behind an `AdsController` boundary.
- **Optional** RevenueCat purchases behind a `PurchasesController` boundary
  (`isPremium`, not RevenueCat types, is what features see).
- Firebase Analytics/Crashlytics behind an `AnalyticsTracker` boundary with a
  documented, typed event catalog (`Docs/analytics/events.md`).
- dev/staging/prod build environments, light/dark/system themes, dynamic color.
- Idempotent Python factory automation (`scripts/`) and a single canonical
  verification entry point (`./scripts/verify.sh`).
- CI that never requires production secrets.

## Quick start (configuring a new app from this factory)

```bash
# 1. Clone this factory as the starting point for your new app's repository.
git clone <this-repo-url> my-new-app
cd my-new-app

# 2. Edit APP_SPEC.yaml: app name, package name, which optional capabilities are on.

# 3. Validate the spec (no changes made).
python scripts/validate_spec.py APP_SPEC.yaml

# 4. Configure the app (idempotent — safe to re-run).
python scripts/configure_app.py APP_SPEC.yaml

# 5. Follow the external-setup checklist configure_app.py prints (Firebase project,
#    google-services.json, RevenueCat/AdMob keys) — see Docs/setup/.

# 6. Verify everything the factory can verify without real credentials.
./scripts/verify.sh
```

See "Cloning this factory into a new application" in `ARCHITECTURE.md` for the full,
step-by-step version including Play Console and signing setup.

## Requirements

- JDK 17, Android Studio (current stable), Android SDK platform 36 + build-tools.
- Python 3.12+.
- See `Docs/plans/2026-08-31-native-android-factory-v1.md` §5 for the exact verified
  dependency versions and why each was chosen.

## Repository map

| Path | What it is |
|---|---|
| `AGENTS.md` / `CLAUDE.md` | Rules and workflow for anyone (human or AI) working here |
| `ARCHITECTURE.md` | Module map, dependency direction, how the factory is put together |
| `APP_SPEC.yaml` | The product config for *your* app |
| `MODULES.yaml` | The capability catalog the factory understands |
| `build-logic/` | Gradle convention plugins |
| `core/`, `feature/`, `ads/`, `purchases/`, `app/` | The Android project |
| `scripts/` | Python factory automation |
| `Docs/` | Plans, decisions, module docs, setup guides, release/testing docs |
| `CHANGELOG.md` | What actually shipped, in order |

## Status

This is the V1 factory build. Track live progress and exact remaining work in
`Docs/sessions/CURRENT.md` and the active plan under `Docs/plans/`.
