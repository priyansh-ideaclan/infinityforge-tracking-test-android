package com.factory.core.common

import java.util.UUID
import javax.inject.Inject

/**
 * Deterministic ID boundary, matching [Clock]'s reasoning: nothing outside this
 * interface's default implementation calls `UUID.randomUUID()` directly, so ID
 * generation is replaceable with a sequential fake in tests.
 */
interface IdGenerator {
    fun newId(): String
}

class UuidIdGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
