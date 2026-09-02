package com.factory.core.analytics

/**
 * Every analytics event the factory emits, typed so a call site can't typo an event
 * name or forget a required property. See `Docs/analytics/events.md` for the name,
 * properties, trigger, and business meaning of each one — that file and this sealed
 * class must be kept in sync (verified by `scripts/verify_project.py`).
 */
sealed class AnalyticsEvent(val name: String, val properties: Map<String, String> = emptyMap()) {

    data object AppOpened : AnalyticsEvent("app_opened")

    data class ScreenViewed(val screenName: String) :
        AnalyticsEvent("app_screen_viewed", mapOf("screen_name" to screenName))

    data object OnboardingCompleted : AnalyticsEvent("app_onboarding_completed")

    data class AuthLoginSucceeded(val method: String) :
        AnalyticsEvent("auth_login_succeeded", mapOf("method" to method))

    data class AuthLoginFailed(val method: String, val reason: String) :
        AnalyticsEvent("auth_login_failed", mapOf("method" to method, "reason" to reason))

    data class AuthRegistrationSucceeded(val method: String) :
        AnalyticsEvent("auth_registration_succeeded", mapOf("method" to method))

    data object AuthLogoutSucceeded : AnalyticsEvent("auth_logout_succeeded")

    data class PurchaseStarted(val productId: String) :
        AnalyticsEvent("purchase_started", mapOf("product_id" to productId))

    data class PurchaseCompleted(val productId: String) :
        AnalyticsEvent("purchase_completed", mapOf("product_id" to productId))

    data class PurchaseFailed(val productId: String, val reason: String) :
        AnalyticsEvent("purchase_failed", mapOf("product_id" to productId, "reason" to reason))

    data object PurchaseRestored : AnalyticsEvent("purchase_restored")

    data class AdImpression(val placement: String, val format: String) :
        AnalyticsEvent("ad_impression", mapOf("placement" to placement, "format" to format))

    data class AdClicked(val placement: String, val format: String) :
        AnalyticsEvent("ad_clicked", mapOf("placement" to placement, "format" to format))

    data class AdFailedToLoad(val placement: String, val format: String, val reason: String) :
        AnalyticsEvent(
            "ad_failed_to_load",
            mapOf("placement" to placement, "format" to format, "reason" to reason),
        )

    data class SettingsThemeChanged(val mode: String) :
        AnalyticsEvent("settings_theme_changed", mapOf("mode" to mode))
}
