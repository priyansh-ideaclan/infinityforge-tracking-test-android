package com.factory.core.analytics

/**
 * The only analytics entry point feature code uses — never `FirebaseAnalytics`
 * directly (see AGENTS.md §11). Swapping providers, or turning analytics off entirely
 * per `APP_SPEC.yaml`, only ever changes which implementation `AnalyticsModule` binds.
 */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
    fun setUserProperty(name: String, value: String?)
}

/** Used when `APP_SPEC.yaml`'s `analytics.enabled` is false, and in tests/CI. */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
}
