package com.factory.core.tracking

import com.factory.core.common.DispatcherProvider
import com.factory.core.common.IdGenerator
import com.factory.core.datastore.PreferenceKeys
import com.factory.core.datastore.PreferencesDataSource
import com.factory.core.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InfinityForgeIdentity"

/**
 * Implements specification/identity.md. `anonymous_id` is SDK-generated and persists
 * for the life of the app install (via `core-datastore`'s `PreferencesDataSource` —
 * no second identity store, no new persistence mechanism); `user_id` is app-set via
 * `identify()` and cleared by `reset()`, which always establishes a NEW `anonymous_id`
 * rather than reusing the old one (identity.md's privacy rationale for shared/reused
 * devices).
 *
 * `track`/`identify`/`setUserProperties`/`screen`/`reset`/`recordMetric` on
 * `InfinityForgeTrackingClient` must never block a user-facing action
 * (specification/api.md) — an in-memory snapshot (the properties below) is always
 * authoritative immediately, while persistence to [PreferencesDataSource] (itself
 * `suspend`) happens in an un-awaited coroutine on [dispatcherProvider]'s `io`
 * dispatcher, mirroring the reference Swift implementation's fire-and-forget `Task {}`
 * and the reference RN implementation's fire-and-forget `void persistAll()`.
 *
 * User properties are persisted through the same [PreferencesDataSource] as the two
 * identity identifiers, matching specification/api.md's requirement that they survive
 * app sessions. Persistence writes are chained in call order (via [persistenceJob]) so
 * a `reset()` cannot be overwritten by an older in-flight write — but chaining alone
 * only orders writes correctly if the *snapshot-and-chain* step itself can never
 * interleave across callers. `identify`/`setUserProperties`/`reset` are plain,
 * non-suspend functions and are deliberately callable from ANY thread (that's what
 * makes them non-blocking), so two of them can genuinely run at once on two different
 * threads. [stateLock] makes each call's state mutation, its snapshot of that state,
 * and its read-then-update of [persistenceJob] happen as one indivisible step — so
 * concurrent callers can never both read the same `previousJob` and race each other
 * afterward, which is what would let an older in-flight write land on disk after a
 * newer one (e.g. after `reset()`) and resurrect stale identity/user-property state.
 * The locked section only takes snapshots and starts a coroutine; it never performs
 * I/O itself, so holding it never blocks a caller on disk access.
 */
@Singleton
class InfinityForgeIdentity @Inject constructor(
    private val store: PreferencesDataSource,
    private val idGenerator: IdGenerator,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) {
    @Volatile var anonymousId: String = generateAnonymousId()
        private set

    @Volatile var userId: String? = null
        private set

    @Volatile var userProperties: Map<String, InfinityForgePropertyValue> = emptyMap()
        private set

    private var hydrated = false
    private val persistenceScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    /** Guards the state mutation + snapshot + [persistenceJob] read-then-update done by
     * [identify]/[setUserProperties]/[reset] — see the class doc comment. A plain JVM
     * monitor (not a [kotlinx.coroutines.sync.Mutex]) is correct and sufficient here
     * because the critical section never suspends: it only mutates fields, takes an
     * immutable snapshot, and calls `launch` (which itself never blocks). */
    private val stateLock = Any()

    @Volatile private var persistenceJob: Job? = null

    /**
     * Loads persisted identity state, if any. Safe to call more than once — only the
     * first call does anything. Every other member on this type is safe to call even
     * before this resolves; a fresh, valid in-memory `anonymous_id` always exists from
     * construction.
     */
    suspend fun hydrate() {
        if (hydrated) return
        hydrated = true

        val storedAnonymousId = store.stringFlow(PreferenceKeys.TRACKING_ANONYMOUS_ID, "").first()
        if (storedAnonymousId.isNotEmpty()) {
            anonymousId = storedAnonymousId
        } else {
            // First launch on this install — persist the freshly-generated anonymous_id
            // immediately.
            store.setString(PreferenceKeys.TRACKING_ANONYMOUS_ID, anonymousId)
        }

        val storedUserId = store.stringFlow(PreferenceKeys.TRACKING_USER_ID, "").first()
        if (storedUserId.isNotEmpty()) {
            userId = storedUserId
        }

        val storedProperties = store.stringFlow(PreferenceKeys.TRACKING_USER_PROPERTIES, "").first()
        if (storedProperties.isNotEmpty()) {
            userProperties = decodeUserProperties(storedProperties)
        }
    }

    /** No-ops on a blank `userId` rather than clearing identity — specification/api.md.
     * Calling this with a *different* `userId` than the one already active, without an
     * intervening `reset()`, is a misuse of the contract (specification/identity.md's
     * "Account switching semantics" — the calling application should `reset()` then
     * `identify()`). This adapter never crashes on it (it still overwrites `userId`,
     * same as before), but now surfaces a development diagnostic so the misuse is
     * visible rather than silent, matching the contract's "should log a development
     * diagnostic" recommendation for this specific case. */
    fun identify(newUserId: String) {
        if (newUserId.isBlank()) return
        synchronized(stateLock) {
            val previousUserId = userId
            if (previousUserId != null && previousUserId != newUserId) {
                logger.warn(
                    TAG,
                    "InfinityForge Tracking: identify() called with a different user_id " +
                        "while one was already active. Call reset() before identify() when " +
                        "switching accounts (specification/identity.md).",
                )
            }
            userId = newUserId
            persistInBackgroundLocked()
        }
    }

    fun setUserProperties(properties: Map<String, InfinityForgePropertyValue>) {
        if (properties.isEmpty()) return
        synchronized(stateLock) {
            userProperties = userProperties + properties
            persistInBackgroundLocked()
        }
    }

    /** Clears `user_id` and user properties and establishes a brand-new `anonymous_id`
     * — never a reused one — specification/identity.md. */
    fun reset() {
        synchronized(stateLock) {
            userId = null
            userProperties = emptyMap()
            anonymousId = generateAnonymousId()
            persistInBackgroundLocked()
        }
    }

    /** Must only be called while holding [stateLock] — see the class doc comment.
     * Snapshots the identity state that was just mutated under the same lock, then
     * chains a new write after whatever [persistenceJob] currently points to. Because
     * the snapshot and the [persistenceJob] read-then-update happen inside the same
     * critical section as the mutation itself, concurrent callers can never observe
     * (and chain onto) the same `previousJob` — writes reach disk in exactly the order
     * their calls acquired [stateLock], regardless of how many threads call
     * concurrently. */
    private fun persistInBackgroundLocked() {
        val anonymousIdSnapshot = anonymousId
        val userIdSnapshot = userId
        val userPropertiesSnapshot = userProperties
        val previousJob = persistenceJob
        persistenceJob = persistenceScope.launch {
            previousJob?.join()
            store.setString(PreferenceKeys.TRACKING_ANONYMOUS_ID, anonymousIdSnapshot)
            store.setString(PreferenceKeys.TRACKING_USER_ID, userIdSnapshot.orEmpty())
            if (userPropertiesSnapshot.isEmpty()) {
                store.setString(PreferenceKeys.TRACKING_USER_PROPERTIES, "")
            } else {
                val encoded = encodeUserProperties(userPropertiesSnapshot)
                if (encoded != null) {
                    store.setString(PreferenceKeys.TRACKING_USER_PROPERTIES, encoded)
                } else {
                    logger.warn(TAG, "InfinityForge Tracking: user properties could not be persisted.")
                }
            }
            logger.debug(TAG, "InfinityForge Tracking: identity persisted.")
        }
    }

    /** Allows lifecycle/tests to wait for already-scheduled persistence without making
     * the public tracking operations blocking. */
    suspend fun waitForPendingPersistence() {
        persistenceJob?.join()
    }

    private fun encodeUserProperties(properties: Map<String, InfinityForgePropertyValue>): String? = try {
        Json.encodeToString(JsonObject.serializer(), JsonObject(properties.mapValues { it.value.toJsonElement() }))
    } catch (error: SerializationException) {
        logger.warn(TAG, "InfinityForge Tracking: failed to encode user properties.", error)
        null
    }

    private fun decodeUserProperties(encoded: String): Map<String, InfinityForgePropertyValue> = try {
        Json.decodeFromString(JsonObject.serializer(), encoded)
            .mapNotNull { (key, element) -> element.toPropertyValueOrNull()?.let { key to it } }
            .toMap()
    } catch (error: SerializationException) {
        logger.warn(
            TAG,
            "InfinityForge Tracking: persisted user properties were unreadable; using an empty set.",
            error,
        )
        emptyMap()
    }

    /** Not cryptographically strong — a tracking anonymous/session identifier doesn't
     * need to be, only practically unique per install. Delegates to the injected
     * [IdGenerator] (never `UUID.randomUUID()` directly — see that interface's own
     * doc comment) so this identifier stays fake-able and deterministic in tests. */
    private fun generateAnonymousId(): String = "anon_${idGenerator.newId()}"
}
