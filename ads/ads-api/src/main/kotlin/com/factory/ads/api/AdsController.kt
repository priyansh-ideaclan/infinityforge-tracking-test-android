package com.factory.ads.api

import android.app.Activity
import com.factory.core.common.AppResult

/**
 * The only ads entry point feature code uses for interstitial/rewarded/app-open
 * formats — never the Google Mobile Ads SDK directly (see AGENTS.md §6). Banner ads are
 * view-based and go through [BannerAdRenderer] instead. `show*` takes the current
 * [Activity] because fullscreen ad SDKs require one to present over — feature code
 * already has it (it's a Compose screen's `LocalActivity`/`LocalContext`).
 */
interface AdsController {
    suspend fun loadInterstitial(placement: AdPlacement): AppResult<Unit>
    suspend fun showInterstitial(activity: Activity, placement: AdPlacement): AppResult<Unit>

    suspend fun loadRewarded(placement: AdPlacement): AppResult<Unit>
    suspend fun showRewarded(activity: Activity, placement: AdPlacement): AppResult<RewardResult>

    suspend fun loadAppOpen(placement: AdPlacement): AppResult<Unit>
    suspend fun showAppOpen(activity: Activity, placement: AdPlacement): AppResult<Unit>
}
