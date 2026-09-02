package com.factory.core.tracking.firebase

import com.factory.core.logging.Logger
import com.factory.core.tracking.InfinityForgeEnvironment
import com.factory.core.tracking.InfinityForgeEventEnvelope
import com.factory.core.tracking.InfinityForgeMetricEnvelope
import com.factory.core.tracking.InfinityForgePropertyValue
import com.factory.core.tracking.InfinityForgeTrackingProvider
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

private const val TAG = "InfinityForgeTracking"

/**
 * The real Firebase Analytics provider behind [InfinityForgeTrackingProvider] — the
 * first real vendor for this capability on Android (this phase implements the
 * boundary only; no Tracking test UI or real Firebase project verification happens in
 * this phase — see Docs/INFINITYFORGE_TRACKING.md). Every translation decision lives
 * in [FirebaseInfinityForgeMapping] as pure, SDK-free functions; this type only turns
 * mapped values into actual `FirebaseAnalytics` SDK calls.
 *
 * Error handling: deliberately no `try`/`catch` of its own — every method here is
 * called through `InfinityForgeDispatcher`, which already isolates a provider's
 * failure from every other provider and from the application (specification/
 * errors.md) and applies the production-safe diagnostic rule. The Firebase Analytics
 * Kotlin API used here does not itself throw for these calls; the provider interface's
 * methods are `suspend` only to satisfy the shared provider contract.
 *
 * Identity mapping — specification/identity.md:
 * - `identify(user_id)` -> Firebase's own `setUserId`.
 * - `anonymous_id` does NOT map onto Firebase's own auto-generated App Instance ID —
 *   that identifier is generated and owned entirely by the Firebase SDK and cannot be
 *   set or overridden by this adapter (a genuine Firebase-specific limitation, not a
 *   contract violation: the contract's `anonymous_id` is fully intact everywhere
 *   else). Instead `infinityforge_anonymous_id` rides along as an event parameter on
 *   every event/metric (see [FirebaseInfinityForgeMapping]), keeping it queryable
 *   downstream (e.g. a BigQuery export).
 * - `reset()` -> Firebase's own `resetAnalyticsData`, the closest Firebase-native
 *   equivalent of "establish a new anonymous_id, never reuse the old one."
 */
class FirebaseInfinityForgeProvider(
    private val analytics: FirebaseAnalytics,
    private val logger: Logger,
    private val environment: InfinityForgeEnvironment,
) : InfinityForgeTrackingProvider {
    override val name = "firebase"

    override suspend fun send(envelope: InfinityForgeEventEnvelope) {
        if (envelope.event == "screen_viewed") {
            val mapped = FirebaseInfinityForgeMapping.mapScreenView(envelope)
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, mapped.screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, mapped.screenClass)
            }
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            return
        }
        val mapped = FirebaseInfinityForgeMapping.mapEvent(envelope)
        warnAboutDropped("event parameter", mapped.dropped)
        analytics.logEvent(mapped.name, mapped.params.toBundle())
    }

    override suspend fun recordMetric(envelope: InfinityForgeMetricEnvelope) {
        val mapped = FirebaseInfinityForgeMapping.mapMetric(envelope)
        warnAboutDropped("metric dimension", mapped.dropped)
        analytics.logEvent(mapped.name, mapped.params.toBundle())
    }

    override suspend fun identify(userId: String) {
        analytics.setUserId(FirebaseInfinityForgeMapping.sanitizeUserId(userId))
    }

    override suspend fun setUserProperties(properties: Map<String, InfinityForgePropertyValue>) {
        val mapped = FirebaseInfinityForgeMapping.mapUserProperties(properties)
        warnAboutDropped("user property", mapped.dropped)
        for ((key, value) in mapped.properties) {
            analytics.setUserProperty(key, value)
        }
    }

    override suspend fun reset() {
        analytics.resetAnalyticsData()
    }

    /** Development-only visibility for dropped field *names* (never values — see
     * [FirebaseInfinityForgeMapping]) that exceeded Firebase's parameter-count
     * ceiling — errors.md's rule for malformed/oversized input ("expose useful
     * diagnostics during development"). Never fires in production. */
    private fun warnAboutDropped(kind: String, dropped: List<String>) {
        if (dropped.isEmpty() || environment == InfinityForgeEnvironment.PRODUCTION) return
        logger.warn(
            TAG,
            "InfinityForge Tracking: Firebase provider dropped ${dropped.size} $kind(s) " +
                "exceeding Firebase's limits: ${dropped.joinToString(", ")}",
        )
    }
}

private fun Map<String, Any>.toBundle(): Bundle {
    val bundle = Bundle()
    for ((key, value) in this) {
        when (value) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Double -> bundle.putDouble(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            else -> bundle.putString(key, value.toString())
        }
    }
    return bundle
}
