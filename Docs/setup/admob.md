# AdMob setup

The `ads-admob` module never uses a production ad unit ID in a debug build — see
`AdUnitIdResolver` in `ads/ads-admob/src/main/kotlin/com/factory/ads/admob/`. This
document is about wiring up **production** ad units for a release build.

## 1. Create an AdMob app and ad units

1. In the [AdMob console](https://apps.admob.com), add your app (or link the Play
   Console listing once published).
2. For each placement in `APP_SPEC.yaml`'s `ads.placements`
   (`banner_home`, `interstitial_transition`, `rewarded_bonus`, `app_open_launch`),
   create a matching ad unit of the corresponding format.
3. Update the AdMob Application ID in `app/src/main/AndroidManifest.xml`'s
   `com.google.android.gms.ads.APPLICATION_ID` meta-data — it currently holds Google's
   own published *test* App ID (`ca-app-pub-3940256099942544~3347511713`), which is safe
   to ship in debug but must be replaced with your real App ID before a production
   release.

## 2. Wire production ad unit IDs

`AdUnitIdResolver` (see `ads/ads-admob/.../AdUnitIdResolver.kt`) always returns Google's
test ad unit IDs in debug builds, regardless of configuration — this is intentional and
cannot be bypassed, so a developer can never accidentally ship test traffic as real
impressions or vice versa.

For a release build, provide real IDs via `ProductionAdUnitIds` in
`app/src/main/kotlin/com/factory/app/di/AppModule.kt`'s
`provideProductionAdUnitIds()` — replace the empty default with a real map, sourced from
a Gradle property (do not hardcode real ad unit IDs into a committed file; ad unit IDs
are not secret in the same sense as an API key, but keeping them externally configured
means the same source doesn't need editing per-app):

```kotlin
@Provides
@Singleton
fun provideProductionAdUnitIds(): ProductionAdUnitIds = ProductionAdUnitIds(
    mapOf(
        AdPlacement.BANNER_HOME to (System.getenv("ADMOB_BANNER_HOME") ?: ""),
        // ...
    ),
)
```

## 3. Release check

`scripts/release_check.py` fails a release if `ads.enabled: true` in `APP_SPEC.yaml`
but `ProductionAdUnitIds()` is still the empty default — this is exactly the "silently
reaching production" case `AGENTS.md` §6 requires to be caught, not shipped.

## 4. Verification

- **Without real ad units**: debug builds show Google's test ads — this is fully
  verifiable locally/in CI (`ads.enabled: true` + a debug build renders a real test ad).
- **With real ad units**: verify in a release (or a debug build temporarily pointed at
  production IDs) that impressions appear in the AdMob console within a few hours;
  this factory cannot verify live ad-serving itself.
