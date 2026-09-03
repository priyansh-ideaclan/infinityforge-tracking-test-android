package com.factory.core.tracking

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingProvider(override val name: String) : InfinityForgeTrackingProvider {
    val sentEvents = mutableListOf<InfinityForgeEventEnvelope>()

    override suspend fun send(envelope: InfinityForgeEventEnvelope) {
        sentEvents += envelope
    }
}

private class FailingProvider(override val name: String, private val error: Throwable) : InfinityForgeTrackingProvider {
    override suspend fun send(envelope: InfinityForgeEventEnvelope) {
        throw error
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InfinityForgeTrackingProviderTest {

    private fun sampleEnvelope() = InfinityForgeEventEnvelope(
        event = "app_opened",
        schemaVersion = 1,
        timestamp = "2026-01-01T00:00:00Z",
        appId = "com.example.app",
        environment = InfinityForgeEnvironment.DEVELOPMENT,
        platform = InfinityForgePlatform.ANDROID,
        sdkVersion = "1.0.0",
        sdkName = "kotlin",
        appVersion = "1.0",
        userId = null,
        anonymousId = "anon_1",
        properties = emptyMap(),
    )

    @Test
    fun `a failing provider does not prevent another provider from receiving the call`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = kotlinx.coroutines.CoroutineScope(dispatcher)
        val logger = RecordingLogger()
        val failing = FailingProvider("failing", RuntimeException("boom"))
        val healthy = RecordingProvider("healthy")

        InfinityForgeDispatcher.dispatch(
            scope,
            listOf(failing, healthy),
            "send",
            logger,
            InfinityForgeEnvironment.DEVELOPMENT,
        ) { it.send(sampleEnvelope()) }
        advanceUntilIdle()

        assertEquals(1, healthy.sentEvents.size)
    }

    @Test
    fun `a provider failure is isolated - never rethrown to the caller`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = kotlinx.coroutines.CoroutineScope(dispatcher)
        val logger = RecordingLogger()
        val failing = FailingProvider("failing", RuntimeException("boom"))

        // Must not throw.
        InfinityForgeDispatcher.dispatch(
            scope,
            listOf(failing),
            "send",
            logger,
            InfinityForgeEnvironment.DEVELOPMENT,
        ) { it.send(sampleEnvelope()) }
        advanceUntilIdle()

        assertTrue(logger.messages.any { it.contains("failing") && it.contains("send") })
    }

    @Test
    fun `production diagnostics never include the raw error message`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = kotlinx.coroutines.CoroutineScope(dispatcher)
        val logger = RecordingLogger()
        val failing = FailingProvider("failing", RuntimeException("super secret detail"))

        InfinityForgeDispatcher.dispatch(
            scope,
            listOf(failing),
            "send",
            logger,
            InfinityForgeEnvironment.PRODUCTION,
        ) { it.send(sampleEnvelope()) }
        advanceUntilIdle()

        assertTrue(logger.messages.none { it.contains("super secret detail") })
    }

    @Test
    fun `default provider methods are safe no-ops for a provider that only implements send`() = runTest {
        val provider = RecordingProvider("minimal")

        provider.initialize()
        provider.recordMetric(
            InfinityForgeMetricEnvelope(
                metricName = "revenue", schemaVersion = 1, value = 1.0, unit = InfinityForgeMetricUnit.CURRENCY,
                currency = "USD", source = InfinityForgeMetricSource.BILLING_SYSTEM, referenceId = null,
                timestamp = "2026-01-01T00:00:00Z", appId = "app", environment = InfinityForgeEnvironment.DEVELOPMENT,
                platform = InfinityForgePlatform.ANDROID, sdkVersion = "1.0.0", sdkName = "kotlin", appVersion = "1.0",
                userId = null, anonymousId = "anon_1", dimensions = emptyMap(),
            ),
        )
        provider.identify("user_1")
        provider.setUserProperties(emptyMap())
        provider.reset()
        // No exception thrown means the interface's no-op defaults work as documented.
    }
}
