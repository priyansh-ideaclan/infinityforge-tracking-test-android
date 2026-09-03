package com.factory.core.tracking

import com.factory.core.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "InfinityForgeTracking"

/**
 * The provider boundary — infinityforge-tracking-module's contract does not know about
 * analytics vendors, but a real implementation eventually needs to send data
 * somewhere. Mirrors the reference Swift implementation's `InfinityForgeTrackingProvider`
 * protocol and the reference RN implementation's `AnalyticsProvider` interface: every
 * method has a no-op default except [send] (a provider that only understands events is
 * still fully valid per specification/contract.md's "Metric conformance (optional
 * capability)" section) — a conforming type only overrides what it actually supports.
 *
 * Every method is `suspend` and may throw; a thrown exception is the *expected*,
 * handled path (caught and isolated by [InfinityForgeDispatcher], never by the
 * provider itself) — specification/errors.md's "Provider failures must be isolated."
 */
interface InfinityForgeTrackingProvider {
    val name: String

    /** Called once, from `InfinityForgeTrackingClient.initialize()`. */
    suspend fun initialize() = Unit

    /** One event envelope per `track()`/`screen()` call — the only method every
     * provider must implement. */
    suspend fun send(envelope: InfinityForgeEventEnvelope)

    /** One metric envelope per `recordMetric()` call. */
    suspend fun recordMetric(envelope: InfinityForgeMetricEnvelope) = Unit

    /** Mirrors `identify()` — called only after this adapter's own
     * validation/identity-store update already succeeded. */
    suspend fun identify(userId: String) = Unit

    /** Mirrors `setUserProperties()` — called only after this adapter's own
     * validation/identity-store update already succeeded. */
    suspend fun setUserProperties(properties: Map<String, InfinityForgePropertyValue>) = Unit

    /** Mirrors `reset()` — called after this adapter's own identity store has already
     * been cleared. */
    suspend fun reset() = Unit
}

/**
 * Dispatches one call to every registered provider, isolating each provider's failure
 * from every other provider and from the application — specification/errors.md.
 * Mirrors the reference Swift implementation's `InfinityForgeDispatcher` and the
 * reference RN implementation's `dispatch()`/`dispatchMetric()` helpers.
 */
object InfinityForgeDispatcher {
    fun dispatch(
        scope: CoroutineScope,
        providers: List<InfinityForgeTrackingProvider>,
        operation: String,
        logger: Logger,
        environment: InfinityForgeEnvironment,
        action: suspend (InfinityForgeTrackingProvider) -> Unit,
    ) {
        for (provider in providers) {
            scope.launch {
                try {
                    action(provider)
                } catch (cancellation: CancellationException) {
                    // Never swallow structured-concurrency cancellation — only a
                    // genuine provider failure is isolated here (specification/
                    // errors.md's "Provider failures must be isolated" is about a
                    // provider's own errors, not this scope being torn down).
                    throw cancellation
                } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                    // Deliberately broad: this is the one place specification/errors.md's
                    // "Provider failures must be isolated" is enforced, and a provider is
                    // arbitrary vendor/third-party code (Firebase SDK today, any future
                    // provider) that can throw literally any Exception subtype — there is
                    // no narrower type that still satisfies "isolate any provider failure."
                    // AGENTS.md §8 sanctions exactly this: a narrow, commented suppression
                    // at the call site over a blanket rule disable or baseline.
                    logProviderFailure(provider, operation, error, logger, environment)
                }
            }
        }
    }

    fun logProviderFailure(
        provider: InfinityForgeTrackingProvider,
        operation: String,
        error: Throwable,
        logger: Logger,
        environment: InfinityForgeEnvironment,
    ) {
        val message = "$TAG: provider \"${provider.name}\" failed during $operation"
        if (environment == InfinityForgeEnvironment.PRODUCTION) {
            // Production diagnostics never include a provider's raw error message — it
            // could in principle echo back something sensitive (a malformed request
            // body, a header value) — specification/errors.md.
            logger.warn(TAG, message)
        } else {
            logger.warn(TAG, "$message: $error")
        }
    }
}

/**
 * A development-only provider that logs every envelope's shape (never
 * property/dimension *values*, matching this factory's existing `AnalyticsTracker`
 * diagnostics policy) instead of sending it anywhere — used when Firebase is not yet
 * configured (no `google-services.json`), so flows remain inspectable without provider
 * traffic. See `TrackingModule`'s provider-selection logic.
 */
class DebugLoggingInfinityForgeTrackingProvider(private val logger: Logger) : InfinityForgeTrackingProvider {
    override val name = "debug"

    override suspend fun send(envelope: InfinityForgeEventEnvelope) {
        val keys = envelope.properties.keys.sorted().joinToString(", ")
        logger.debug(TAG, "InfinityForge Tracking (debug): event \"${envelope.event}\" properties: [$keys]")
    }

    override suspend fun recordMetric(envelope: InfinityForgeMetricEnvelope) {
        logger.debug(
            TAG,
            "InfinityForge Tracking (debug): metric \"${envelope.metricName}\" " +
                "unit=${envelope.unit.wireValue} source=${envelope.source.wireValue}",
        )
    }
}
