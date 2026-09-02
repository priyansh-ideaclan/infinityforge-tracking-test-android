# RevenueCat setup

Without a real RevenueCat API key, `PurchasesModule` (in `purchases-revenuecat`) binds
`FakePurchasesController` regardless of `APP_SPEC.yaml`'s `purchases.enabled` — see
`FactoryApplication.initializePurchasesIfConfigured()`, which skips
`Purchases.configure(...)` (and logs why) when `BuildConfig.REVENUECAT_API_KEY` is
blank. This document is about wiring up the real integration.

## 1. Create a RevenueCat project

1. In the [RevenueCat dashboard](https://app.revenuecat.com), create a project and add
   your Android app (needs the Play Console package name — matches
   `APP_SPEC.yaml`'s `app.package_name`).
2. Connect Google Play Billing (requires a Play Console service account with the
   appropriate permissions — see RevenueCat's own Play Store integration guide).

## 2. Configure products and entitlements

1. Create your subscription/one-time products in Play Console first.
2. In RevenueCat, create matching Products, then an Entitlement whose identifier
   **exactly matches** `APP_SPEC.yaml`'s `purchases.premium_entitlement_id` (default:
   `"premium"`) — `RevenueCatPurchasesController` checks
   `customerInfo.entitlements[premiumEntitlementId]?.isActive`, so a mismatch here means
   `isPremium` silently never becomes `true`.
3. Create an Offering with Packages pointing at your Products — `fetchPaywallState()`
   reads `Purchases.sharedInstance.awaitOfferings().current`.

## 3. Set the API key

Get the **public** SDK key (Project settings → API keys → Public app-specific key) and
set it via a Gradle property (never commit it):

```properties
# ~/.gradle/gradle.properties (machine-local) or -PFACTORY_REVENUECAT_API_KEY=...
FACTORY_REVENUECAT_API_KEY=goog_XXXXXXXXXXXXXXXXXXXXXXXXXXX
```

## 4. Release check

`scripts/release_check.py` fails a release if `purchases.enabled: true` but
`app/build.gradle.kts` no longer reads `FACTORY_REVENUECAT_API_KEY` (i.e. someone
removed the wiring) — it cannot check the property's *value* at build time without
requiring it be set for every CI run, which would fail the "no production credentials
required for ordinary CI" requirement (`AGENTS.md`/CI design). A blank key at runtime is
instead handled safely by `FactoryApplication` as described above.

## 5. Verification

- **Without a real key**: `isPremium`/`fetchPaywallState()`/`purchase()`/
  `restorePurchases()` all work against `FakePurchasesController` — fully unit-testable,
  see `purchases-api`'s and `purchases-revenuecat`'s tests.
- **With a real key**: use Play Console's license testing (or a real sandbox purchase)
  to confirm a purchase flips `isPremium` to `true` and that Restore Purchases works
  after reinstalling. This factory cannot verify a live purchase itself.
