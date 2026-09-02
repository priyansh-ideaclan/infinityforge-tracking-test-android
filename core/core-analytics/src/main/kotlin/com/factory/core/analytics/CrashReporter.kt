package com.factory.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

/**
 * The only Crashlytics entry point feature/core code uses. [recordException] and
 * [log] must never receive raw user PII (emails, tokens) — see AGENTS.md §7 and
 * `Docs/analytics/events.md`'s privacy-safe logging rules.
 */
interface CrashReporter {
    fun recordException(throwable: Throwable)
    fun log(message: String)
    fun setUserId(userId: String?)
}

class NoOpCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) = Unit
    override fun log(message: String) = Unit
    override fun setUserId(userId: String?) = Unit
}

class FirebaseCrashReporter @Inject constructor(
    private val crashlytics: FirebaseCrashlytics,
) : CrashReporter {

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId.orEmpty())
    }
}
