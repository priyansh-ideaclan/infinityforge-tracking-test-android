package com.factory.purchases.revenuecat

import android.app.Activity
import com.factory.core.common.AppError
import com.factory.core.common.AppResult
import com.factory.core.logging.Logger
import com.factory.purchases.api.PaywallState
import com.factory.purchases.api.PurchasePackage
import com.factory.purchases.api.PurchasesController
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "RevenueCatPurchases"

/**
 * Real RevenueCat implementation. [Purchases.configure] must already have been called
 * (in `FactoryApplication`, with the *public* SDK key from external configuration — see
 * `Docs/setup/revenuecat.md`) before this class is used; that call is intentionally not
 * repeated here so there is exactly one place the SDK key is read from.
 */
class RevenueCatPurchasesController(
    private val premiumEntitlementId: String,
    private val logger: Logger,
) : PurchasesController {

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium

    init {
        Purchases.sharedInstance.updatedCustomerInfoListener = { customerInfo ->
            _isPremium.value = customerInfo.entitlements[premiumEntitlementId]?.isActive == true
        }
    }

    override suspend fun fetchPaywallState(): PaywallState = try {
        val offerings = Purchases.sharedInstance.awaitOfferings()
        val current = offerings.current
        if (current == null) {
            PaywallState.Error("No current offering is configured in RevenueCat.")
        } else {
            PaywallState.Loaded(
                packages = current.availablePackages.map { pkg ->
                    PurchasePackage(
                        id = pkg.identifier,
                        productId = pkg.product.id,
                        priceFormatted = pkg.product.price.formatted,
                    )
                },
            )
        }
    } catch (e: PurchasesException) {
        logger.warn(TAG, "Failed to fetch offerings", e)
        PaywallState.Error(e.message)
    }

    override suspend fun purchase(activity: Activity, packageId: String): AppResult<Unit> = try {
        val offerings = Purchases.sharedInstance.awaitOfferings()
        val pkg = offerings.current?.availablePackages?.find { it.identifier == packageId }
            ?: return AppResult.Failure(AppError.Purchases(message = "Package not found: $packageId"))

        val result = Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, pkg).build())
        _isPremium.value = result.customerInfo.entitlements[premiumEntitlementId]?.isActive == true
        AppResult.Success(Unit)
    } catch (e: PurchasesException) {
        logger.warn(TAG, "Purchase failed", e)
        AppResult.Failure(AppError.Purchases(message = e.message, userCancelled = e.userCancelled))
    }

    override suspend fun restorePurchases(): AppResult<Unit> = try {
        val customerInfo = Purchases.sharedInstance.awaitRestore()
        _isPremium.value = customerInfo.entitlements[premiumEntitlementId]?.isActive == true
        AppResult.Success(Unit)
    } catch (e: PurchasesException) {
        logger.warn(TAG, "Restore failed", e)
        AppResult.Failure(AppError.Purchases(message = e.message))
    }
}

private val PurchasesException.userCancelled: Boolean
    get() = code.name == "PurchaseCancelledError"
