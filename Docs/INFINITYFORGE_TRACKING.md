# InfinityForge Tracking — status in this repository

This document tracks, honestly and only, what InfinityForge Tracking work actually
exists in this repository today. It intentionally does not describe the Swift or React
Native implementations' internals — see those repositories' own docs for that — this
file states only what is true here, in the Android/Kotlin app.

## Layers, and what exists for each

| Layer | Owner repository | Status in this repo |
| --- | --- | --- |
| Central contract (events, metrics, identity, validation, privacy, versioning) | `infinityforge-tracking-module` | Not vendored here. This app will consume it as the single source of truth once the Kotlin implementation begins. |
| Kotlin Tracking implementation (the Android equivalent of Swift's `Core/InfinityForgeTracking/` and RN's `src/modules/analytics/`) | this repo (planned) | **Not implemented yet.** No tracking types, catalogs, validators, or client exist in this repository. |
| Android test application (Home/Settings/navigation/DI scaffold) | this repo | Present, inherited from the Native Android App Factory, configured with this app's own identity (see below). Contains no tracking-specific screens, calls, or logic. |
| Firebase provider (the Android equivalent of Swift's `FirebaseInfinityForgeProvider`) | this repo (planned) | Not implemented yet. |
| Real Firebase project / `google-services.json` / DebugView verification | external, per-developer | Not configured. No credentials, service-account keys, or `google-services.json` exist in this repository. |

## App identity (configured — Phase 6A)

- App name: `InfinityForge Tracking Test`
- Application ID / package: `com.ideaclan.infinityforgetrackingtestkotlin`
- Room database name: `infinityforge_tracking_test.db`

Configured via the factory's supported `APP_SPEC.yaml` + `scripts/configure_app.py`
workflow — see `ARCHITECTURE.md`'s "Cloning this factory into a new application"
section for the mechanics. No Xcode/Swift/RN-specific tooling or files were touched to
produce this; this repository was derived directly from `native-android-app-factory`.

## What "done" will look like

Once the Kotlin Tracking module (Phase 6B) and a real Firebase project are both in
place, this document should be replaced with one that mirrors
`infinityforge-tracking-test-swift`'s tracking doc in structure (API shape, event/metric
catalogs, identity/reset behavior, provider mapping, and a runbook for reproducing a
Firebase DebugView event) — adapted to Kotlin/Android idioms, not copied verbatim.
Until then, any statement about Kotlin-side tracking behavior should be treated as
aspirational, not implemented.
