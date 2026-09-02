package com.factory.core.testing

import com.factory.core.common.IdGenerator

/** Deterministic, sequential IDs (`id-1`, `id-2`, ...) instead of random UUIDs. */
class FakeIdGenerator(private val prefix: String = "id") : IdGenerator {
    private var counter = 0

    override fun newId(): String {
        counter += 1
        return "$prefix-$counter"
    }
}
