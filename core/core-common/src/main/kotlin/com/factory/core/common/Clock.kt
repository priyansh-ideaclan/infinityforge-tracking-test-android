package com.factory.core.common

import javax.inject.Inject

/**
 * Deterministic time boundary. Nothing in this factory calls `System.currentTimeMillis()`
 * or `Instant.now()` directly outside this interface's default implementation — that is
 * what makes time-dependent logic (token expiry, analytics timestamps, "joined N days
 * ago") unit-testable via `core-testing`'s `FakeClock`.
 */
interface Clock {
    fun nowEpochMillis(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
