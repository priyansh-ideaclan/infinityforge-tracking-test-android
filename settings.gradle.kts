@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "native-android-app-factory"

include(":app")

// --- Always-on core modules ---
include(":core:core-designsystem")
include(":core:core-navigation")
include(":core:core-network")
include(":core:core-database")
include(":core:core-datastore")
include(":core:core-logging")
include(":core:core-analytics")
include(":core:core-common")
include(":core:core-testing")

// --- Always-on feature modules (demonstrate the core stack end-to-end) ---
include(":feature:feature-home")
include(":feature:feature-settings")

// --- Optional-capability modules ---
// Always included, unconditionally — `app/build.gradle.kts` always depends on all of
// them too. Which capability is actually *active* at runtime is controlled entirely by
// `AppSpecFlags.kt` (see `scripts/configure_app.py`), which selects the real
// (FirebaseAuthRepository/AdMobAdsController/RevenueCatPurchasesController) or fake
// implementation via each module's Hilt `@Provides` function. An earlier design tried
// to *also* exclude unused modules here at the Gradle level; that required this file
// and app/build.gradle.kts's dependencies to be toggled in lockstep, and a first real
// run of configure_app.py proved that fragile (toggling one without the other breaks
// the build). Static inclusion + runtime selection is simpler and was kept instead.
include(":feature:feature-auth")
include(":feature:feature-onboarding")
include(":ads:ads-api")
include(":ads:ads-admob")
include(":purchases:purchases-api")
include(":purchases:purchases-revenuecat")
