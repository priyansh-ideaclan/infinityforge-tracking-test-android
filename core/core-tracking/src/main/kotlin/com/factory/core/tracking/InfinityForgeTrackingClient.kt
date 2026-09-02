package com.factory.core.tracking

import com.factory.core.common.DispatcherProvider
import com.factory.core.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private const val TAG = "InfinityForgeTracking"

/** Mirrors the reference Swift implementation's `TelemetryAvailability` — a
 * diagnostic-only surface (never read by validation or dispatch logic) describing why
 * the current [InfinityForgeTrackingClient] is (or isn't) actually sending data
 * anywhere. */
enum class InfinityForgeTrackingAvailability {
    CONFIGURED,
    DEBUG,
    DISABLED,
    MISSING_CONFIGURATION,
}

/**
 * The public InfinityForge Tracking API — implements the six operations from
 * infinityforge-tracking-module's specification/api.md (initialize, track, identify,
 * setUserProperties, screen, reset) plus the optional Metrics capability
 * (recordMetric). This is the *only* shape feature code should depend on; it must
 * never depend on a concrete provider or vendor SDK directly (AGENTS.md §11's "no
 * module calls a vendor SDK directly" rule applies here exactly as it does to
 * `AnalyticsTracker`).
 *
 * `track`/`identify`/`setUserProperties`/`screen`/`reset`/`recordMetric` are
 * synchronous (non-`suspend`) and fire-and-forget by design — specification/api.md is
 * explicit these must never block a user-facing action. `initialize` alone is
 * `suspend`, matching every other injected dependency with setup work to await.
 */
interface InfinityForgeTrackingClient {
    val availability: InfinityForgeTrackingAvailability
    val providerNames: List<String>

    suspend fun initialize()
    fun track(event: InfinityForgeEvent)
    fun identify(userId: String)
    fun setUserProperties(properties: Map<String, InfinityForgePropertyValue>)
    fun screen(screenName: String, properties: Map<String, InfinityForgePropertyValue> = emptyMap())
    fun reset()
    fun recordMetric(metric: InfinityForgeMetric)
}

/** The safe, always-available disabled state — never touches identity persistence,
 * never validates, never dispatches to a provider. Mirrors the reference Swift
 * implementation's `NoOpInfinityForgeTrackingClient` exactly. */
class NoOpInfinityForgeTrackingClient(
    override val availability: InfinityForgeTrackingAvailability = InfinityForgeTrackingAvailability.DISABLED,
) : InfinityForgeTrackingClient {
    override val providerNames: List<String> = emptyList()

    override suspend fun initialize() {}
    override fun track(event: InfinityForgeEvent) {}
    override fun identify(userId: String) {}
    override fun setUserProperties(properties: Map<String, InfinityForgePropertyValue>) {}
    override fun screen(screenName: String, properties: Map<String, InfinityForgePropertyValue>) {}
    override fun reset() {}
    override fun recordMetric(metric: InfinityForgeMetric) {}
}

/**
 * The real, validated engine: builds contract-compliant envelopes, validates every
 * call, and dispatches to [providers]. Used both for a fully "live" configuration
 * (real vendor providers registered) and for a development-only inspectable
 * configuration ([DebugLoggingInfinityForgeTrackingProvider] only, no real Firebase
 * project yet) — the validation/envelope/identity logic is identical either way, so
 * this is one engine parameterized by its providers rather than several near-duplicate
 * classes.
 *
 * Every dispatch runs on its own internal [CoroutineScope] (backed by
 * [dispatcherProvider]'s `default` dispatcher, never `Dispatchers.Default` referenced
 * directly — see `DispatcherProvider`'s own doc comment) rather than requiring an
 * externally-supplied application scope: this keeps the tracking module fully
 * self-contained and independently testable, mirroring how the reference Swift
 * implementation's `Task {}` calls need no externally-supplied scope either.
 */
class InfinityForgeTrackingClientImpl(
    override val availability: InfinityForgeTrackingAvailability,
    private val providers: List<InfinityForgeTrackingProvider>,
    private val identity: InfinityForgeIdentity,
    private val metadata: InfinityForgeMetadata.Base,
    private val logger: Logger,
    dispatcherProvider: DispatcherProvider,
) : InfinityForgeTrackingClient {
    override val providerNames: List<String> get() = providers.map { it.name }

    private val dispatchScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)

    @Volatile private var lastScreenName: String? = null

    override suspend fun initialize() {
        identity.hydrate()
        // Every provider's initialize() is dispatched concurrently and independently
        // — one provider's slow/failing initialize() must never delay or break
        // another's, or the app's own startup (specification/errors.md).
        InfinityForgeDispatcher.dispatch(dispatchScope, providers, "initialize", logger, metadata.environment) {
            it.initialize()
        }
    }

    override fun track(event: InfinityForgeEvent) {
        val result = InfinityForgeEventValidation.validateTrackCall(event.name, event.properties)
        if (!result.isValid) {
            logValidationFailure(event.name, result)
            return
        }
        if (InfinityForgeEventValidation.isAppSpecificEvent(event.name)) {
            logger.debug(
                TAG,
                "InfinityForge Tracking: track() called with an app-specific event " +
                    "(not in the canonical registry): ${event.name}",
            )
        }
        dispatchEvent(event)
    }

    override fun identify(userId: String) {
        val result = InfinityForgeEventValidation.validateUserId(userId)
        if (!result.isValid) {
            logValidationFailure("identify()", result)
            return
        }
        identity.identify(userId)
        InfinityForgeDispatcher.dispatch(dispatchScope, providers, "identify", logger, metadata.environment) {
            it.identify(userId)
        }
    }

    override fun setUserProperties(properties: Map<String, InfinityForgePropertyValue>) {
        if (properties.isEmpty()) return
        val result = InfinityForgeEventValidation.validateUserProperties(properties)
        if (!result.isValid) {
            logValidationFailure("setUserProperties()", result)
            return
        }
        identity.setUserProperties(properties)
        InfinityForgeDispatcher.dispatch(dispatchScope, providers, "setUserProperties", logger, metadata.environment) {
            it.setUserProperties(properties)
        }
    }

    override fun screen(screenName: String, properties: Map<String, InfinityForgePropertyValue>) {
        val nameResult = InfinityForgeEventValidation.validateScreenName(screenName)
        if (!nameResult.isValid) {
            logValidationFailure("screen()", nameResult)
            return
        }
        if (screenName == lastScreenName) return
        val event = InfinityForgeEvent.screenViewed(screenName, lastScreenName, properties)
        val result = InfinityForgeEventValidation.validateTrackCall(event.name, event.properties)
        if (!result.isValid) {
            logValidationFailure(event.name, result)
            return
        }
        lastScreenName = screenName
        dispatchEvent(event)
    }

    override fun reset() {
        identity.reset()
        lastScreenName = null
        InfinityForgeDispatcher.dispatch(dispatchScope, providers, "reset", logger, metadata.environment) { it.reset() }
    }

    override fun recordMetric(metric: InfinityForgeMetric) {
        val result = InfinityForgeMetricValidation.validateRecordMetricCall(metric)
        if (!result.isValid) {
            logValidationFailure(metric.metricName, result)
            return
        }
        if (InfinityForgeMetricValidation.isAppSpecificMetric(metric.metricName)) {
            logger.debug(
                TAG,
                "InfinityForge Tracking: recordMetric() called with an app-specific metric " +
                    "(not in the canonical registry): ${metric.metricName}",
            )
        }
        val envelope = InfinityForgeMetricEnvelope(
            metricName = metric.metricName,
            schemaVersion = metric.schemaVersion,
            value = metric.value,
            unit = metric.unit,
            currency = metric.currency,
            source = metric.source,
            referenceId = metric.referenceId,
            timestamp = InfinityForgeMetadata.timestamp(),
            appId = metadata.appId,
            environment = metadata.environment,
            platform = metadata.platform,
            sdkVersion = metadata.sdkVersion,
            sdkName = metadata.sdkName,
            appVersion = metadata.appVersion,
            userId = identity.userId,
            anonymousId = identity.anonymousId,
            dimensions = metric.dimensions,
        )
        InfinityForgeDispatcher.dispatch(dispatchScope, providers, "recordMetric", logger, metadata.environment) {
            it.recordMetric(envelope)
        }
    }

    private fun dispatchEvent(event: InfinityForgeEvent) {
        val envelope = InfinityForgeEventEnvelope(
            event = event.name,
            schemaVersion = event.schemaVersion,
            timestamp = InfinityForgeMetadata.timestamp(),
            appId = metadata.appId,
            environment = metadata.environment,
            platform = metadata.platform,
            sdkVersion = metadata.sdkVersion,
            sdkName = metadata.sdkName,
            appVersion = metadata.appVersion,
            userId = identity.userId,
            anonymousId = identity.anonymousId,
            properties = event.properties,
        )
        InfinityForgeDispatcher.dispatch(dispatchScope, providers, "send", logger, metadata.environment) {
            it.send(envelope)
        }
    }

    /** specification/errors.md: useful diagnostics during development, but production
     * diagnostics must never leak sensitive information. `result.issues` can include a
     * rejected property's raw value — arbitrary application data
     * specification/privacy.md prohibits assuming is safe to log unconditionally — so
     * only `subject` (an application-chosen identifier, never user data) is safe in
     * every environment. */
    private fun logValidationFailure(subject: String, result: InfinityForgeValidationResult) {
        val safeMessage = "InfinityForge Tracking: validation failed: $subject"
        if (metadata.environment == InfinityForgeEnvironment.PRODUCTION) {
            logger.warn(TAG, safeMessage)
        } else {
            val detail = result.issues.joinToString("\n") { "  - $it" }
            logger.warn(TAG, "$safeMessage\n$detail")
        }
    }
}
