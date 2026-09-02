# ADR-0001: minSdk 26 / targetSdk 36 / compileSdk 37

- Status: Accepted
- Date: 2026-08-31

## Decision

`minSdk = 26` (Android 8.0), `targetSdk = 36` (Android 16), `compileSdk = 37`.

## Context

Google Play requires new apps and updates to target API 36 starting 2026-08-31 (the day
this factory was built). `compileSdk` was initially set to 36 to match, but a real
`./gradlew :app:assembleDebug` failed, listing every Compose/Lifecycle artifact in
Compose BOM 2026.08.00 as requiring `compileSdk 37+`. `compileSdk` and `targetSdk` are
independent — raising the former to satisfy the compiler does not change what the app
declares it targets at runtime, so the Play Store requirement is still met at 36.

`minSdk 26` covers the large majority of active devices while dropping most pre-Oreo
compatibility shims (adaptive icons work unconditionally, no legacy notification channel
absence to special-case, etc.).

## Consequences

- `platforms;android-37.1` and `build-tools;37.0.0` must be installed locally (done this
  session via `sdkmanager`).
- Re-verify this decision whenever the Compose BOM is upgraded — a future BOM may raise
  the floor further.
