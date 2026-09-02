package com.factory.core.tracking

import java.util.concurrent.ConcurrentHashMap

/**
 * Canonical event names and their contract-defined property shape, used by
 * [InfinityForgeEventValidation] — the single source of truth the factories in
 * [InfinityForgeEvent] and the validator both agree with. Kept separate from the
 * factories so validation can check a *string* event name (from `custom()`, or a
 * misspelled canonical name) against the same registry.
 */
object InfinityForgeEventCatalog {
    sealed interface Kind {
        data object StringKind : Kind
        data object IntKind : Kind
        data object NumberKind : Kind
        data object BooleanKind : Kind
        data class EnumKind(val allowedValues: Set<String>) : Kind
    }

    data class PropertySpec(val kind: Kind, val required: Boolean)

    val schemaVersions: Map<String, Int> = mapOf(
        "app_opened" to 1,
        "screen_viewed" to 1,
        "signup_started" to 1,
        "signup_completed" to 1,
        "login_started" to 1,
        "login_completed" to 1,
        "onboarding_started" to 1,
        "onboarding_completed" to 1,
        "feature_used" to 1,
        "paywall_viewed" to 1,
        "trial_started" to 1,
        "subscription_started" to 1,
        "subscription_cancelled" to 1,
        "purchase_completed" to 1,
    )

    private val methodSpec = PropertySpec(Kind.EnumKind(setOf("email", "phone", "social", "other")), required = false)

    val properties: Map<String, Map<String, PropertySpec>> = mapOf(
        "app_opened" to mapOf(
            "launch_type" to PropertySpec(Kind.EnumKind(setOf("cold", "warm")), required = false),
        ),
        "screen_viewed" to mapOf(
            "screen_name" to PropertySpec(Kind.StringKind, required = true),
            "previous_screen" to PropertySpec(Kind.StringKind, required = false),
        ),
        "signup_started" to mapOf("method" to methodSpec),
        "signup_completed" to mapOf("method" to methodSpec),
        "login_started" to mapOf("method" to methodSpec),
        "login_completed" to mapOf("method" to methodSpec),
        "onboarding_started" to emptyMap(),
        "onboarding_completed" to mapOf(
            "step_count" to PropertySpec(Kind.IntKind, required = false),
        ),
        "feature_used" to mapOf(
            "feature_name" to PropertySpec(Kind.StringKind, required = true),
            "feature_category" to PropertySpec(Kind.StringKind, required = false),
        ),
        "paywall_viewed" to mapOf(
            "placement" to PropertySpec(Kind.StringKind, required = false),
            "source" to PropertySpec(Kind.StringKind, required = false),
        ),
        "trial_started" to mapOf(
            "plan" to PropertySpec(Kind.StringKind, required = false),
            "trial_length_days" to PropertySpec(Kind.IntKind, required = false),
        ),
        "subscription_started" to mapOf(
            "plan" to PropertySpec(Kind.StringKind, required = true),
            "price" to PropertySpec(Kind.NumberKind, required = false),
            "currency" to PropertySpec(Kind.StringKind, required = false),
            "billing_cycle" to PropertySpec(
                Kind.EnumKind(setOf("monthly", "yearly", "weekly", "other")),
                required = false,
            ),
            "transaction_id" to PropertySpec(Kind.StringKind, required = false),
        ),
        "subscription_cancelled" to mapOf(
            "plan" to PropertySpec(Kind.StringKind, required = false),
            "transaction_id" to PropertySpec(Kind.StringKind, required = false),
        ),
        "purchase_completed" to mapOf(
            "product_id" to PropertySpec(Kind.StringKind, required = true),
            "price" to PropertySpec(Kind.NumberKind, required = false),
            "currency" to PropertySpec(Kind.StringKind, required = false),
            "quantity" to PropertySpec(Kind.IntKind, required = false),
            "transaction_id" to PropertySpec(Kind.StringKind, required = false),
        ),
    )

    /** Events whose contract definition pairs a monetary `price` with a required
     * sibling `currency` — specification/conventions.md. */
    val priceCurrencyPairedEvents: Set<String> = setOf("subscription_started", "purchase_completed")

    fun isCanonical(name: String): Boolean = schemaVersions.containsKey(name)
}

/**
 * Registry for app-specific (non-canonical) events'/metrics' `schema_version` —
 * specification/versioning.md's "App-specific event schema_version" section: the
 * contract cannot define app-specific events, so it delegates ownership of their
 * version to whichever application defines them. This adapter never assigns or infers
 * a version on its own; it only records whatever the application registers and places
 * it in the envelope unchanged, defaulting to 1 (the contract's documented initial
 * version) for a name never registered.
 *
 * [ConcurrentHashMap] gives the same thread safety a lock or an actor would, without
 * making `event(name)`/`metric(name)` `suspend` — [InfinityForgeEvent.custom] and
 * [InfinityForgeMetric.custom] call them synchronously (event/metric construction must
 * stay a plain, synchronous function callable from any thread — see
 * [InfinityForgeTrackingProvider]'s "no actor/main-thread requirement" note).
 *
 * `registerEvent`/`registerMetric` are one-time, developer-driven configuration calls
 * (typically made once, near app startup, directly by application code — never as part
 * of a live `track()`/`recordMetric()` call), not tracking data flowing through the
 * non-crashing runtime path. An invalid `version` here is an app bug, not malformed
 * tracking data, so — unlike [InfinityForgeEventValidation]/[InfinityForgeMetricValidation],
 * which always drop-and-log rather than throw, because that *is* the live, non-crashing
 * call path — this fails loudly via [require] so the mistake surfaces immediately during
 * development, matching the reference RN implementation's `registerAppSpecificEventSchemaVersion`
 * (which throws for the same input). `event(name)`/`metric(name)` themselves — the
 * lookup path every real `track()`/`recordMetric()` call goes through — are unaffected
 * and always safely default to `1` for anything never (successfully) registered.
 */
object InfinityForgeAppSpecificSchemaVersions {
    private val events = ConcurrentHashMap<String, Int>()
    private val metrics = ConcurrentHashMap<String, Int>()

    fun registerEvent(name: String, version: Int) {
        require(version >= 1) {
            "InfinityForge Tracking: registerEvent(\"$name\", $version) — schema_version " +
                "must be >= 1 (specification/versioning.md)."
        }
        events[name] = version
    }

    fun event(name: String): Int = events[name] ?: 1

    fun registerMetric(name: String, version: Int) {
        require(version >= 1) {
            "InfinityForge Tracking: registerMetric(\"$name\", $version) — schema_version " +
                "must be >= 1 (specification/versioning.md)."
        }
        metrics[name] = version
    }

    fun metric(name: String): Int = metrics[name] ?: 1

    /** Test-only: clears registered versions so tests don't leak state. */
    fun resetForTests() {
        events.clear()
        metrics.clear()
    }
}
