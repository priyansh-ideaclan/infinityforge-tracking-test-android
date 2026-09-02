package com.factory.purchases.api

import android.app.Activity
import com.factory.core.common.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * A real, always-available implementation with no RevenueCat dependency — selected
 * when `APP_SPEC.yaml`'s `purchases.enabled` is false, and used directly in unit tests.
 * "Purchasing" a package immediately grants premium in-memory, with no store call.
 */
class FakePurchasesController @Inject constructor() : PurchasesController {

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium

    override suspend fun fetchPaywallState(): PaywallState = PaywallState.Loaded(
        packages = listOf(
            PurchasePackage(id = "fake_monthly", productId = "premium_monthly", priceFormatted = "$4.99"),
            PurchasePackage(id = "fake_annual", productId = "premium_annual", priceFormatted = "$39.99"),
        ),
    )

    override suspend fun purchase(activity: Activity, packageId: String): AppResult<Unit> {
        _isPremium.value = true
        return AppResult.Success(Unit)
    }

    override suspend fun restorePurchases(): AppResult<Unit> = AppResult.Success(Unit)
}
