package com.factory.ads.api

import android.app.Activity
import com.factory.core.common.AppResult
import javax.inject.Inject

/**
 * Always "succeeds" instantly with no network call — selected when `APP_SPEC.yaml`'s
 * `ads.enabled` is false, and used directly in unit tests.
 */
class FakeAdsController @Inject constructor() : AdsController {
    override suspend fun loadInterstitial(placement: AdPlacement): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun showInterstitial(activity: Activity, placement: AdPlacement): AppResult<Unit> =
        AppResult.Success(Unit)
    override suspend fun loadRewarded(placement: AdPlacement): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun showRewarded(activity: Activity, placement: AdPlacement): AppResult<RewardResult> =
        AppResult.Success(RewardResult.Earned(amount = 1, type = "fake_reward"))
    override suspend fun loadAppOpen(placement: AdPlacement): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun showAppOpen(activity: Activity, placement: AdPlacement): AppResult<Unit> =
        AppResult.Success(Unit)
}
