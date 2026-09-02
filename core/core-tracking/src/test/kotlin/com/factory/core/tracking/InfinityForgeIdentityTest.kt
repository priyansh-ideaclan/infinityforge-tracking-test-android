package com.factory.core.tracking

import com.factory.core.common.DefaultDispatcherProvider
import com.factory.core.datastore.PreferenceKeys
import com.factory.core.testing.FakeDispatcherProvider
import com.factory.core.testing.FakeIdGenerator
import com.factory.core.testing.FakePreferencesDataSource
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InfinityForgeIdentityTest {

    private lateinit var store: FakePreferencesDataSource
    private lateinit var identity: InfinityForgeIdentity
    private lateinit var logger: RecordingLogger

    @Before
    fun setUp() {
        store = FakePreferencesDataSource()
        logger = RecordingLogger()
        identity = InfinityForgeIdentity(
            store = store,
            idGenerator = FakeIdGenerator(prefix = "id"),
            dispatcherProvider = FakeDispatcherProvider(),
            logger = logger,
        )
    }

    @Test
    fun `a fresh identity has an anonymous_id and no user_id before hydrate`() {
        assertTrue(identity.anonymousId.startsWith("anon_"))
        assertNull(identity.userId)
        assertTrue(identity.userProperties.isEmpty())
    }

    @Test
    fun `hydrate is a no-op on first call beyond persisting the in-memory anonymous_id`() = runTest {
        val before = identity.anonymousId
        identity.hydrate()
        assertEquals(before, identity.anonymousId)
    }

    @Test
    fun `hydrate loads a previously persisted anonymous_id instead of the fresh one`() = runTest {
        store.setString(com.factory.core.datastore.PreferenceKeys.TRACKING_ANONYMOUS_ID, "anon_persisted")

        identity.hydrate()

        assertEquals("anon_persisted", identity.anonymousId)
    }

    @Test
    fun `identify sets user_id`() = runTest {
        identity.identify("user_1")
        assertEquals("user_1", identity.userId)
    }

    @Test
    fun `identify with a blank user_id is a no-op`() = runTest {
        identity.identify("user_1")
        identity.identify("   ")
        assertEquals("user_1", identity.userId)
    }

    @Test
    fun `identify persists user_id so it survives a new InfinityForgeIdentity instance`() = runTest {
        identity.identify("user_1")
        identity.waitForPendingPersistence()

        val rehydrated = InfinityForgeIdentity(store, FakeIdGenerator(), FakeDispatcherProvider(), RecordingLogger())
        rehydrated.hydrate()

        assertEquals("user_1", rehydrated.userId)
    }

    @Test
    fun `setUserProperties merges onto the existing set`() {
        identity.setUserProperties(mapOf("plan" to InfinityForgePropertyValue.StringValue("pro")))
        identity.setUserProperties(mapOf("theme" to InfinityForgePropertyValue.StringValue("dark")))

        assertEquals(2, identity.userProperties.size)
        assertEquals(
            InfinityForgePropertyValue.StringValue("pro"),
            identity.userProperties["plan"],
        )
    }

    @Test
    fun `setUserProperties with an empty map is a no-op`() {
        identity.setUserProperties(mapOf("plan" to InfinityForgePropertyValue.StringValue("pro")))
        identity.setUserProperties(emptyMap())

        assertEquals(1, identity.userProperties.size)
    }

    @Test
    fun `setUserProperties persists and survives rehydration`() = runTest {
        identity.setUserProperties(
            mapOf(
                "plan" to InfinityForgePropertyValue.StringValue("pro"),
                "seat_count" to InfinityForgePropertyValue.IntValue(5),
                "trial" to InfinityForgePropertyValue.BooleanValue(false),
            ),
        )
        identity.waitForPendingPersistence()

        val rehydrated = InfinityForgeIdentity(store, FakeIdGenerator(), FakeDispatcherProvider(), RecordingLogger())
        rehydrated.hydrate()

        assertEquals(
            InfinityForgePropertyValue.StringValue("pro"),
            rehydrated.userProperties["plan"],
        )
        assertEquals(InfinityForgePropertyValue.IntValue(5), rehydrated.userProperties["seat_count"])
        assertEquals(InfinityForgePropertyValue.BooleanValue(false), rehydrated.userProperties["trial"])
    }

    // -- reset() : the core identity/privacy behavior --

    @Test
    fun `reset clears user_id and user properties`() {
        identity.identify("user_1")
        identity.setUserProperties(mapOf("plan" to InfinityForgePropertyValue.StringValue("pro")))

        identity.reset()

        assertNull(identity.userId)
        assertTrue(identity.userProperties.isEmpty())
    }

    @Test
    fun `reset establishes a brand-new anonymous_id, never reusing the old one`() {
        val before = identity.anonymousId

        identity.reset()

        assertNotEquals(before, identity.anonymousId)
    }

    @Test
    fun `reset is persisted so a rehydrated identity never sees the old user_id`() = runTest {
        identity.identify("user_1")
        identity.reset()
        identity.waitForPendingPersistence()

        val rehydrated = InfinityForgeIdentity(store, FakeIdGenerator(), FakeDispatcherProvider(), RecordingLogger())
        rehydrated.hydrate()

        assertNull(rehydrated.userId)
        assertTrue(rehydrated.userProperties.isEmpty())
    }

    @Test
    fun `an unreadable persisted user properties value is treated as empty, not a crash`() = runTest {
        store.setString(com.factory.core.datastore.PreferenceKeys.TRACKING_USER_PROPERTIES, "{not valid json")

        identity.hydrate()

        assertTrue(identity.userProperties.isEmpty())
        assertTrue(logger.messages.any { it.contains("unreadable") })
    }

    // -- concurrency: reset() cannot be followed by a stale write (Phase 6B review fix) --
    //
    // These use real Dispatchers.IO/Default (via DefaultDispatcherProvider), not
    // FakeDispatcherProvider's single-threaded UnconfinedTestDispatcher, because the
    // bug this guards against only exists across genuinely concurrent JVM threads:
    // identify()/setUserProperties()/reset() are plain non-suspend functions callable
    // from any thread, and it's two such calls racing on *different* threads that could
    // previously both read the same in-flight persistenceJob and de-serialize disk
    // writes relative to call order.

    private val realDispatchers = DefaultDispatcherProvider()

    /** Runs [first] and [second] on two separate threads, released as close to
     * simultaneously as a [CountDownLatch] gate can manage, then waits for both to
     * finish — forcing genuine cross-thread contention on the lock under test rather
     * than relying on incidental scheduling. */
    private fun runConcurrently(first: () -> Unit, second: () -> Unit) {
        val ready = CountDownLatch(2)
        val go = CountDownLatch(1)
        val threads = listOf(first, second).map { action ->
            Thread {
                ready.countDown()
                go.await()
                action()
            }
        }
        threads.forEach { it.start() }
        ready.await()
        go.countDown()
        threads.forEach { it.join() }
    }

    @Test
    fun `reset racing with an in-flight identify never leaves a stale user_id persisted`() {
        repeat(30) { trial ->
            val store = FakePreferencesDataSource()
            val identity = InfinityForgeIdentity(
                store, FakeIdGenerator(prefix = "id"), realDispatchers, RecordingLogger(),
            )

            runConcurrently(
                { identity.identify("stale_user_$trial") },
                { identity.reset() },
            )
            runBlocking { identity.waitForPendingPersistence() }

            val persistedUserId = runBlocking {
                store.stringFlow(PreferenceKeys.TRACKING_USER_ID, "").first()
            }

            // Whichever call actually happened last (per InfinityForgeIdentity's
            // internal lock — either outcome is legitimate under a genuine race), the
            // persisted value must match the in-memory value exactly: no stale write
            // from the losing call can land on disk after the winning call's write.
            assertEquals(
                "trial $trial: persisted user_id must match in-memory user_id, never a stale value",
                identity.userId.orEmpty(),
                persistedUserId,
            )
            // The specific case this review flagged: if reset() was the call that
            // logically happened last, the persisted user_id must be cleared, not the
            // identify() call's stale value.
            if (identity.userId == null) {
                assertTrue(
                    "trial $trial: reset() must not be followed by a stale identify() write",
                    persistedUserId.isEmpty(),
                )
            }
        }
    }

    @Test
    fun `reset racing with an in-flight setUserProperties never leaves stale properties persisted`() {
        repeat(30) { trial ->
            val store = FakePreferencesDataSource()
            val identity = InfinityForgeIdentity(
                store, FakeIdGenerator(prefix = "id"), realDispatchers, RecordingLogger(),
            )

            runConcurrently(
                {
                    identity.setUserProperties(
                        mapOf("plan_$trial" to InfinityForgePropertyValue.StringValue("pro")),
                    )
                },
                { identity.reset() },
            )
            runBlocking { identity.waitForPendingPersistence() }

            val persistedProperties = runBlocking {
                store.stringFlow(PreferenceKeys.TRACKING_USER_PROPERTIES, "").first()
            }

            if (identity.userProperties.isEmpty()) {
                // The specific case this review flagged: reset() winning must never be
                // followed by the setUserProperties() call's stale write landing after.
                assertTrue(
                    "trial $trial: reset() must not be followed by a stale setUserProperties() write",
                    persistedProperties.isEmpty(),
                )
            } else {
                assertTrue(
                    "trial $trial: persisted properties must reflect the winning setUserProperties() call",
                    persistedProperties.isNotEmpty(),
                )
            }
        }
    }
}
