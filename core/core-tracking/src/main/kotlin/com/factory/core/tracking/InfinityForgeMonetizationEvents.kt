package com.factory.core.tracking

/**
 * Monetization-category canonical event factories for [InfinityForgeEvent.Companion]
 * — split into this file only to keep that companion object under Detekt's
 * per-object function-count ceiling (complexity.TooManyFunctions); see
 * [InfinityForgeEvent]'s own doc comment. Every function below is a Kotlin extension
 * function on the companion object, so it resolves exactly as
 * `InfinityForgeEvent.paywallViewed(...)` etc., identical to a member.
 */
fun InfinityForgeEvent.Companion.paywallViewed(placement: String? = null, source: String? = null): InfinityForgeEvent {
    val properties = buildMap {
        placement?.let { put("placement", InfinityForgePropertyValue.StringValue(it)) }
        source?.let { put("source", InfinityForgePropertyValue.StringValue(it)) }
    }
    return InfinityForgeEvent("paywall_viewed", 1, properties, isCanonical = true)
}

fun InfinityForgeEvent.Companion.trialStarted(plan: String? = null, trialLengthDays: Int? = null): InfinityForgeEvent {
    val properties = buildMap {
        plan?.let { put("plan", InfinityForgePropertyValue.StringValue(it)) }
        trialLengthDays?.let { put("trial_length_days", InfinityForgePropertyValue.IntValue(it)) }
    }
    return InfinityForgeEvent("trial_started", 1, properties, isCanonical = true)
}

/** `price` and `currency` must be supplied together — validated at `track()` time
 * (specification/conventions.md), not enforced by this factory's signature, matching
 * the reference implementations' own runtime (not compile-time) enforcement of the
 * pairing. */
fun InfinityForgeEvent.Companion.subscriptionStarted(
    plan: String,
    price: Double? = null,
    currency: String? = null,
    billingCycle: InfinityForgeBillingCycle? = null,
    transactionId: String? = null,
): InfinityForgeEvent {
    val properties = buildMap {
        put("plan", InfinityForgePropertyValue.StringValue(plan))
        price?.let { put("price", InfinityForgePropertyValue.NumberValue(it)) }
        currency?.let { put("currency", InfinityForgePropertyValue.StringValue(it)) }
        billingCycle?.let { put("billing_cycle", InfinityForgePropertyValue.StringValue(it.wireValue)) }
        transactionId?.let { put("transaction_id", InfinityForgePropertyValue.StringValue(it)) }
    }
    return InfinityForgeEvent("subscription_started", 1, properties, isCanonical = true)
}

fun InfinityForgeEvent.Companion.subscriptionCancelled(
    plan: String? = null,
    transactionId: String? = null,
): InfinityForgeEvent {
    val properties = buildMap {
        plan?.let { put("plan", InfinityForgePropertyValue.StringValue(it)) }
        transactionId?.let { put("transaction_id", InfinityForgePropertyValue.StringValue(it)) }
    }
    return InfinityForgeEvent("subscription_cancelled", 1, properties, isCanonical = true)
}

fun InfinityForgeEvent.Companion.purchaseCompleted(
    productId: String,
    price: Double? = null,
    currency: String? = null,
    quantity: Int? = null,
    transactionId: String? = null,
): InfinityForgeEvent {
    val properties = buildMap {
        put("product_id", InfinityForgePropertyValue.StringValue(productId))
        price?.let { put("price", InfinityForgePropertyValue.NumberValue(it)) }
        currency?.let { put("currency", InfinityForgePropertyValue.StringValue(it)) }
        quantity?.let { put("quantity", InfinityForgePropertyValue.IntValue(it)) }
        transactionId?.let { put("transaction_id", InfinityForgePropertyValue.StringValue(it)) }
    }
    return InfinityForgeEvent("purchase_completed", 1, properties, isCanonical = true)
}
