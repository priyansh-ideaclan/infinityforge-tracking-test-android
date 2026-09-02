package com.factory.core.tracking

import com.factory.core.testing.FakeDispatcherProvider
import com.factory.core.testing.FakeIdGenerator
import com.factory.core.testing.FakePreferencesDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class CapturingProvider : InfinityForgeTrackingProvider {
    override val name = "capturing"
    val sentEvents = mutableListOf<InfinityForgeEventEnvelope>()
    val recordedMetrics = mutableListOf<InfinityForgeMetricEnvelope>()
    val identifiedUserIds = mutableListOf<String>()
    val resetCount get() = resets
    private var resets = 0

    override suspend fun send(envelope: InfinityForgeEventEnvelope) {
        sentEvents += envelope
    }

    override suspend fun recordMetric(envelope: InfinityForgeMetricEnvelope) {
        recordedMetrics += envelope
    }

    override suspend fun identify(userId: String) {
        identifiedUserIds += userId
    }

    override suspend fun reset() {
        resets += 1
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InfinityForgeTrackingClientImplTest {

    private lateinit var provider: CapturingProvider
    private lateinit var identity: InfinityForgeIdentity
    private lateinit var logger: RecordingLogger
    private lateinit var client: InfinityForgeTrackingClientImpl
    private val base = InfinityForgeMetadata.Base(
        appId = "com.example.app",
        appVersion = "1.0",
        environment = InfinityForgeEnvironment.DEVELOPMENT,
        platform = InfinityForgePlatform.ANDROID,
        sdkName = "kotlin",
        sdkVersion = "1.0.0",
    )

    @Before
    fun setUp() {
        InfinityForgeAppSpecificSchemaVersions.resetForTests()
        provider = CapturingProvider()
        logger = RecordingLogger()
        val dispatcherProvider = FakeDispatcherProvider()
        identity = InfinityForgeIdentity(FakePreferencesDataSource(), FakeIdGenerator(), dispatcherProvider, logger)
        client = InfinityForgeTrackingClientImpl(
            availability = InfinityForgeTrackingAvailability.DEBUG,
            providers = listOf(provider),
            identity = identity,
            metadata = base,
            logger = logger,
            dispatcherProvider = dispatcherProvider,
        )
    }

    @Test
    fun `track builds a contract-shaped envelope with the current identity`() = runTest {
        client.track(InfinityForgeEvent.appOpened(InfinityForgeLaunchType.COLD))
        advanceUntilIdle()

        assertEquals(1, provider.sentEvents.size)
        val envelope = provider.sentEvents.first()
        assertEquals("app_opened", envelope.event)
        assertEquals(1, envelope.schemaVersion)
        assertEquals("com.example.app", envelope.appId)
        assertEquals(InfinityForgeEnvironment.DEVELOPMENT, envelope.environment)
        assertEquals(InfinityForgePlatform.ANDROID, envelope.platform)
        assertEquals("kotlin", envelope.sdkName)
        assertEquals(identity.anonymousId, envelope.anonymousId)
        assertNull(envelope.userId)
    }

    @Test
    fun `an invalid track call is dropped before reaching a provider`() = runTest {
        client.track(InfinityForgeEvent.custom("Not Snake Case"))
        advanceUntilIdle()

        assertTrue(provider.sentEvents.isEmpty())
        assertTrue(logger.messages.any { it.contains("validation failed") })
    }

    @Test
    fun `identify updates identity and dispatches to the provider`() = runTest {
        client.identify("user_1")
        advanceUntilIdle()

        assertEquals("user_1", identity.userId)
        assertEquals(listOf("user_1"), provider.identifiedUserIds)
    }

    @Test
    fun `events after identify carry both anonymous_id and user_id`() = runTest {
        client.identify("user_1")
        client.track(InfinityForgeEvent.appOpened())
        advanceUntilIdle()

        val envelope = provider.sentEvents.first()
        assertEquals("user_1", envelope.userId)
        assertEquals(identity.anonymousId, envelope.anonymousId)
    }

    @Test
    fun `screen suppresses a duplicate consecutive call for the same screen`() = runTest {
        client.screen("home")
        client.screen("home")
        advanceUntilIdle()

        assertEquals(1, provider.sentEvents.size)
    }

    @Test
    fun `screen emits again after navigating to a different screen and back`() = runTest {
        client.screen("home")
        client.screen("settings")
        client.screen("home")
        advanceUntilIdle()

        assertEquals(3, provider.sentEvents.size)
        assertEquals("settings", provider.sentEvents[2].properties["previous_screen"]?.let {
            (it as InfinityForgePropertyValue.StringValue).value
        })
    }

    @Test
    fun `reset clears identity and dispatches reset to every provider`() = runTest {
        client.identify("user_1")
        client.screen("home")
        advanceUntilIdle()

        client.reset()
        advanceUntilIdle()

        assertNull(identity.userId)
        assertEquals(1, provider.resetCount)

        // A screen re-visited after reset is not suppressed as a duplicate — reset()
        // clears lastScreenName too, so this second "home" is a fresh occurrence.
        client.screen("home")
        advanceUntilIdle()
        assertEquals(2, provider.sentEvents.size)
    }

    @Test
    fun `recordMetric builds a contract-shaped metric envelope`() = runTest {
        client.recordMetric(
            InfinityForgeMetric.revenue(9.99, "USD", InfinityForgeTransactionType.CHARGE),
        )
        advanceUntilIdle()

        assertEquals(1, provider.recordedMetrics.size)
        val envelope = provider.recordedMetrics.first()
        assertEquals("revenue", envelope.metricName)
        assertEquals(InfinityForgeMetricUnit.CURRENCY, envelope.unit)
        assertEquals("USD", envelope.currency)
        assertEquals(identity.anonymousId, envelope.anonymousId)
    }

    @Test
    fun `an invalid metric is dropped before reaching a provider`() = runTest {
        client.recordMetric(
            InfinityForgeMetric.custom(
                name = "revenue", value = -1.0, unit = InfinityForgeMetricUnit.CURRENCY, currency = "USD",
                source = InfinityForgeMetricSource.BILLING_SYSTEM,
            ),
        )
        advanceUntilIdle()

        assertTrue(provider.recordedMetrics.isEmpty())
    }

    @Test
    fun `initialize hydrates identity and calls provider initialize without throwing`() = runTest {
        client.initialize()
        advanceUntilIdle()
        // No assertion beyond "did not throw" — CapturingProvider's initialize() is
        // the interface's no-op default.
    }
}
