package com.factory.core.tracking

import com.factory.core.logging.Logger

/**
 * A minimal in-memory [Logger] test double, local to this module's own test suite
 * (mirroring the reference Swift implementation's `RecordingLogger`/`SilentLogger`
 * test doubles) — `core-testing` does not yet provide a shared fake `Logger`.
 */
class RecordingLogger : Logger {
    val messages = mutableListOf<String>()

    override fun debug(tag: String, message: String) {
        messages += message
    }

    override fun info(tag: String, message: String) {
        messages += message
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        messages += message
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        messages += message
    }
}
