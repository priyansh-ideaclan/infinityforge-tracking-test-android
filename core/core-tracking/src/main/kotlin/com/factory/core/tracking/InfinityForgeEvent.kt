package com.factory.core.tracking

/**
 * The contract version this adapter was written against —
 * infinityforge-tracking-module @ 1.2.0. Every canonical event below mirrors
 * events/*.yaml field-for-field; every canonical event's `schemaVersion` mirrors its
 * definition's own `schema_version` (specification/versioning.md) — update it here
 * only when that definition's own version changes, never on a whim.
 */
object InfinityForgeContractVersion {
    const val VALUE = "1.2.0"
}

/** Shared enum values reused across multiple canonical events/dimensions — mirrors the
 * identical allowed-value lists in events/*.yaml and metrics/*.yaml. */
enum class InfinityForgeLaunchType(val wireValue: String) { COLD("cold"), WARM("warm") }

enum class InfinityForgeAuthMethod(val wireValue: String) {
    EMAIL("email"), PHONE("phone"), SOCIAL("social"), OTHER("other")
}

enum class InfinityForgeBillingCycle(val wireValue: String) {
    MONTHLY("monthly"), YEARLY("yearly"), WEEKLY("weekly"), OTHER("other")
}

/**
 * A validated event, ready to become an [InfinityForgeEventEnvelope]. Canonical events
 * are only constructible through this type's factory functions below, which give
 * required contract properties as compiler-enforced arguments — mirroring
 * `core-analytics`'s own `AnalyticsEvent` factory idiom, applied here to a different,
 * contract-conformant taxonomy (see Docs/INFINITYFORGE_TRACKING.md for why these two
 * event systems are not merged). [custom] is the only path for an app-specific event
 * name; name-pattern and reserved-field validation happen once, in
 * `InfinityForgeTrackingClient.track()` — not at construction — so a rejected call
 * fails the same way whether it came from a canonical factory or [custom].
 */
data class InfinityForgeEvent internal constructor(
    val name: String,
    val schemaVersion: Int,
    val properties: Map<String, InfinityForgePropertyValue>,
    val isCanonical: Boolean,
) {
    companion object {
        // -- Application --

        fun appOpened(launchType: InfinityForgeLaunchType? = null): InfinityForgeEvent {
            val properties = buildMap {
                launchType?.let { put("launch_type", InfinityForgePropertyValue.StringValue(it.wireValue)) }
            }
            return InfinityForgeEvent("app_opened", 1, properties, isCanonical = true)
        }

        /** Constructed internally by `InfinityForgeTrackingClient.screen()` —
         * screen_viewed has no standalone public factory, matching the reference
         * Swift/RN implementations (which build it only from their own `screen()`
         * method, never exposing it to `track()` directly). */
        internal fun screenViewed(
            screenName: String,
            previousScreen: String?,
            extraProperties: Map<String, InfinityForgePropertyValue>,
        ): InfinityForgeEvent {
            val properties = buildMap {
                putAll(extraProperties)
                put("screen_name", InfinityForgePropertyValue.StringValue(screenName))
                previousScreen?.let { put("previous_screen", InfinityForgePropertyValue.StringValue(it)) }
            }
            return InfinityForgeEvent("screen_viewed", 1, properties, isCanonical = true)
        }

        // -- Authentication --

        fun signupStarted(method: InfinityForgeAuthMethod? = null) =
            InfinityForgeEvent("signup_started", 1, methodProperty(method), isCanonical = true)

        fun signupCompleted(method: InfinityForgeAuthMethod? = null) =
            InfinityForgeEvent("signup_completed", 1, methodProperty(method), isCanonical = true)

        fun loginStarted(method: InfinityForgeAuthMethod? = null) =
            InfinityForgeEvent("login_started", 1, methodProperty(method), isCanonical = true)

        fun loginCompleted(method: InfinityForgeAuthMethod? = null) =
            InfinityForgeEvent("login_completed", 1, methodProperty(method), isCanonical = true)

        private fun methodProperty(method: InfinityForgeAuthMethod?): Map<String, InfinityForgePropertyValue> =
            method?.let { mapOf("method" to InfinityForgePropertyValue.StringValue(it.wireValue)) } ?: emptyMap()

        // -- Onboarding --

        fun onboardingStarted() = InfinityForgeEvent("onboarding_started", 1, emptyMap(), isCanonical = true)

        fun onboardingCompleted(stepCount: Int? = null): InfinityForgeEvent {
            val properties = buildMap {
                stepCount?.let { put("step_count", InfinityForgePropertyValue.IntValue(it)) }
            }
            return InfinityForgeEvent("onboarding_completed", 1, properties, isCanonical = true)
        }

        // -- Product --

        fun featureUsed(featureName: String, featureCategory: String? = null): InfinityForgeEvent {
            val properties = buildMap {
                put("feature_name", InfinityForgePropertyValue.StringValue(featureName))
                featureCategory?.let { put("feature_category", InfinityForgePropertyValue.StringValue(it)) }
            }
            return InfinityForgeEvent("feature_used", 1, properties, isCanonical = true)
        }

        // -- Monetization --

        fun paywallViewed(placement: String? = null, source: String? = null): InfinityForgeEvent {
            val properties = buildMap {
                placement?.let { put("placement", InfinityForgePropertyValue.StringValue(it)) }
                source?.let { put("source", InfinityForgePropertyValue.StringValue(it)) }
            }
            return InfinityForgeEvent("paywall_viewed", 1, properties, isCanonical = true)
        }

        fun trialStarted(plan: String? = null, trialLengthDays: Int? = null): InfinityForgeEvent {
            val properties = buildMap {
                plan?.let { put("plan", InfinityForgePropertyValue.StringValue(it)) }
                trialLengthDays?.let { put("trial_length_days", InfinityForgePropertyValue.IntValue(it)) }
            }
            return InfinityForgeEvent("trial_started", 1, properties, isCanonical = true)
        }

        /** `price` and `currency` must be supplied together — validated at `track()`
         * time (specification/conventions.md), not enforced by this factory's
         * signature, matching the reference implementations' own runtime (not
         * compile-time) enforcement of the pairing. */
        fun subscriptionStarted(
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

        fun subscriptionCancelled(plan: String? = null, transactionId: String? = null): InfinityForgeEvent {
            val properties = buildMap {
                plan?.let { put("plan", InfinityForgePropertyValue.StringValue(it)) }
                transactionId?.let { put("transaction_id", InfinityForgePropertyValue.StringValue(it)) }
            }
            return InfinityForgeEvent("subscription_cancelled", 1, properties, isCanonical = true)
        }

        fun purchaseCompleted(
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

        // -- App-specific --

        /** An app-specific (non-canonical) event — specification/events.md's
         * "App-specific events" allowance. `schemaVersion` defaults to whatever
         * [InfinityForgeAppSpecificSchemaVersions] has on file for `name` (1 if never
         * registered) — the application owns this version, never the contract or this
         * adapter (specification/versioning.md). */
        fun custom(name: String, properties: Map<String, InfinityForgePropertyValue> = emptyMap()): InfinityForgeEvent =
            InfinityForgeEvent(
                name,
                InfinityForgeAppSpecificSchemaVersions.event(name),
                properties,
                isCanonical = false,
            )
    }
}
