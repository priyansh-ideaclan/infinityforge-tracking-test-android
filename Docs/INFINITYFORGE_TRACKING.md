# InfinityForge Tracking — status in this repository

This document tracks, honestly and only, what InfinityForge Tracking work actually
exists in this repository today. It intentionally does not describe the Swift or React
Native implementations' internals — see those repositories' own docs for that — this
file states only what is true here, in the Android/Kotlin app.

## Layers, and what exists for each

| Layer | Owner repository | Status in this repo |
| --- | --- | --- |
| Central contract (events, metrics, identity, validation, privacy, versioning) | `infinityforge-tracking-module` | Not vendored here (this contract has no code artifact to vendor — see its own README). This app implements it at contract version 1.2.0 — see `core/core-tracking/.../InfinityForgeContractVersion`. |
| Kotlin Tracking implementation | `core/core-tracking` (this repo) | **Implemented (Phase 6B).** All six core operations (`initialize`, `track`, `identify`, `setUserProperties`, `screen`, `reset`) plus the optional `recordMetric` — canonical event/metric catalogs, validation, identity persistence (via `core-datastore`), envelope/metadata generation, the provider boundary, and a `TrackingModule` Hilt selection. Not yet exercised by any UI — see below. |
| Android test application (Home/Settings/navigation/DI scaffold) | this repo | Present, inherited from the Native Android App Factory. `FactoryApplication.onCreate()` now calls `InfinityForgeTrackingClient.initialize()` once, fire-and-forget (specification/api.md). No other app/UI code calls `track()`/`identify()`/`screen()`/`recordMetric()` yet — the dedicated Tracking test UI (Phase 6C) is what will do that; nothing in `feature-*` or `app/` builds envelopes, validates, or talks to Firebase directly (`core-tracking` is the only place that does). |
| Firebase provider | `core/core-tracking/.../firebase` (this repo) | **Implemented (Phase 6B).** `FirebaseInfinityForgeProvider` + `FirebaseInfinityForgeMapping` (pure, SDK-free translation functions, unit-tested independent of the Firebase SDK). Selected by `TrackingModule` only when a real `google-services.json` is present; otherwise a debug-only logging provider (in debug builds) or no provider at all (release), exactly mirroring `core-analytics`'s own `AnalyticsModule` selection pattern. |
| Real Firebase project / `google-services.json` / DebugView verification | external, per-developer | Not configured. No credentials, service-account keys, or `google-services.json` exist in this repository. No runtime Firebase call has been made or verified — `FirebaseInfinityForgeProvider` compiles and is unit-testable, but has not sent data to a real Firebase project. |

## App identity (configured — Phase 6A)

- App name: `InfinityForge Tracking Test`
- Application ID / package: `com.ideaclan.infinityforgetrackingtestkotlin`
- Room database name: `infinityforge_tracking_test.db`

Configured via the factory's supported `APP_SPEC.yaml` + `scripts/configure_app.py`
workflow — see `ARCHITECTURE.md`'s "Cloning this factory into a new application"
section for the mechanics.

## Kotlin Tracking core (implemented — Phase 6B)

`core-tracking` is a new, always-on core module (`MODULES.yaml`), following the same
architectural boundary `core-analytics` already establishes for this factory's own
(separate, older, closed-taxonomy) analytics capability — see "Why two analytics
systems?" below.

- **Client / API** (`InfinityForgeTrackingClient`): `initialize()` is `suspend`; every
  other operation is synchronous and fire-and-forget (specification/api.md — tracking
  must never block a user-facing action). `NoOpInfinityForgeTrackingClient` exists for
  parity with the reference Swift/RN implementations and for tests, but is not
  currently wired by `TrackingModule` — this app has no `APP_SPEC.yaml`-driven on/off
  switch for InfinityForge Tracking (a deliberate Phase 6B scope decision; see below).
- **Identity** (`InfinityForgeIdentity`): `anonymous_id`/`user_id`/user properties
  persisted through `core-datastore`'s `PreferencesDataSource` (three new
  `PreferenceKeys` entries) — no second persistence mechanism. `reset()` always
  establishes a new `anonymous_id`, never reuses the old one
  (specification/identity.md's privacy rationale).
- **Event/metric catalogs + validation** (`InfinityForgeEventCatalog`/`InfinityForgeEvent`/
  `InfinityForgeEventValidation`, and the metric equivalents): mirror
  `events/*.yaml`/`metrics/*.yaml` field-for-field at contract 1.2.0, including the
  `transaction_id` properties added to `subscription_started`/`subscription_cancelled`/
  `purchase_completed` and the full Metrics capability (`revenue`, `ad_impression`,
  `ad_revenue`, `session_duration`, `app_launch_duration`, `screen_load_duration`,
  `operation_duration`, `handled_error`).
- **Envelope/metadata** (`InfinityForgeEnvelope.kt`, `InfinityForgeMetadata`): `app_id`/
  `app_version` are read from this device's own `PackageInfo` (no core module reads
  `BuildConfig` — see `AppModule`'s own rule); `environment` is derived from the
  existing `EnvironmentConfig.name` (`"dev"`/`"staging"`/`"prod"`, one per product
  flavor) mapped onto `development`/`preview`/`production`.
- **Provider boundary + failure isolation** (`InfinityForgeTrackingProvider`,
  `InfinityForgeDispatcher`): every provider call runs on its own coroutine, a
  provider's failure is caught and logged (never rethrown to the application or to
  another provider), and coroutine cancellation is never swallowed
  (specification/errors.md).
- **Firebase provider + mapping** (`core/core-tracking/.../firebase`): see the table
  above.

## Why two analytics systems?

`core-analytics`'s `AnalyticsTracker`/`AnalyticsEvent` (this factory's own, pre-existing
capability) and `core-tracking`'s `InfinityForgeTrackingClient` are deliberately not
merged — the same disclosed divergence the reference Swift implementation documents.
`AnalyticsTracker` is a closed, app-specific event taxonomy with no cross-app
compatibility guarantee; `InfinityForgeTrackingClient` implements a versioned,
platform-agnostic contract shared with the Swift and React Native test apps
specifically so their data is comparable. Merging them would either weaken the
contract's guarantees or force unrelated app-specific events through it.

## Intentional Phase 6B scope decisions

- **No `APP_SPEC.yaml` toggle for InfinityForge Tracking.** The reference Swift
  implementation gates the whole capability behind an app-level
  `telemetry.infinityForgeTracking.enabled` flag. Adding an equivalent here would mean
  extending `APP_SPEC.yaml`'s schema and `scripts/configure_app.py` — schema/tooling
  work outside "implement the reusable Kotlin Tracking core." `TrackingModule` always
  provides the real client; only the *provider list* varies (Firebase, debug-logging,
  or none).
- **No Tracking test UI.** Nothing in `feature-*` calls `track()`/`identify()`/
  `screen()`/`recordMetric()` yet — that is Phase 6C's job.
- **No real Firebase project.** `FirebaseInfinityForgeProvider` is implemented and
  unit-tested against pure mapping functions, but has never run against live
  credentials — see Docs/setup/firebase.md for what remains external, per-developer
  setup.
