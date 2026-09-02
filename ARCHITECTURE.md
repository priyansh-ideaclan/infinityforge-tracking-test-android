# Architecture

## Module graph and dependency direction

```
                              app (composition root)
                 ┌───────────────┼────────────────────────┐
                 ▼               ▼                        ▼
        feature-auth      feature-onboarding    feature-home / feature-settings
                 │               │                        │
                 └───────┬───────┴────────────┬───────────┘
                         ▼                    ▼
                  ads-api / purchases-api   core-* (designsystem, navigation,
                         ▲                    network, database, datastore,
                         │                    logging, analytics, common, testing)
             ┌───────────┴───────────┐
             ▼                       ▼
        ads-admob            purchases-revenuecat
   (Google Mobile Ads SDK)      (RevenueCat SDK)
```

Rules enforced by module `build.gradle.kts` dependency declarations (not just
convention):

- `feature/*` depends on `core/*` and, where relevant, `ads-api` / `purchases-api`.
  It never depends on `ads-admob`, `purchases-revenuecat`, or Firebase SDK artifacts
  directly.
- `ads-admob` and `purchases-revenuecat` depend on their respective `*-api` module and
  the third-party SDK. Nothing outside `app`'s DI wiring depends on them.
- `app` is the only module allowed to see every module — it is the Hilt composition
  root that binds `AdsController → AdMobAdsController` (or `FakeAdsController`),
  `PurchasesController → RevenueCatPurchasesController` (or fake), `AuthRepository →
  FirebaseAuthRepository` (or fake), based on `APP_SPEC.yaml`-derived `BuildConfig`
  flags.
- `core-testing` (fakes, `MainDispatcherRule`, `FakeClock`, `FakeIdGenerator`) is a
  `testImplementation`/`androidTestImplementation`-only dependency of every other
  module — it never ships in a release artifact.

## Why boundaries instead of calling SDKs directly

Three concrete reasons, not abstraction for its own sake:
1. **Testability without credentials.** `FakeAuthRepository`, `FakePurchasesController`,
   `FakeAdsController`, and `FakeAnalyticsTracker` let every ViewModel and Compose
   screen be unit/UI-tested with zero network calls and zero real API keys — including
   in CI.
2. **Replaceability.** If the product later swaps RevenueCat for Play Billing directly,
   or AdMob for another mediation SDK, only `purchases-revenuecat`/`ads-admob` and the
   app's DI bindings change — feature code and its tests do not.
3. **Product-concept isolation.** Features should reason about `isPremium: Boolean` and
   `AdPlacement.BannerHome`, not RevenueCat `CustomerInfo` or AdMob `AdRequest` — this
   keeps feature code readable and keeps a vendor's API churn from rippling outward.

## Gradle structure

- `settings.gradle.kts` includes every module — `feature-auth`, `ads-*`, and
  `purchases-*` are always built, whether or not `APP_SPEC.yaml` turns that capability
  on. Which one is *active* is a runtime decision: `scripts/configure_app.py` writes
  the resolved booleans into `app/.../AppSpecFlags.kt`, and each capability's Hilt
  `@Provides` function reads them to bind the real implementation or its `Fake*`
  counterpart. (An earlier design also tried to exclude unused modules at the Gradle
  level; that required `settings.gradle.kts` and `app/build.gradle.kts`'s dependencies
  to be toggled in lockstep, and running `configure_app.py` for the first time
  demonstrated that's fragile — toggling one without the other breaks the build. Static
  inclusion + runtime selection is simpler and correct, at the cost of a slightly larger
  APK than a "truly minimal" build could achieve; not a concern for V1.)
- `gradle/libs.versions.toml` is the single source of dependency versions (see the plan
  document §5 for the reasoning behind each pinned version).
- `build-logic/` is an included build (via `pluginManagement { includeBuild("build-logic") }`)
  providing convention plugins: `factory.android.application`, `factory.android.library`,
  `factory.android.feature` (library + Compose + Hilt bundle for feature modules),
  `factory.compose`, `factory.hilt`, `factory.detekt-ktlint`. Every module applies one or
  more of these instead of repeating `compileSdk`/`minSdk`/lint config by hand.

## Environments (dev / staging / prod)

Implemented as an AGP product flavor dimension `env` with flavors `dev`, `staging`,
`prod` (see `A4.7` in the plan). Each flavor supplies:
- its own `applicationIdSuffix` (dev/staging only; prod has none),
- its own `google-services.json` slot under `app/src/<flavor>/google-services.json`
  (absent files are handled explicitly — see `Docs/setup/firebase.md`),
- a `BuildConfig.BASE_URL` sourced from `APP_SPEC.yaml.environments.<flavor>.base_url`.

## Theming

`core-designsystem` exposes `FactoryTheme(darkTheme: ThemeMode, dynamicColor: Boolean)`
wrapping Material 3's `dynamicLightColorScheme`/`dynamicDarkColorScheme` on API 31+ with
a static fallback scheme derived from `APP_SPEC.yaml` branding colors below API 31 or
when dynamic color is disabled. `ThemeMode` is `LIGHT`/`DARK`/`SYSTEM`, persisted via
`core-datastore`.

## Error handling

`core-common` defines a sealed `AppError` (network, auth, purchases, unknown, each
carrying a user-safe message key, never a raw exception message) and a
`Result<T, AppError>`-shaped wrapper (`AppResult`) used at every repository boundary.
Feature ViewModels map `AppResult` to UI state; they never catch raw `Throwable`/SDK
exception types directly.

## Cloning this factory into a new application

1. `git clone <factory-repo-url> <new-app-name>` — do **not** fork/nest; this produces
   an independent repository history for the new app.
2. `cd <new-app-name>` and point `origin` at the new app's own remote:
   `git remote set-url origin <new-app-remote-url>`.
3. Edit `APP_SPEC.yaml` for the new app's name, package, branding, and which optional
   capabilities (`auth`, `ads`, `purchases`, `onboarding`) are enabled.
4. `python scripts/validate_spec.py APP_SPEC.yaml`
5. `python scripts/configure_app.py APP_SPEC.yaml` — renames the package (via
   `scripts/rename_package.py` internally), rewrites app name/IDs/build config, includes
   only the Gradle modules the spec turned on, and prints the external-setup checklist
   (Firebase project, RevenueCat keys, AdMob ad unit IDs, signing).
6. Follow the printed checklist and the matching `Docs/setup/*.md` files.
7. `./scripts/verify.sh` and confirm `PASS` or `PASS_WITH_EXTERNAL_SETUP` (the latter is
   expected until real credentials are added).
8. Open in Android Studio, select a device/emulator, run.
