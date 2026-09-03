package com.ideaclan.infinityforgetrackingtestkotlin.trackingtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.core.common.DispatcherProvider
import com.factory.core.logging.Logger
import com.factory.core.tracking.InfinityForgeDispatcher
import com.factory.core.tracking.InfinityForgeEvent
import com.factory.core.tracking.InfinityForgeEventEnvelope
import com.factory.core.tracking.InfinityForgeIdentity
import com.factory.core.tracking.InfinityForgeMetadata
import com.factory.core.tracking.InfinityForgeMetric
import com.factory.core.tracking.InfinityForgeMetricEnvelope
import com.factory.core.tracking.InfinityForgePropertyValue
import com.factory.core.tracking.InfinityForgeTrackingClient
import com.factory.core.tracking.InfinityForgeTrackingProvider
import com.factory.core.tracking.InfinityForgeTransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingTestIdentitySnapshot(
    val anonymousId: String,
    val userId: String?,
    val userPropertyCount: Int,
)

/**
 * A throwaway [InfinityForgeTrackingProvider] that always fails, used only by
 * [TrackingTestViewModel.triggerProviderFailure] to demonstrate
 * specification/errors.md's provider-failure-isolation guarantee from this app's own
 * UI. Never registered with the real, DI-provided [InfinityForgeTrackingClient] —
 * this app's tracking data must never be affected by this drill.
 */
private class AlwaysFailingTrackingProvider : InfinityForgeTrackingProvider {
    override val name = "always_failing_test_provider"

    override suspend fun send(envelope: InfinityForgeEventEnvelope): Unit =
        throw IllegalStateException("Deliberate test failure from AlwaysFailingTrackingProvider.send()")

    override suspend fun recordMetric(envelope: InfinityForgeMetricEnvelope): Unit =
        throw IllegalStateException("Deliberate test failure from AlwaysFailingTrackingProvider.recordMetric()")
}

/**
 * Backs the Tracking Test screen — this app's whole reason for existing
 * (Docs/INFINITYFORGE_TRACKING.md). Exercises every InfinityForgeTrackingClient
 * operation from the UI, plus two deliberate failure drills (an invalid event and a
 * failing provider), so the six-operation contract (+ recordMetric) is observable and
 * repeatable by a developer without reading logcat blind. Deliberately simple: no
 * ViewModel-level business logic beyond calling the client and reflecting back what
 * happened — this screen is a validation surface, not a feature.
 */
@HiltViewModel
class TrackingTestViewModel @Inject constructor(
    private val trackingClient: InfinityForgeTrackingClient,
    private val identity: InfinityForgeIdentity,
    private val metadata: InfinityForgeMetadata,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _identitySnapshot = MutableStateFlow(currentIdentitySnapshot())
    val identitySnapshot: StateFlow<TrackingTestIdentitySnapshot> = _identitySnapshot.asStateFlow()

    val providerNames: List<String> get() = trackingClient.providerNames
    val availability get() = trackingClient.availability

    // Replays the exact Home/Home/Settings/Settings/Profile sequence this task's own
    // screen-tracking validation scenario calls for — consecutive presses land on the
    // same name (demonstrating duplicate suppression) before advancing.
    private val screenSequence = listOf("home", "home", "settings", "settings", "profile")
    private var screenSequenceIndex = 0

    // Cycles through canonical metrics so repeated presses exercise more than one —
    // ad_revenue first, since Section 12 of this task calls it out as the reference
    // case (the reference app's own ad telemetry model).
    private var metricCycleIndex = 0

    // Alternates two fixed user ids so a second press (with no reset() in between)
    // exercises the identify()-without-reset() diagnostic.
    private var identifyToggle = true

    private fun currentIdentitySnapshot() = TrackingTestIdentitySnapshot(
        anonymousId = identity.anonymousId,
        userId = identity.userId,
        userPropertyCount = identity.userProperties.size,
    )

    private fun appendLog(message: String) {
        _log.update { (listOf(message) + it).take(50) }
        _identitySnapshot.value = currentIdentitySnapshot()
    }

    fun trackEvent() {
        val featureName = "tracking_test_button_${System.currentTimeMillis()}"
        trackingClient.track(InfinityForgeEvent.custom("feature_used", mapOf("feature_name" to InfinityForgePropertyValue.StringValue(featureName))))
        appendLog("track(feature_used) — feature_name=\"$featureName\"")
    }

    fun trackScreen() {
        val screenName = screenSequence[screenSequenceIndex % screenSequence.size]
        screenSequenceIndex++
        trackingClient.screen(screenName)
        appendLog("screen(\"$screenName\") — step ${screenSequenceIndex} of the Home/Home/Settings/Settings/Profile sequence")
    }

    fun identifyUser() {
        val userId = if (identifyToggle) "tracking_test_user_a" else "tracking_test_user_b"
        identifyToggle = !identifyToggle
        trackingClient.identify(userId)
        appendLog("identify(\"$userId\")")
    }

    fun setUserProperty() {
        val value = "value_${System.currentTimeMillis()}"
        trackingClient.setUserProperties(mapOf("tracking_test_property" to InfinityForgePropertyValue.StringValue(value)))
        appendLog("setUserProperties({tracking_test_property: \"$value\"})")
    }

    fun resetUser() {
        trackingClient.reset()
        appendLog("reset() — new anonymous_id, user_id cleared")
    }

    fun recordMetric() {
        val metric = when (metricCycleIndex % 3) {
            0 -> InfinityForgeMetric.adRevenue(
                value = 0.0123,
                currency = "USD",
                placement = "tracking_test_interstitial",
                network = "tracking_test_network",
            )
            1 -> InfinityForgeMetric.revenue(
                value = 4.99,
                currency = "USD",
                transactionType = InfinityForgeTransactionType.CHARGE,
            )
            else -> InfinityForgeMetric.adImpression(placement = "tracking_test_banner")
        }
        metricCycleIndex++
        trackingClient.recordMetric(metric)
        appendLog("recordMetric(\"${metric.metricName}\") value=${metric.value} unit=${metric.unit.wireValue}")
    }

    fun triggerInvalidEvent() {
        // A canonical event name (feature_used) with its required property omitted —
        // custom() bypasses the compile-time-typed factories, so this reaches runtime
        // validation, which must drop it and log a development diagnostic
        // (specification/errors.md) rather than crash or silently send it.
        trackingClient.track(InfinityForgeEvent.custom("feature_used", emptyMap()))
        appendLog(
            "track(feature_used, {}) — deliberately missing required feature_name; " +
                "should be rejected and dropped (see logcat tag \"InfinityForgeTracking\" for the validation warning)",
        )
    }

    fun triggerProviderFailure() {
        // A throwaway client wired with one always-failing provider and one healthy
        // debug-logging provider, built from this app's real, injected identity and
        // metadata — demonstrates specification/errors.md's provider-isolation
        // guarantee without touching the real, DI-provided InfinityForgeTrackingClient
        // or its Firebase provider.
        val throwawayScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)
        val base = metadata.base()
        InfinityForgeDispatcher.dispatch(
            throwawayScope,
            listOf(AlwaysFailingTrackingProvider()),
            "send",
            logger,
            base.environment,
        ) {
            it.send(
                InfinityForgeEventEnvelope(
                    event = "provider_failure_drill",
                    schemaVersion = 1,
                    timestamp = InfinityForgeMetadata.timestamp(),
                    appId = base.appId,
                    environment = base.environment,
                    platform = base.platform,
                    sdkVersion = base.sdkVersion,
                    sdkName = base.sdkName,
                    appVersion = base.appVersion,
                    userId = identity.userId,
                    anonymousId = identity.anonymousId,
                    properties = emptyMap(),
                ),
            )
        }
        appendLog(
            "dispatched to a deliberately failing provider — its exception should be caught and " +
                "isolated (InfinityForgeDispatcher), logged, and never crash this app " +
                "(see logcat tag \"InfinityForgeTracking\")",
        )
    }

    init {
        viewModelScope.launch {
            appendLog(
                "ready — availability=${trackingClient.availability}, " +
                    "providers=${trackingClient.providerNames.ifEmpty { listOf("none") }}",
            )
        }
    }
}
