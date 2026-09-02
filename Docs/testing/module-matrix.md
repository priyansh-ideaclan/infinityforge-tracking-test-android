# Module-combination testing

`MODULES.yaml`'s `verified_combinations` names four configurations of the optional
capabilities (`auth`, `ads`, `purchases`):

| Name | auth | ads | purchases |
|---|---|---|---|
| `minimal` | off | off | off |
| `auth_and_ads` | on | on | off |
| `auth_and_purchases` | on | off | on |
| `full_v1` | on | on | on |

## What "verified" means for V1

Building and testing all four combinations as fully separate, independently-configured
apps (i.e. running `scripts/configure_app.py` four times into four checkouts) is out of
scope for this pass — recorded here honestly rather than silently skipped, per
`AGENTS.md`.

What **is** verified, automatically, on every `./scripts/verify.sh` run:

- Each optional capability's Hilt `@Provides` selection function is unit-tested in both
  directions (enabled/disabled) directly — without a running Hilt graph or a device:
  - `ads-admob`'s `AdsModuleTest` — `AdsModule.provideAdsController`/
    `provideBannerAdRenderer` select `FakeAdsController`/`FakeBannerAdRenderer` when
    `ads.enabled: false`, and `AdMobAdsController`/`AdMobBannerRenderer` when `true`.
  - `purchases-revenuecat`'s `PurchasesModuleTest` — selects `FakePurchasesController`
    when `purchases.enabled: false`. The `true` branch is **not** exercised (see below).
  - `feature-auth`'s `AuthModuleTest` — selects `FakeAuthRepository` whenever Firebase
    isn't actually configured (a Robolectric test environment never has one), which is
    the safety property that matters most: auth can never crash for lack of
    `google-services.json`, regardless of what `auth.enabled` says.
- Every ViewModel that depends on one of these boundaries (`LoginViewModel`,
  `HomeViewModel`, `SettingsViewModel`) is tested against the *fake* implementations,
  which is exactly what runs in every combination above once `auth`/`ads`/`purchases`
  is off, and exercises the same ViewModel code path used when it's on (the ViewModel
  never knows which implementation it got).

## What is not verified this pass

- `purchases.enabled: true` selecting a real `RevenueCatPurchasesController` is not unit
  tested: its constructor reads `Purchases.sharedInstance`, a static singleton only set
  by a real `Purchases.configure(...)` call. Mocking that singleton (e.g. via MockK's
  `mockkObject`) is possible but wasn't done in this pass.
- `ads.enabled: true` and `auth.enabled: true` with a **real, configured** Firebase
  project / AdMob production ad unit ID are not tested here for the obvious reason: this
  factory never invents those credentials (`AGENTS.md` §7). `Docs/setup/firebase.md` and
  `Docs/setup/admob.md` describe the manual verification steps once real credentials
  exist.
- A full four-way `assembleDebug` matrix (actually running `configure_app.py` with each
  combination and building the result) is the most realistic remaining gap. It's
  mechanically straightforward to add as a CI matrix job once `configure_app.py` exists
  (see the plan's Phase 11/16) — tracked as remaining work here rather than attempted
  partially.
