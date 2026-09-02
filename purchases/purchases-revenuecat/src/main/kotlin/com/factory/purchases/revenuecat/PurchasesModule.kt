package com.factory.purchases.revenuecat

import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.logging.Logger
import com.factory.purchases.api.FakePurchasesController
import com.factory.purchases.api.PurchasesController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `premiumEntitlementId` is sourced from `APP_SPEC.yaml`'s
 * `purchases.premium_entitlement_id` via `AppModule` — never hardcoded here.
 */
data class PremiumEntitlementId(val value: String)

@Module
@InstallIn(SingletonComponent::class)
object PurchasesModule {

    @Provides
    @Singleton
    fun providePurchasesController(
        featureFlagProvider: FeatureFlagProvider,
        premiumEntitlementId: PremiumEntitlementId,
        logger: Logger,
    ): PurchasesController =
        if (featureFlagProvider.isEnabled(FeatureFlag.PURCHASES)) {
            RevenueCatPurchasesController(premiumEntitlementId.value, logger)
        } else {
            FakePurchasesController()
        }
}
