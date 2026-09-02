# ADR-0002: Static Gradle module inclusion, runtime capability selection

- Status: Accepted (supersedes an initial design tried and reverted in the same session)
- Date: 2026-08-31

## Decision

`settings.gradle.kts` includes every module (`feature-auth`, `ads-api`/`ads-admob`,
`purchases-api`/`purchases-revenuecat`) unconditionally. Whether a capability is
*active* is a pure runtime decision, driven by `app/src/.../AppSpecFlags.kt` (which
`scripts/configure_app.py` keeps in sync with `APP_SPEC.yaml`) and each capability's
Hilt `@Provides` function, which binds the real implementation or its `Fake*`
counterpart accordingly.

## Context

The original design additionally tried to *exclude* unused optional modules at the
Gradle level (commenting out their `include(...)` line in `settings.gradle.kts` based
on `APP_SPEC.yaml`). The first real run of `configure_app.py` proved this fragile: it
commented out `feature-auth`/`ads-*`/`purchases-*` in `settings.gradle.kts` without
correspondingly removing `app/build.gradle.kts`'s `implementation(project(...))`
dependencies on those same modules (which are unconditional), producing an immediate
"project not found" build failure.

## Alternatives considered

1. **Toggle both files in lockstep.** Doable, but doubles the surface area
   `configure_app.py` must keep synchronized and doubles the ways it can drift.
2. **Static inclusion + runtime selection (chosen).** Simpler, and the `Fake*`
   implementations already exist for testing, so there's no separate "excluded" code
   path to maintain — disabled capabilities just always resolve to the fake at runtime.

## Consequences

- A configured app's APK includes the AdMob/RevenueCat/Firebase-Auth SDKs and factory
  boundary code even when that capability is off in `APP_SPEC.yaml`, at some (currently
  unmeasured) APK size cost. Acceptable for V1; revisit if a specific app's size budget
  demands true exclusion (R8 will already strip unreachable `Fake*`-only code paths to
  some degree, but not the underlying vendor SDK dependency itself).
- `MODULES.yaml` and `ARCHITECTURE.md` were updated to describe this model, not the
  discarded one.
