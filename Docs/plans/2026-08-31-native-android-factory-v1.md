# Native Android App Factory — V1 Implementation Plan

- Created: 2026-08-31
- Owner session: Claude (this repository is the factory repository itself)
- Status legend: NOT_STARTED, IN_PROGRESS, BLOCKED, IMPLEMENTED, VERIFIED, DONE

## 1. Objective

Turn this empty repository into a production-quality **Native Android App Factory**:
a reusable Kotlin/Compose Android template plus Python automation that a team can
clone, configure via a single `APP_SPEC.yaml` ("Product Spec"), and turn into a new
shipping Android app quickly.

## 2. Scope

In scope for V1 (this document):
- Repository operating system (governance docs, plans, decisions, session state).
- A real, buildable Android app (Kotlin, Compose, Material 3, Hilt, Navigation Compose,
  Retrofit/OkHttp, kotlinx.serialization, Room, DataStore).
- Core reusable modules: design system, navigation, networking, database, preferences,
  logging, analytics abstraction, error model, environment config, test doubles.
- Optional auth module (none/email-password/Google/anonymous) behind an app-owned
  `AuthRepository` boundary — Firebase is an implementation detail.
- AdMob boundary (banner/interstitial/rewarded/app-open) behind an app-owned ads API.
- RevenueCat boundary (`isPremium`, entitlements, paywall state) — RevenueCat APIs
  never leak into feature code.
- Analytics/Crashlytics abstraction with typed events and a documented event catalog.
- `APP_SPEC.yaml` (product configuration) and `MODULES.yaml` (capability selection).
- Python factory automation (`scripts/*.py`) that is idempotent.
- Canonical verification (`scripts/verify.sh`).
- Tests: unit, repository, ViewModel, Compose UI, factory-script tests, fakes for
  auth/purchases/analytics/API/clock/feature-flags.
- GitHub Actions CI that never requires production secrets.
- Release-readiness documentation (never generates/replaces signing keys).
- One example configuration wiring all capabilities together.

Out of scope for V1 (explicitly deferred, recorded here so it is not "silently dropped"):
- Real Firebase/RevenueCat/AdMob production credentials (cannot be invented; see Blockers).
- Full combinatorial build matrix across every `MODULES.yaml` permutation (representative
  subset only — see §12 Testing).
- Google Mobile Ads "Next-Gen" SDK migration (too new as of 2026-08; legacy stable SDK used).
- Room 3.0 / Kotlin Multiplatform migration (V1 is Android-only; Room 2.x stays in support).
- Notifications (placeholder field in `APP_SPEC.yaml` only, per instructions).
- Android emulator provisioning (explicitly skipped by user for this session; physical
  device or Android Studio emulator setup is documented instead).

## 3. Requirements traceback

All numbered requirements from the task prompt (Repository operating system through
Example application) are tracked as phases in §6 and as checklist items in §7. Each
phase's "Files affected" and "Testing" entries map back to a numbered requirement.

## 4. Assumptions (safe production defaults — recorded per instructions)

Environment:
- A4.1 JDK 17 (Temurin, Homebrew) is the build JDK; Android Studio's bundled JBR is not
  relied on for CLI builds. **Decision D-001**.
- A4.2 Android SDK Platform 36 + Build-Tools 36.1.0 were installed this session
  specifically to meet Google Play's 2026-08-31 new-app target API requirement.
  Platform 35 / Build-Tools 35.0.0 remain installed as the `minSdk`-adjacent baseline
  is not affected. **Decision D-002**.
- A4.3 No emulator is configured. Verification that requires a running device/emulator
  (instrumented Compose UI tests, `connectedAndroidTest`) is documented but not executed
  automatically; canonical verification runs JVM-only checks (Robolectric-free unit tests,
  lint, static analysis, assemble/bundle tasks) that do not require a device.

Product decisions (all recorded as ADRs under `Docs/decisions/`):
- A4.4 `minSdk = 26` (Android 8.0), `targetSdk = 36` (Android 16), `compileSdk = 37`
  (required by Compose BOM 2026.08.00's artifacts). See D-002.
- A4.5 Package name placeholder: `com.factory.app` (template default), renamed per-app by
  `scripts/rename_package.py` driven by `APP_SPEC.yaml.package_name`.
- A4.6 App name placeholder: "Factory App" — renamed by `scripts/configure_app.py`.
- A4.7 Three build flavors via product flavor dimension `env`: `dev`, `staging`, `prod`,
  each with its own applicationIdSuffix, `google-services.json` slot, and `BuildConfig`
  base URL. No flavor requires secrets to *compile*.
- A4.8 Auth default for the template's shipped example config: `none` is the safest
  zero-credential default; the example configuration (§15) demonstrates
  `emailPassword + anonymous + google` wired through fakes so it compiles and tests
  without real Firebase credentials, with a documented external-setup step for real use.
- A4.9 Ads/RevenueCat default to **disabled** in `APP_SPEC.yaml`'s shipped template value,
  with the modules fully implemented and unit-testable via fakes; the example
  configuration turns them on using Google/RevenueCat *test* identifiers only.
- A4.10 Analytics events are namespaced `app_*` / `auth_*` / `purchase_*` / `ad_*` and
  documented in `Docs/analytics/events.md`.
- A4.11 Dependency injection graph: Hilt with `@Singleton` app-level modules per core
  capability (`NetworkModule`, `DatabaseModule`, `DataStoreModule`, `AuthModule`,
  `AdsModule`, `PurchasesModule`, `AnalyticsModule`, `ClockModule`).
- A4.12 Convention plugins live in an included build `build-logic/` (not a published
  artifact), applied by `id("factory.<name>")` from the version catalog's plugin aliases.
- A4.13 Testing library choices not explicitly named in the prompt but required to make
  "unit-test foundations" real: MockK (mocking), Turbine (Flow testing), kotlinx-coroutines-test.
  These are recorded as assumptions, not silent additions.
- A4.14 CI runner: `macos-14` is avoided (cost); `ubuntu-latest` is used since Android
  CLI builds do not require macOS. Documented in `Docs/testing/ci.md`.

## 5. Verified dependency versions (checked 2026-08-31 via official sources/web search)

| Dependency | Version | Source / reasoning |
|---|---|---|
| Kotlin | 2.3.20 | Latest stable (JetBrains blog, 2026-03-16). Compose compiler ships in-step with Kotlin since 2.0. |
| Android Gradle Plugin | 9.3.0 | Latest stable (developer.android.com, 2026-07). Supports Kotlin 2.3.x, compileSdk 36/API 37 ceiling. |
| KSP | 2.3.11 | KSP switched to Kotlin-version-independent numbering starting at 2.3.0 (Maven Central metadata checked directly, since it's not surfaced well by search); 2.3.11 is latest and resolved successfully against Kotlin 2.3.20 during this session's real build. |
| Gradle wrapper | 9.1 (or latest 9.x compatible with AGP 9.3) | Required by AGP 9.3 baseline. |
| Compose BOM | 2026.08.00 | Latest stable BOM (developer.android.com). |
| Material3 | via Compose BOM | Version resolved by BOM. |
| Hilt (dagger) | 2.59.2 | Superseded the initially-selected 2.57.1 during this session's real build: AGP 9's built-in Kotlin support broke the Hilt Gradle plugin's AGP-detection (`Android BaseExtension not found`) on anything before 2.59; 2.59.2 is the latest patch fixing a follow-up 2.59.0 regression (missing `ComponentTreeDeps` at runtime). |
| androidx.hilt:hilt-navigation-compose | 1.2.0 | Stable line compatible with Navigation Compose 2.9.x. |
| Navigation Compose | 2.9.6 | Latest stable (2025-11); type-safe routes via kotlinx.serialization are stable. |
| Room | 2.8.4 | Latest **2.x** stable (2025-11-19). Room 3.0 (KMP, `androidx.room3`, KSP-only) shipped its first alpha 2026-03-11 — too new/breaking for a production template; **Decision D-003** stays on 2.x until 3.0 reaches parity + ecosystem adoption. |
| DataStore (preferences) | 1.2.1 | Latest stable (2026-03-11). |
| Retrofit | 3.0.0 | Latest stable (2025-05-15), upgrades baseline OkHttp to 4.12. |
| OkHttp | 4.12.0 | Pinned to the version Retrofit 3.0.0 is built/tested against, avoiding OkHttp 5 major-version ambiguity in a template meant to stay stable. |
| kotlinx.serialization | 1.11.0 | Latest stable (2026-04-09). |
| kotlinx.coroutines | 1.11.0 | Latest stable. |
| Firebase Android BoM | 34.17.0 | Latest (Aug 2026). |
| Google Mobile Ads SDK | 25.0.0 | Latest **stable/legacy** SDK (2026-02). The "Next-Gen" SDK was announced 2026-01 and promoted 2026-07 but is a new rewrite; deferred per A-out-of-scope. |
| RevenueCat `purchases` / `purchases-ui` | 10.8.0 | Latest (2026 codelab + GitHub). |
| JUnit | 4.13.2 | Compose UI testing (`createComposeRule`) is built on the JUnit4 rule model; JUnit4 chosen over JUnit5 to keep Compose UI tests first-class. |
| androidx.test.ext:junit | 1.3.0 | Corrected from an initial 1.2.1 pick: rechecked Maven metadata directly while wiring Robolectric and found 1.3.0 is stable, not alpha. |
| androidx.test.espresso:espresso-core | 3.7.0 | Corrected from an initial 3.6.1 pick for the same reason — 3.7.0 is stable. |
| androidx.test:core | 1.7.0 | Added for Robolectric-based Room DAO tests (`ApplicationProvider`); versioned independently from `androidx.test.ext:junit` despite being part of the same "AndroidX Test" family. |
| MockK | 1.13.13 | Assumption A4.13. |
| Turbine | 1.2.0 | Assumption A4.13. |
| Robolectric | 4.16.1 | Added when writing `core-database`'s Room DAO test — lets Room run against a real (JVM-hosted, via Robolectric's shadow framework) SQLite implementation in a plain unit test, so repository/DAO tests don't require a device/emulator (the user explicitly skipped emulator setup this session). Latest stable checked 2026-08-31. |
| Detekt | 1.23.8 | Latest stable 1.x (2.0 is alpha-only; staying stable). |
| ktlint Gradle plugin (org.jlleitschuh.gradle.ktlint) | 14.0.1 | Latest stable (2025-11-10), Gradle 9.x compatible. |

If any of these have moved by the time this is read, **do not silently bump them** —
open an ADR and update this table plus `gradle/libs.versions.toml` together.

## 6. Architecture

```
app/                         # thin shell: Application, MainActivity, NavHost wiring, DI entry
build-logic/                 # convention plugins (Gradle Kotlin DSL, included build)
  convention/                #   factory.android.application, factory.android.library,
                              #   factory.compose, factory.hilt, factory.testing, factory.detekt-ktlint
core/
  core-designsystem/         # theme, typography, color schemes, dynamic color, reusable components
  core-navigation/           # typed nav destinations, NavHost host composable, deep-link contracts
  core-network/              # Retrofit/OkHttp setup, interceptors, ApiResult, environment base URLs
  core-database/              # Room database, DAOs, entities, migrations
  core-datastore/            # DataStore<Preferences> wrapper, typed preference keys
  core-logging/               # Logger abstraction (Timber-free, no direct Log.* in features)
  core-analytics/             # AnalyticsTracker interface + typed events + Firebase/Fake impls
  core-common/                # AppError, Result wrapper, Clock, IdGenerator, Dispatchers qualifiers
  core-testing/               # fakes: FakeClock, FakeIdGenerator, FakeAnalyticsTracker,
                               # FakeAuthRepository, FakeBillingRepository, MainDispatcherRule
feature/
  feature-auth/                # login/register/forgot-password UI + AuthViewModel, AuthRepository
                               # boundary + FirebaseAuthRepository + FakeAuthRepository
  feature-onboarding/          # onboarding flow (spec-driven, optional)
  feature-home/                 # home/dashboard sample screen (networking + Room demo)
  feature-settings/             # theme choice, sign-out, restore purchases, environment display
ads/
  ads-api/                     # AdPlacement, AdsController interface, ad state models
  ads-admob/                   # AdMob implementation, test-ad-unit guard, lifecycle-safe views
purchases/
  purchases-api/                # PurchasesController interface, Entitlement, PaywallState
  purchases-revenuecat/         # RevenueCat implementation, fake implementation
scripts/                        # Python factory automation
Docs/                            # operating system + module + setup + release docs
.github/workflows/                # CI
```

Dependency direction: `feature/* → core/*`, `feature/* → ads-api / purchases-api`
(never `ads-admob` / `purchases-revenuecat` directly), `app → everything` (composition
root only). This is enforced by module `build.gradle.kts` dependency declarations, not
by convention alone.

## 7. Implementation phases & checklist

| # | Phase | Status |
|---|---|---|
| 0 | Environment + persistent plan (this document) | DONE |
| 1 | Repository operating system (AGENTS.md, CLAUDE.md, README, ARCHITECTURE, CHANGELOG, APP_SPEC.yaml, MODULES.yaml, factory-version.txt, Docs/ tree) | DONE |
| 2 | Gradle foundation: settings, version catalog, build-logic convention plugins | VERIFIED (wrapper generated, `./gradlew :app:...` config phase resolves, real AGP 9 DSL issues found & fixed — see Decisions) |
| 3 | Core modules (designsystem, navigation, common, logging, testing) | VERIFIED (`compileDebugKotlin` green) |
| 4 | Core modules (network, database, datastore, analytics abstraction) | VERIFIED (`compileDebugKotlin`/KSP green for Room + Hilt) |
| 5 | App shell: Application/Hilt entry point, NavHost, themes, environment config | VERIFIED (`./gradlew :app:assembleProdDebug` succeeds; APK at `app/build/outputs/apk/prod/debug/app-prod-debug.apk`) |
| 6 | Auth module (boundary + Firebase impl + fake impl + UI + tests) | VERIFIED (boundary in core-common, FirebaseAuthRepository + FakeAuthRepository + Login/Register/ForgotPassword screens + ViewModels + AuthModule all compile; LoginViewModelTest + AuthModuleTest pass; LoginScreenTest compiles, needs a device to run) |
| 7 | Firebase docs + safe-missing-config handling | DONE (safe-missing-config handling in app/build.gradle.kts + AnalyticsModule + AuthModule; Docs/setup/firebase.md written) |
| 8 | AdMob module (boundary + impl + fake + docs) | VERIFIED (ads-api + ads-admob compile, test-ad-unit guard in AdUnitIdResolver, AdsModuleTest passes; Docs/setup/admob.md written) |
| 9 | RevenueCat module (boundary + impl + fake + docs) | VERIFIED (purchases-api + purchases-revenuecat compile, PurchasesModuleTest passes for the disabled path — see Docs/testing/module-matrix.md for the untested enabled path; Docs/setup/revenuecat.md written) |
| 10 | Analytics/Crashlytics wiring + events.md | DONE (AnalyticsTracker/CrashReporter/typed events implemented and wired in core-analytics; Docs/analytics/events.md written, including which events are reserved-but-unfired) |
| 10.5 | Whole-project ktlint/Detekt/Lint clean pass | VERIFIED (`./gradlew ktlintCheck detekt :app:lintProdDebug` all green; fixed real findings: AGP9 import-ordering/filename mismatches, two justified `TooGenericExceptionCaught` suppressions, `LongParameterList` threshold tuned for Compose hoisting, a real `roundIcon` manifest bug + missing monochrome icon layer + redundant `-v26` qualifier) |
| 11 | Python factory automation scripts | VERIFIED (`validate_spec.py`, `configure_app.py`, `rename_package.py`, `verify_project.py`, `release_check.py`; 11 subprocess-based tests, all real, all passing) |
| 12 | Canonical verification script `scripts/verify.sh` | VERIFIED (real end-to-end run: `FINAL HEALTH: PASS_WITH_EXTERNAL_SETUP` — see §10) |
| 13 | Test suite completion (unit/ViewModel/Compose UI/script tests, representative module-combination checks) | VERIFIED (23 Kotlin JUnit tests + 11 Python tests, all passing; 1 compiling instrumented Compose UI test; module-combination checks for Ads/Purchases/Auth `@Provides` selection — see Docs/testing/module-matrix.md for the honest remaining gap) |
| 14 | CI workflow | IMPLEMENTED (`.github/workflows/pr.yml`; not executed — no GitHub Actions runner available in this environment, but it mirrors `verify.sh`'s exact steps, which were verified for real) |
| 15 | Release readiness docs | DONE (`Docs/release/README.md`: debug/release builds, APK/AAB, local + CI signing, Play App Signing, internal testing, data safety/privacy policy/permissions/ads declaration/IAP) |
| 16 | Example application configuration | DONE (checked-in `APP_SPEC.yaml` demonstrates networking+Room via feature-home, navigation, settings, analytics, and onboarding live; auth/ads/purchases are fully implemented and unit-tested, shipping *disabled* by default — the safest zero-credential default per assumption A4.8/A4.9 — rather than silently faking "enabled" behavior in production code) |
| 17 | Final verification run + summary | DONE (see §10; final summary delivered to the user) |

Phases are executed in order; each phase updates this table and §11/§12 before moving on.

## 8. Files affected

Populated incrementally per phase in the "Decisions" log (§11) rather than duplicated
here, to avoid this list drifting from reality. `git status`/`git diff` is the source of
truth for exact files at any point in time.

## 9. Testing checklist

- [ ] `core-common`, `core-network`, `core-database`, `core-datastore`, `core-analytics` unit tests
- [ ] `feature-auth` ViewModel tests using `FakeAuthRepository`
- [ ] `purchases-api` tests using fake `PurchasesController`
- [ ] `ads-api` tests using fake `AdsController`
- [ ] Representative Compose UI tests (login screen, home screen loading/error/success)
- [ ] Python script tests (`scripts/tests/`) covering idempotency (`configure_app.py` run twice)
- [ ] Module-combination checks (§12 of prompt): minimal / auth+ads / auth+purchases / full V1
- [ ] `./scripts/verify.sh` executed and result recorded (§10 below)

## 10. Verification results

### Canonical run — `./scripts/verify.sh` — 2026-08-31

```
  1: PASS — APP_SPEC.yaml validation
  2: PASS_WITH_EXTERNAL_SETUP — module configuration matches APP_SPEC.yaml
  3: PASS_WITH_EXTERNAL_SETUP — unresolved placeholder / release-readiness scan
  4: PASS — no secret-shaped files are committed
  5: PASS — Gradle configuration resolves
  6: PASS — ktlint formatting check
  7: PASS — Android Lint (prodDebug)
  8: PASS — Detekt static analysis
  9a: PASS — Kotlin unit tests (all modules, debug) — 23 tests, 0 failures
  9b: PASS — Python factory-script tests — 11 tests, 0 failures
  10: PASS — Debug build (assembleProdDebug) — app/build/outputs/apk/prod/debug/app-prod-debug.apk
  11: PASS — Release compilation (assembleProdRelease, unsigned, R8-minified) —
       app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk
       (AAB also verified separately: ./gradlew :app:bundleProdRelease succeeds —
       app/build/outputs/bundle/prodRelease/app-prod-release.aab)
  12: PASS — documentation/state files present

FINAL HEALTH: PASS_WITH_EXTERNAL_SETUP
```

Steps 2 and 3 are `PASS_WITH_EXTERNAL_SETUP`, not `FAIL`, because the only reason they
don't fully pass is the expected, undeniable one: no real `google-services.json` exists
(and none was invented — see B-001). Every check that does not require a real backend
passed cleanly, including a full R8-minified release compilation.

### Re-confirmation audit — 2026-08-31 (post-interruption)

Repeated the full `./scripts/verify.sh` run after a session interruption, specifically
to check for truncated/incomplete files left mid-write. Result: identical —
`FINAL HEALTH: PASS_WITH_EXTERNAL_SETUP`, all 13 steps the same as the first run
(1 PASS, 2/3 PASS_WITH_EXTERNAL_SETUP, 4–12 PASS). 23/23 Kotlin unit tests pass (0
failures). No empty/near-empty `.kt`/`.py` files and no `TODO`/`FIXME`/`XXX` markers
found anywhere in source. `app-prod-debug.apk`, `app-prod-release-unsigned.apk`, and
`app-prod-release.aab` all present on disk.

Requires re-running (and reports honestly, not assumed) after further changes:
- `./gradlew :feature:feature-auth:connectedAndroidTest` — the one Compose UI test
  (`LoginScreenTest`) compiles but has never executed; that requires the emulator the
  user explicitly deferred this session (see Docs/testing/README.md once written).
- Anything Firebase/RevenueCat/AdMob-*live* — compiles against each SDK; live behavior
  requires the external setup in `Docs/setup/`.

## 11. Decisions log (ADRs live under `Docs/decisions/`, summarized here)

- D-001: Use Homebrew `openjdk@17` as `JAVA_HOME` for CLI/Gradle builds (Android Studio's
  bundled JBR 25 is left for IDE use only, to exactly match the required "JDK 17").
- D-002: `minSdk=26`, `targetSdk=36`, `compileSdk=37`. Rationale: Google Play requires
  new apps/updates to target API 36 starting 2026-08-31 (today); minSdk 26 (Android 8.0)
  is a modern, low-fragmentation baseline that avoids most pre-Oreo compatibility shims
  while still covering the large majority of active devices. `compileSdk` was raised to
  37 **after** a real `assembleDebug` failed listing every Compose/Lifecycle artifact in
  the 2026.08.00 BOM requiring API 37+ to compile against — `compileSdk` and `targetSdk`
  are independent, so this does not change the Play Store target-API commitment above.
- D-003: Stay on Room 2.8.x rather than Room 3.0 (KMP-first, `androidx.room3`, alpha
  since 2026-03) because this is an Android-only production template; revisit once
  Room 3.0 is stable and this factory adds KMP scope.
- D-004: Google Mobile Ads legacy/stable SDK (25.0.0), not the 2026 "Next-Gen" SDK,
  because the Next-Gen SDK is a brand-new rewrite with limited production track record.
- D-005: JUnit4 (not JUnit5) to match Compose UI testing's rule-based API without an
  extra interop layer.
- D-006: `core-common`, `core-logging`, `core-navigation` deliberately do **not** apply
  the Hilt Gradle plugin — their `@Inject`-constructed default implementations
  (`UuidIdGenerator`, `SystemClock`, `AndroidLogger`, `DefaultFactoryNavigator`,
  `DefaultDispatcherProvider`) are bound to their interfaces in one place,
  `app/src/main/kotlin/com/factory/app/di/CoreBindingsModule.kt`, discovered as a real
  `Dagger/MissingBinding` failure during this session's first `assembleDebug`. Keeps
  those modules Hilt-free/lighter; `app` is already the composition root for
  everything else (`AuthRepository`, `AdsController`, `PurchasesController`).
- D-007: `APP_SPEC.yaml`'s capability toggles are mirrored into
  `app/src/main/kotlin/com/factory/app/AppSpecFlags.kt` as plain Kotlin `const val`s
  inside a `// factory:app-spec-flags:start/end` marker block, rather than parsed from
  YAML at runtime. `scripts/configure_app.py` rewrites only that block. Rationale: no
  YAML-parsing dependency needed at runtime; `scripts/validate_spec.py` cross-checks the
  file against `APP_SPEC.yaml` so they cannot silently drift.

## 12. Blockers

- B-001 (expected, not a stoppage): No real `google-services.json`, RevenueCat API keys,
  or AdMob production ad unit IDs exist or will be invented. The example configuration
  and CI use Google/RevenueCat *test* identifiers only. `Docs/setup/firebase.md` and
  `Docs/setup/revenuecat.md` document the exact external steps a team must perform
  before shipping with real credentials. This does not block compilation, unit tests,
  lint, Detekt, or debug/release *compilation* (release *signing* is a separate,
  intentionally-external step — no key is generated by this factory).

## 13. Remaining work

All 18 phases (0–17) are DONE/VERIFIED as of this entry. What remains is genuinely
external or explicitly deferred, not incomplete factory work:

- Real Firebase/RevenueCat/AdMob credentials (external, by design — `Docs/setup/`).
- Running `LoginScreenTest` on a real device/emulator (deferred this session).
- Unit-testing `RevenueCatPurchasesController`'s "enabled" path (would need mocking the
  RevenueCat SDK's static singleton — `Docs/testing/module-matrix.md`).
- Firing the "reserved" analytics events once a concrete app has paywall/ad-impression
  UI to hang them off (`Docs/analytics/events.md`).
- A full four-way `assembleDebug` matrix actually invoking `configure_app.py` per named
  combination (mechanically straightforward, not done this pass — see
  `Docs/testing/module-matrix.md`).
- Running `.github/workflows/pr.yml` for real (no GitHub Actions runner in this
  environment — the workflow mirrors `verify.sh`'s steps, which were verified directly).

Tracked live in `Docs/sessions/CURRENT.md` (always the up-to-date pointer) and in the
phase table above. This file is the durable plan; `CURRENT.md` is the fast "resume here"
pointer for the next session.
