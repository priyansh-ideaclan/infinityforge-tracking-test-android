package com.factory.ads.api

/**
 * Named ad placements — matches `APP_SPEC.yaml`'s `ads.placements` keys. Feature code
 * requests ads by placement, never by a raw AdMob ad unit ID (those are resolved
 * per-environment by `ads-admob`'s `AdUnitIdResolver`; see AGENTS.md §6/Docs/setup/admob.md).
 */
enum class AdPlacement {
    BANNER_HOME,
    INTERSTITIAL_TRANSITION,
    REWARDED_BONUS,
    APP_OPEN_LAUNCH,
}

enum class AdFormat {
    BANNER,
    INTERSTITIAL,
    REWARDED,
    APP_OPEN,
}

sealed interface RewardResult {
    data class Earned(val amount: Int, val type: String) : RewardResult
    data object NotEarned : RewardResult
}
