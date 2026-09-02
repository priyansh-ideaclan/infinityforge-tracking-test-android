# Changelog

All notable changes to the factory itself (not to apps generated from it) are recorded
here, newest first. Format is free-form but must say what actually shipped, not what
was attempted.

## [1.0.0] — V1 factory build

Canonical verification: `./scripts/verify.sh` → **`PASS_WITH_EXTERNAL_SETUP`** (all 13
steps pass; the two soft steps are soft only because no real `google-services.json`
exists, by design — see `AGENTS.md` §7). Re-confirmed via a completion audit after a
session interruption: identical result, 23/23 Kotlin unit tests, no incomplete files.

### Added

**Repository operating system**
- `AGENTS.md`, `CLAUDE.md`, `README.md`, `ARCHITECTURE.md`, `APP_SPEC.yaml`,
  `MODULES.yaml`, `factory-version.txt`.
- `Docs/plans/2026-08-31-native-android-factory-v1.md` (the persistent plan),
  `Docs/sessions/CURRENT.md`, `Docs/decisions/0001`–`0003` (ADRs), `Docs/setup/`
  (firebase/admob/revenuecat), `Docs/modules/README.md`, `Docs/testing/` (README +
  module-matrix), `Docs/release/README.md`, `Docs/troubleshooting/README.md`,
  `Docs/analytics/events.md`.

**Gradle foundation**
- `settings.gradle.kts`, `gradle/libs.versions.toml` (every dependency version verified
  against official sources — see the plan §5), `build-logic/` convention plugins
  (`factory.android.application`/`.library`/`.feature`, `factory.compose`,
  `factory.hilt`, `factory.lint`). Gradle wrapper 9.7.1.

**Core modules** — `core-common`, `core-logging`, `core-designsystem`, `core-navigation`,
`core-network`, `core-database`, `core-datastore`, `core-analytics`, `core-testing`. See
`Docs/modules/README.md` for what each owns.

**Optional-capability boundaries** — `ads-api`/`ads-admob` (AdMob), `purchases-api`/
`purchases-revenuecat` (RevenueCat), and `AuthRepository`/`FirebaseAuthRepository`/
`FakeAuthRepository` (in `core-common`/`feature-auth`). Each is selected at runtime by a
Hilt `@Provides` function reading `AppSpecFlags` — never a compile-time module swap
(see ADR-0002 for why that was tried and reverted).

**Feature modules** — `feature-auth` (login/register/forgot-password),
`feature-onboarding`, `feature-home` (the networking+Room example screen, with a banner
ad), `feature-settings` (theme, sign-out, premium status/restore purchases,
environment display).

**App shell** — `FactoryApplication` (Hilt entry point; conditional
Firebase/AdMob/RevenueCat init that never crashes without real credentials),
`MainActivity` + `FactoryNavHost` (composes every feature's nav graph), `AppModule` +
`CoreBindingsModule` (Hilt wiring for every `core-*` interface and every
`APP_SPEC.yaml`-driven external value).

**Factory automation (Python, `scripts/`)**
- `spec_lib.py` (shared YAML loading/schema validation), `validate_spec.py`,
  `configure_app.py` (idempotent; renames the package, rewrites build config/branding/
  flags, prints an external-setup checklist), `rename_package.py`, `verify_project.py`
  (spec/repo drift detection), `release_check.py` (release-blocking placeholder/key/
  signing checks). 11 subprocess-based tests in `scripts/tests/`.

**Canonical verification** — `scripts/verify.sh`: spec validation, drift check,
release-readiness scan, secret-file check, Gradle config check, ktlint, Android Lint,
Detekt, Kotlin + Python unit tests, debug build, unsigned release build (R8-minified),
doc/state check → one final `PASS`/`PASS_WITH_EXTERNAL_SETUP`/`FAIL`/`BLOCKED` line.

**Tests** — 23 Kotlin JUnit tests (repository/DAO via Robolectric+Room, ViewModel tests
via fakes, 3 module-combination `@Provides`-selection tests) + 11 Python tests, all
passing. 1 Compose UI instrumented test (`LoginScreenTest`) that compiles but requires a
device/emulator to execute (deferred this session — see `Docs/testing/README.md`).

**CI** — `.github/workflows/pr.yml`: mirrors `verify.sh`'s non-device steps on
`ubuntu-latest`; requires zero production secrets.

### Fixed (real bugs found via actual builds, not by inspection)

- AGP 9's built-in Kotlin support rejects `org.jetbrains.kotlin.android` outright;
  removed it from every convention plugin.
- Hilt 2.57.1 failed under AGP 9 (`Android BaseExtension not found`); moved to 2.59.2.
- KSP's Kotlin-independent versioning (2.3.11) discovered via Maven Central metadata
  directly, since it isn't well-documented elsewhere yet.
- `compileSdk` raised from 36 to 37 after a real `assembleDebug` failure named every
  Compose 2026.08.00 artifact requiring it; `targetSdk` stays 36 (Play Store's actual
  requirement — independent of `compileSdk`).
- Five `core-*` default implementations (`UuidIdGenerator`, `SystemClock`,
  `AndroidLogger`, `DefaultFactoryNavigator`, `DefaultDispatcherProvider`) had no Hilt
  binding at all — added `CoreBindingsModule`.
- Google Mobile Ads 25.0.0's load callbacks take `LoadAdError`, not `AdError`.
- RevenueCat 10.8.0's `PurchaseParams`/`PurchasesConfiguration` live under
  `com.revenuecat.purchases`, not `.models`.
- `configure_app.py`'s first real run revealed two bugs before they could ship: a
  regex mangling `buildConfigField`'s escaped-quote value (fixed by matching the whole
  value span instead of naively parsing embedded quotes), and a fragile
  settings.gradle.kts/app-build.gradle.kts dual-toggle design (replaced — see ADR-0002).
- `release_check.py`'s signing check false-positived on a comment that merely
  *mentioned* `signingConfig`; fixed to strip `//` comments before matching.
- A real manifest bug: `android:roundIcon` pointed at `ic_launcher` instead of
  `ic_launcher_round`, silently making the round icon resource "unused" per Lint —
  found and fixed via a real `:app:lintProdDebug` run, along with adding a missing
  monochrome (themed-icon) adaptive-icon layer and dropping a now-redundant `-v26`
  resource qualifier (minSdk is already 26).

### Known gaps (recorded honestly, not silently dropped — see the plan §12/§13 and
### `Docs/testing/module-matrix.md`)

- No real Firebase/RevenueCat/AdMob credentials exist or were invented (expected;
  `Docs/setup/` has the exact steps).
- The one Compose UI instrumented test has never executed (needs a device/emulator,
  deferred this session).
- `RevenueCatPurchasesController`'s "enabled" path isn't unit tested (would need mocking
  RevenueCat's static `Purchases.sharedInstance`).
- Several `AnalyticsEvent`s are defined but not yet fired from any call site
  (`app_opened`, `purchase_started`/`completed`/`failed`, all three `ad_*` events) — see
  `Docs/analytics/events.md`'s "Reserved" column.
- A full four-way `assembleDebug` matrix actually re-running `configure_app.py` per
  named combination (`minimal`/`auth_and_ads`/`auth_and_purchases`/`full_v1`) was not
  run this pass; the underlying `@Provides` selection logic each combination depends on
  is unit tested directly instead.
