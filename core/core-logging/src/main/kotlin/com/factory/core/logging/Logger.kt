package com.factory.core.logging

/**
 * The only logging entry point features/core modules should use — no module calls
 * `android.util.Log` or `println` directly. This is what makes it possible to (a) strip
 * verbose logs from release builds in one place and (b) route errors to Crashlytics
 * (see `core-analytics`'s `CrashReporter`) without every call site knowing about it.
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/** Logs to Logcat only. Verbose/debug logs are dropped when [isDebug] is false. */
class AndroidLogger(private val isDebug: Boolean) : Logger {
    override fun debug(tag: String, message: String) {
        if (isDebug) android.util.Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        android.util.Log.i(tag, message)
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.w(tag, message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.e(tag, message, throwable)
    }
}
