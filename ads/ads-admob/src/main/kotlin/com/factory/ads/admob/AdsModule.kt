package com.factory.ads.admob

import android.content.Context
import com.factory.ads.api.AdPlacement
import com.factory.ads.api.AdsController
import com.factory.ads.api.BannerAdRenderer
import com.factory.ads.api.FakeAdsController
import com.factory.ads.api.FakeBannerAdRenderer
import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * External configuration for production ad unit IDs, keyed by placement — sourced from
 * `AppModule`'s `BuildConfig`-backed provider, never hardcoded here. Empty by default;
 * see `AdUnitIdResolver` for what happens when a placement has no configured ID.
 */
data class ProductionAdUnitIds(val ids: Map<AdPlacement, String> = emptyMap())

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {

    @Provides
    @Singleton
    fun provideAdUnitIdResolver(
        isDebugBuild: IsDebugBuild,
        productionAdUnitIds: ProductionAdUnitIds,
    ): AdUnitIdResolver = AdUnitIdResolver(isDebugBuild.value, productionAdUnitIds.ids)

    @Provides
    @Singleton
    fun provideAdsController(
        featureFlagProvider: FeatureFlagProvider,
        @ApplicationContext context: Context,
        adUnitIdResolver: AdUnitIdResolver,
        logger: Logger,
    ): AdsController =
        if (featureFlagProvider.isEnabled(FeatureFlag.ADS)) {
            AdMobAdsController(context, adUnitIdResolver, logger)
        } else {
            FakeAdsController()
        }

    @Provides
    @Singleton
    fun provideBannerAdRenderer(
        featureFlagProvider: FeatureFlagProvider,
        adUnitIdResolver: AdUnitIdResolver,
    ): BannerAdRenderer =
        if (featureFlagProvider.isEnabled(FeatureFlag.ADS)) {
            AdMobBannerRenderer(adUnitIdResolver)
        } else {
            FakeBannerAdRenderer()
        }
}

/** Wrapper so Hilt can inject a plain boolean without ambiguity with other booleans. */
data class IsDebugBuild(val value: Boolean)
