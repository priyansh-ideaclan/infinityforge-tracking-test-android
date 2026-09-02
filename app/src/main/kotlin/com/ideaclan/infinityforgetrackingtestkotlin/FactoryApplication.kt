package com.ideaclan.infinityforgetrackingtestkotlin

import android.app.Application
import com.factory.core.common.DispatcherProvider
import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.logging.Logger
import com.factory.core.tracking.InfinityForgeTrackingClient
import com.google.android.gms.ads.MobileAds
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FactoryApplication"

@HiltAndroidApp
class FactoryApplication : Application() {

    @Inject lateinit var featureFlagProvider: FeatureFlagProvider

    @Inject lateinit var logger: Logger

    @Inject lateinit var infinityForgeTracking: InfinityForgeTrackingClient

    @Inject lateinit var dispatcherProvider: DispatcherProvider

    override fun onCreate() {
        super.onCreate()
        initializeAdsIfEnabled()
        initializePurchasesIfConfigured()
        initializeInfinityForgeTracking()
    }

    /**
     * `InfinityForgeTrackingClient.initialize()` is `suspend` (it hydrates persisted
     * identity — see `InfinityForgeIdentity`) and, per specification/api.md, must
     * never block a user-facing action — so it is launched fire-and-forget on its own
     * short-lived scope here, exactly like the reference Swift implementation's
     * `AppLifecycleCoordinator` awaiting it inside an already-async lifecycle method,
     * and the reference RN implementation's own app-root effect. This factory has no
     * existing app-wide `CoroutineScope` to reuse (see `DispatcherProvider`'s own doc
     * comment on why a raw `Dispatchers.*` is never referenced directly), so a local
     * one is created here rather than inventing a new shared DI primitive for it.
     */
    private fun initializeInfinityForgeTracking() {
        CoroutineScope(SupervisorJob() + dispatcherProvider.default).launch {
            infinityForgeTracking.initialize()
        }
    }

    private fun initializeAdsIfEnabled() {
        if (!featureFlagProvider.isEnabled(FeatureFlag.ADS)) return
        MobileAds.initialize(this)
    }

    /**
     * `Purchases.configure` is called exactly once, here, and only when both
     * `APP_SPEC.yaml`'s `purchases.enabled` is true **and** a real RevenueCat public SDK
     * key was supplied externally (see `Docs/setup/revenuecat.md`). Without a real key,
     * `purchases-revenuecat`'s `PurchasesModule` falls back to `FakePurchasesController`
     * regardless, so skipping configuration here is safe rather than a silent gap.
     */
    private fun initializePurchasesIfConfigured() {
        if (!featureFlagProvider.isEnabled(FeatureFlag.PURCHASES)) return
        if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
            logger.warn(
                TAG,
                "Purchases enabled in APP_SPEC.yaml but no RevenueCat API key is " +
                    "configured; purchases will use FakePurchasesController. See " +
                    "Docs/setup/revenuecat.md.",
            )
            return
        }
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
        Purchases.configure(
            PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build(),
        )
    }
}
