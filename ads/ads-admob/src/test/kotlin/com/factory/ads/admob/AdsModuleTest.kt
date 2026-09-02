package com.factory.ads.admob

import android.content.Context
import com.factory.ads.api.FakeAdsController
import com.factory.ads.api.FakeBannerAdRenderer
import com.factory.core.common.FeatureFlag
import com.factory.core.logging.AndroidLogger
import com.factory.core.testing.FakeFeatureFlagProvider
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One of the factory's "module combination" checks (see
 * Docs/testing/module-matrix.md): verifies `AdsModule`'s `@Provides` functions select
 * the correct implementation based on `APP_SPEC.yaml`'s `ads.enabled` flag, in both
 * directions, without needing a real Hilt graph or a device.
 */
class AdsModuleTest {

    private val resolver = AdUnitIdResolver(isDebug = true)
    private val logger = AndroidLogger(isDebug = true)
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `ads disabled selects the fake controller and banner renderer`() {
        val flags = FakeFeatureFlagProvider(enabledFlags = emptySet())

        val controller = AdsModule.provideAdsController(flags, context, resolver, logger)
        val banner = AdsModule.provideBannerAdRenderer(flags, resolver)

        assertTrue(controller is FakeAdsController)
        assertTrue(banner is FakeBannerAdRenderer)
    }

    @Test
    fun `ads enabled selects the real AdMob controller and banner renderer`() {
        val flags = FakeFeatureFlagProvider(enabledFlags = setOf(FeatureFlag.ADS))

        val controller = AdsModule.provideAdsController(flags, context, resolver, logger)
        val banner = AdsModule.provideBannerAdRenderer(flags, resolver)

        assertTrue(controller is AdMobAdsController)
        assertTrue(banner is AdMobBannerRenderer)
    }
}
