package com.factory.ads.admob

import android.app.Activity
import android.content.Context
import com.factory.ads.api.AdPlacement
import com.factory.ads.api.AdsController
import com.factory.ads.api.RewardResult
import com.factory.core.common.AppError
import com.factory.core.common.AppResult
import com.factory.core.logging.Logger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

private const val TAG = "AdMobAdsController"

/**
 * Real Google Mobile Ads implementation. Every load/show call goes through
 * [AdUnitIdResolver] so debug builds physically cannot request a production ad unit —
 * see AGENTS.md §6.
 */
class AdMobAdsController @Inject constructor(
    private val context: Context,
    private val adUnitIdResolver: AdUnitIdResolver,
    private val logger: Logger,
) : AdsController {

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null

    override suspend fun loadInterstitial(placement: AdPlacement): AppResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            InterstitialAd.load(
                context,
                adUnitIdResolver.resolve(placement),
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        continuation.resume(AppResult.Success(Unit))
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        logger.warn(TAG, "Interstitial failed to load: ${error.message}")
                        continuation.resume(AppResult.Failure(error.toAppError()))
                    }
                },
            )
        }

    override suspend fun showInterstitial(activity: Activity, placement: AdPlacement): AppResult<Unit> {
        val ad = interstitialAd ?: return AppResult.Failure(adNotReadyError())
        return suspendCancellableCoroutine { continuation ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    continuation.resume(AppResult.Success(Unit))
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    continuation.resume(AppResult.Failure(error.toAppError()))
                }
            }
            ad.show(activity)
        }
    }

    override suspend fun loadRewarded(placement: AdPlacement): AppResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            RewardedAd.load(
                context,
                adUnitIdResolver.resolve(placement),
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        continuation.resume(AppResult.Success(Unit))
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        logger.warn(TAG, "Rewarded failed to load: ${error.message}")
                        continuation.resume(AppResult.Failure(error.toAppError()))
                    }
                },
            )
        }

    override suspend fun showRewarded(activity: Activity, placement: AdPlacement): AppResult<RewardResult> {
        val ad = rewardedAd ?: return AppResult.Failure(adNotReadyError())
        return suspendCancellableCoroutine { continuation ->
            var reward: RewardResult = RewardResult.NotEarned
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    continuation.resume(AppResult.Success(reward))
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    continuation.resume(AppResult.Failure(error.toAppError()))
                }
            }
            ad.show(activity) { rewardItem ->
                reward = RewardResult.Earned(amount = rewardItem.amount, type = rewardItem.type)
            }
        }
    }

    override suspend fun loadAppOpen(placement: AdPlacement): AppResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            AppOpenAd.load(
                context,
                adUnitIdResolver.resolve(placement),
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        continuation.resume(AppResult.Success(Unit))
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        logger.warn(TAG, "App open failed to load: ${error.message}")
                        continuation.resume(AppResult.Failure(error.toAppError()))
                    }
                },
            )
        }

    override suspend fun showAppOpen(activity: Activity, placement: AdPlacement): AppResult<Unit> {
        val ad = appOpenAd ?: return AppResult.Failure(adNotReadyError())
        return suspendCancellableCoroutine { continuation ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    continuation.resume(AppResult.Success(Unit))
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    appOpenAd = null
                    continuation.resume(AppResult.Failure(error.toAppError()))
                }
            }
            ad.show(activity)
        }
    }

    private fun adNotReadyError() = AppError.Unknown(message = "Ad was not loaded before show() was called.")

    private fun AdError.toAppError() = AppError.Unknown(message = message)
}
