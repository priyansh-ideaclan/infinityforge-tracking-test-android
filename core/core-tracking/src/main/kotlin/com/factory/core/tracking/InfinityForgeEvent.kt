package com.factory.core.tracking

/**
 * The contract version this adapter was written against —
 * infinityforge-tracking-module @ 1.2.0. Every canonical event below mirrors
 * events/ *.yaml field-for-field; every canonical event's `schemaVersion` mirrors its
 * definition's own `schema_version` (specification/versioning.md) — update it here
 * only when that definition's own version changes, never on a whim.
 */
object InfinityForgeContractVersion {
    const val VALUE = "1.2.0"
}

/** Shared enum values reused across multiple canonical events/dimensions — mirrors the
 * identical allowed-value lists in events/ *.yaml and metrics/ *.yaml. */
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
 *
 * The factory functions are split across sibling files by category — Application/
 * App-specific stay here; Authentication is in InfinityForgeAuthEvents.kt,
 * Onboarding in InfinityForgeOnboardingEvents.kt, Product in
 * InfinityForgeProductEvents.kt, Monetization in
 * InfinityForgeMonetizationEvents.kt — purely to keep this companion object under
 * Detekt's per-object function-count ceiling (complexity.TooManyFunctions). Every one
 * is a Kotlin extension function on [InfinityForgeEvent.Companion], so every call
 * site is unchanged: `InfinityForgeEvent.signupStarted(...)` etc. still work exactly
 * as if these were members.
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
