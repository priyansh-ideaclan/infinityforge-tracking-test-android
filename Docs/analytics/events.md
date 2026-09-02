# Analytics event catalog

Every event below is defined as a typed `AnalyticsEvent` subclass in
`core/core-analytics/src/main/kotlin/com/factory/core/analytics/AnalyticsEvent.kt` —
that file and this document must be kept in sync. Feature code never calls
`AnalyticsTracker.track(...)` with an ad-hoc string; it constructs one of these types.

## Privacy-safe logging rules

- **Never** include a raw email address, password, name, or auth token in an event
  property. `AuthLoginFailed`'s `reason` is a category (`AppError.message`, already
  scrubbed at the repository boundary — see `core-common`'s `AppError`), not the
  underlying exception text.
- Property values are always short, low-cardinality strings (a method name, a
  placement/format enum name, a screen name) — never free-text user input.
- `CrashReporter.log()`/`recordException()` follow the same rule (see `core-analytics`'s
  `CrashReporter.kt`).

## Events

| Event | Properties | Trigger | Business meaning |
|---|---|---|---|
| `app_opened` | — | Reserved for app-level cold-start tracking (not yet fired automatically anywhere — a future app can call it from `FactoryApplication.onCreate()` if desired). | App launch funnel. |
| `app_screen_viewed` | `screen_name` | Fired from a screen's ViewModel `init` (currently: `HomeViewModel`). | Screen-level engagement/funnel analysis. |
| `app_onboarding_completed` | — | `OnboardingViewModel.finishOnboarding()`, whether the user finished all steps or tapped Skip. | Onboarding completion/drop-off rate. |
| `auth_login_succeeded` | `method` (`email_password`\|`google`\|`anonymous`) | `LoginViewModel.onSubmit()`/`onSignInAnonymously()` on success. | Login conversion by method. |
| `auth_login_failed` | `method`, `reason` (an `AppError.message`, not raw exception text) | Same call sites, on failure. | Login friction/error-rate diagnosis. |
| `auth_registration_succeeded` | `method` | `RegisterViewModel.onSubmit()` on success. | Signup conversion. |
| `auth_logout_succeeded` | — | `SettingsViewModel.onSignOut()`. | Session-length/re-engagement analysis. |
| `purchase_started` | `product_id` | Reserved — not yet fired (paywall UI is not implemented in V1; `PurchasesController.purchase()` is called directly by whatever UI a consuming app builds on the paywall state). | Purchase funnel entry. |
| `purchase_completed` | `product_id` | Reserved — fire from the paywall UI that calls `PurchasesController.purchase()`. | Revenue conversion. |
| `purchase_failed` | `product_id`, `reason` | Reserved — fire alongside `purchase_started` on failure. | Purchase friction diagnosis. |
| `purchase_restored` | — | `SettingsViewModel.onRestorePurchases()` on success. | Restore-flow usage (a strong signal of reinstall/device-switch behavior). |
| `ad_impression` | `placement`, `format` | Reserved — `AdsController`/`BannerAdRenderer` do not yet self-report impressions; wire this from AdMob's `OnPaidEventListener`/`FullScreenContentCallback.onAdShowedFullScreenContent()` if per-impression revenue tracking is needed. | Ad revenue/frequency analysis. |
| `ad_clicked` | `placement`, `format` | Reserved — same as above, AdMob's click callback. | Ad engagement. |
| `ad_failed_to_load` | `placement`, `format`, `reason` | Reserved — wire from `AdMobAdsController`'s `onAdFailedToLoad`. | Ad fill-rate diagnosis. |
| `settings_theme_changed` | `mode` (`LIGHT`\|`DARK`\|`SYSTEM`) | `SettingsViewModel.onThemeModeSelected()`. | Theme preference distribution. |

"Reserved" above means the typed event exists and is ready to use, but no call site
fires it yet in this V1 example configuration — recorded honestly rather than
pretending events fire that don't. Wiring them is a small, mechanical addition once a
concrete app has real paywall/ad-impression UI to hang the call off of.
