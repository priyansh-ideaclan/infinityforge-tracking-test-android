package com.factory.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes (kotlinx.serialization-based, stable since
 * Navigation 2.8). Every screen's route lives here, not scattered across feature
 * modules as string literals, so the whole nav graph is discoverable from one file and
 * feature modules share a single source of truth for how to navigate *to* another
 * feature without depending on that feature's implementation.
 */
sealed interface FactoryDestination {

    @Serializable
    data object Home : FactoryDestination

    @Serializable
    data object Settings : FactoryDestination

    @Serializable
    data object Onboarding : FactoryDestination

    @Serializable
    data object Login : FactoryDestination

    @Serializable
    data object Register : FactoryDestination

    @Serializable
    data object ForgotPassword : FactoryDestination

    // Test-app-only addition (infinityforge-tracking-test-android), not part of
    // native-android-app-factory's production destination set — exists solely to
    // exercise InfinityForge automatic screen tracking
    // (core-navigation/InfinityForgeScreenTracking.kt) against a third, distinct
    // screen beyond Home/Settings, proving the resolver needs no per-destination
    // update: adding this one line is the only change required for "profile" to
    // be automatically tracked.
    @Serializable
    data object Profile : FactoryDestination
}
