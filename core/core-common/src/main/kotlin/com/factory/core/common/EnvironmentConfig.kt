package com.factory.core.common

/**
 * The app-level facts that vary per build environment (dev/staging/prod), sourced from
 * `BuildConfig` at the `app` module's Hilt composition root (see `AppModule`) and
 * injected everywhere else — modules below `app` never read `BuildConfig` directly, so
 * they stay buildable/testable independent of any specific flavor.
 */
data class EnvironmentConfig(
    val name: String,
    val baseUrl: String,
    val isDebug: Boolean,
    val googleWebClientId: String,
)
