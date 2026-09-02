# Module docs index

One entry per module: purpose, public API surface, what it depends on, and what must
never depend on it. See `ARCHITECTURE.md` for the full dependency-direction rules this
index assumes.

## core/

| Module | Purpose | Public API | Must never depend on it |
|---|---|---|---|
| `core-common` | Cross-cutting primitives with no Android/vendor dependency: `AppError`/`AppResult`, `Clock`/`IdGenerator`/`DispatcherProvider`, `FeatureFlagProvider`, `EnvironmentConfig`, and the `AuthRepository` boundary + its `FakeAuthRepository`. | `AppError`, `AppResult`, `Clock`, `IdGenerator`, `DispatcherProvider`, `FeatureFlagProvider`, `EnvironmentConfig`, `AuthRepository`, `FakeAuthRepository` | Nothing — this is the dependency floor almost everything else sits on. |
| `core-logging` | The only `Logger` interface allowed; `AndroidLogger` strips verbose/debug logs from release. | `Logger`, `AndroidLogger` | — |
| `core-designsystem` | `FactoryTheme`, color/typography tokens, reusable accessible components (`FactoryLoadingIndicator`, `FactoryErrorState`). | `FactoryTheme`, `ThemeMode`, `FactoryLoadingIndicator`, `FactoryErrorState` | `ads-*`, `purchases-*`, any feature module (design system depends on nothing product-specific). |
| `core-navigation` | Type-safe route definitions (`FactoryDestination`) and `FactoryNavigator` (lets a ViewModel request navigation without holding a `NavController`). | `FactoryDestination`, `FactoryNavigator`, `NavigationCommand` | — |
| `core-network` | Retrofit/OkHttp/kotlinx.serialization wiring (`NetworkModule`) and `safeApiCall` — the one place Retrofit exceptions become `AppError`. | `NetworkModule` (Hilt), `safeApiCall` | Feature-specific API interfaces (those live in the feature module that owns the endpoint, e.g. `feature-home`'s `NotesApi`). |
| `core-database` | Room setup (`FactoryDatabase`, `DatabaseModule`) and the example `NoteEntity`/`NoteDao`. | `FactoryDatabase`, `NoteDao`, `NoteEntity`, `DatabaseName` | — |
| `core-datastore` | Typed `PreferencesDataSource` over `DataStore<Preferences>`, plus the canonical `PreferenceKeys`. | `PreferencesDataSource`, `PreferenceKeys` | — |
| `core-analytics` | `AnalyticsTracker`/`CrashReporter` boundaries, typed `AnalyticsEvent`s, Firebase + NoOp implementations, `AnalyticsModule` (selects between them). | `AnalyticsTracker`, `CrashReporter`, `AnalyticsEvent`, `FirebaseAnalyticsTracker`, `NoOpAnalyticsTracker` | — |
| `core-testing` | `testImplementation`/`androidTestImplementation`-only fakes: `FakeClock`, `FakeIdGenerator`, `FakeFeatureFlagProvider`, `FakeAnalyticsTracker`, `FakeDispatcherProvider`, `MainDispatcherRule`. | (all of the above) | Any `main` source set — this module must never ship in a release artifact. |

## ads/ and purchases/

| Module | Purpose | Public API | Must never depend on it |
|---|---|---|---|
| `ads-api` | `AdsController`/`BannerAdRenderer` boundaries, `AdPlacement`/`AdFormat`, and the real, always-available `FakeAdsController`/`FakeBannerAdRenderer`. | `AdsController`, `BannerAdRenderer`, `AdPlacement`, `FakeAdsController` | `ads-admob` (wrong direction). |
| `ads-admob` | `AdMobAdsController`/`AdMobBannerRenderer`, `AdUnitIdResolver` (test-ID guard), `AdsModule` (Hilt selection). | `AdsModule`, `AdUnitIdResolver` | Any `feature-*` module (features depend on `ads-api` only). |
| `purchases-api` | `PurchasesController` boundary, `PaywallState`/`PurchasePackage`, `FakePurchasesController`. | `PurchasesController`, `FakePurchasesController` | `purchases-revenuecat`. |
| `purchases-revenuecat` | `RevenueCatPurchasesController`, `PurchasesModule` (Hilt selection). | `PurchasesModule` | Any `feature-*` module. |

## feature/

| Module | Purpose | Depends on |
|---|---|---|
| `feature-auth` | Login/Register/ForgotPassword screens + ViewModels, `FirebaseAuthRepository`, `AuthModule` (Hilt selection vs. `FakeAuthRepository`). | `core-common` (for the `AuthRepository` interface it implements), `core-designsystem`, `core-navigation`, `core-analytics`. |
| `feature-onboarding` | Spec-driven welcome/value-prop steps; marks `onboarding_completed` in `core-datastore`. | `core-datastore`, `core-navigation`, `core-analytics`. |
| `feature-home` | The networking+Room example screen: `NotesApi`/`NotesRepository`/`HomeViewModel`, plus a banner ad via `ads-api`. | `core-network`, `core-database`, `ads-api`. |
| `feature-settings` | Theme/dynamic-color toggle, sign-out, premium status + restore purchases, environment display. | `core-datastore`, `purchases-api`, `core-common` (auth). |

## app/

The composition root — the only module allowed to depend on everything. Owns
`FactoryApplication` (Hilt entry point, conditional Firebase/AdMob/RevenueCat init),
`MainActivity` + `FactoryNavHost` (composes every feature's nav graph), `AppModule` +
`CoreBindingsModule` (every Hilt `@Provides`/`@Binds` that reads `BuildConfig` or picks a
default implementation for a `core-*` interface), and `AppSpecFlags` (the
machine-managed mirror of `APP_SPEC.yaml`'s capability toggles).
