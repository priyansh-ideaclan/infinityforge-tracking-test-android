package com.factory.core.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject

/**
 * Real implementation, only ever constructed by [AnalyticsModule] when analytics is
 * both enabled in `APP_SPEC.yaml` and a `google-services.json` is present — see
 * `Docs/setup/firebase.md` for what "present" requires.
 */
class FirebaseAnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.name) {
            event.properties.forEach { (key, value) -> param(key, value) }
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}
