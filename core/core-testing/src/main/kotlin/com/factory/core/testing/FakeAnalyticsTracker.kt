package com.factory.core.testing

import com.factory.core.analytics.AnalyticsEvent
import com.factory.core.analytics.AnalyticsTracker

/** Records every tracked event/user property in memory so tests can assert on them. */
class FakeAnalyticsTracker : AnalyticsTracker {
    val trackedEvents = mutableListOf<AnalyticsEvent>()
    val userProperties = mutableMapOf<String, String?>()

    override fun track(event: AnalyticsEvent) {
        trackedEvents += event
    }

    override fun setUserProperty(name: String, value: String?) {
        userProperties[name] = value
    }
}
