package com.factory.core.common

/**
 * Minimal on/off capability check derived from `APP_SPEC.yaml` at configure-time
 * (`scripts/configure_app.py` writes the resolved flags into `AppModule`'s
 * `BuildConfigFeatureFlagProvider`, not into this file). Kept as an interface so tests
 * and `core-testing`'s `FakeFeatureFlagProvider` can force any combination — including
 * ones the current `APP_SPEC.yaml` doesn't represent — for the module-combination tests
 * described in Docs/testing/module-matrix.md.
 */
interface FeatureFlagProvider {
    fun isEnabled(flag: FeatureFlag): Boolean
}

enum class FeatureFlag {
    AUTH,
    AUTH_EMAIL_PASSWORD,
    AUTH_GOOGLE,
    AUTH_ANONYMOUS,
    ONBOARDING,
    ADS,
    PURCHASES,
    ANALYTICS,
    CRASH_REPORTING,
}
