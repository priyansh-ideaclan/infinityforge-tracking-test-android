package com.factory.purchases.api

import android.app.Activity
import com.factory.core.common.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * The only purchases entry point feature code uses — it depends on `isPremium`, never
 * a RevenueCat `CustomerInfo`/`Purchases` type (see AGENTS.md §7 and ARCHITECTURE.md).
 * [purchase] takes an [Activity] because the underlying billing flow launches over one.
 */
interface PurchasesController {
    val isPremium: StateFlow<Boolean>

    suspend fun fetchPaywallState(): PaywallState
    suspend fun purchase(activity: Activity, packageId: String): AppResult<Unit>
    suspend fun restorePurchases(): AppResult<Unit>
}
