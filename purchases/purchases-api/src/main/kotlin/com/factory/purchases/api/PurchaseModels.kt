package com.factory.purchases.api

/** One purchasable package as offered by the store — never a raw RevenueCat `Package`. */
data class PurchasePackage(
    val id: String,
    val productId: String,
    val priceFormatted: String,
)

sealed interface PaywallState {
    data object Loading : PaywallState
    data class Loaded(val packages: List<PurchasePackage>) : PaywallState
    data class Error(val message: String) : PaywallState
}
