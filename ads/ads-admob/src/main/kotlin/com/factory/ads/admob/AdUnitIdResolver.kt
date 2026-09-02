package com.factory.ads.admob

import com.factory.ads.api.AdPlacement

/**
 * Google's own published *test* ad unit IDs (safe to hardcode — they are not secrets,
 * see https://developers.google.com/admob/android/test-ads). Debug/CI builds always use
 * these regardless of `APP_SPEC.yaml`, per AGENTS.md §6.
 */
private val TEST_AD_UNIT_IDS = mapOf(
    AdPlacement.BANNER_HOME to "ca-app-pub-3940256099942544/6300978111",
    AdPlacement.INTERSTITIAL_TRANSITION to "ca-app-pub-3940256099942544/1033173712",
    AdPlacement.REWARDED_BONUS to "ca-app-pub-3940256099942544/5224354917",
    AdPlacement.APP_OPEN_LAUNCH to "ca-app-pub-3940256099942544/9257395921",
)

/** Placeholder that must never survive into a release build; see `scripts/release_check.py`. */
const val UNCONFIGURED_AD_UNIT_ID = "UNCONFIGURED_AD_UNIT_ID"

/**
 * Resolves a placement to a real AdMob ad unit ID. In debug builds this always returns
 * Google's test IDs, ignoring [productionAdUnitIds], so a developer can never
 * accidentally ship real impressions from a debug build. In release builds it returns
 * the configured production ID, or [UNCONFIGURED_AD_UNIT_ID] if none was supplied —
 * `scripts/release_check.py` fails a release that still contains that placeholder.
 */
class AdUnitIdResolver(
    private val isDebug: Boolean,
    private val productionAdUnitIds: Map<AdPlacement, String> = emptyMap(),
) {
    fun resolve(placement: AdPlacement): String =
        if (isDebug) {
            TEST_AD_UNIT_IDS.getValue(placement)
        } else {
            productionAdUnitIds[placement] ?: UNCONFIGURED_AD_UNIT_ID
        }
}
