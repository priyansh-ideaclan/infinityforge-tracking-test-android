package com.factory.core.testing

import com.factory.core.common.Clock

/** Deterministic [Clock] for tests: time only advances when [advanceBy] is called. */
class FakeClock(initialEpochMillis: Long = 0L) : Clock {
    private var currentEpochMillis = initialEpochMillis

    override fun nowEpochMillis(): Long = currentEpochMillis

    fun advanceBy(millis: Long) {
        currentEpochMillis += millis
    }

    fun setTo(epochMillis: Long) {
        currentEpochMillis = epochMillis
    }
}
