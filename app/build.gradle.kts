plugins {
    alias(libs.plugins.factory.android.application)
    alias(libs.plugins.factory.compose)
    alias(libs.plugins.factory.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The Google Services / Firebase Crashlytics Gradle plugins fail the build outright if
// no google-services.json is present for a processed flavor. This factory must never
// invent that file (see AGENTS.md §7), so applying these plugins is conditional on a
// real config file actually existing. Without one, Firebase-backed dependencies still
// compile (see FirebaseAuthRepository etc.) but no-op at runtime with a logged warning —
// see Docs/setup/firebase.md for the exact external step this replaces.
val googleServicesConfigured =
    fileTree(projectDir) {
        include("google-services.json", "src/*/google-services.json")
    }.files.isNotEmpty()

if (googleServicesConfigured) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
} else {
    logger.warn(
        "[factory-setup-required] No google-services.json found under app/ or any " +
            "app/src/<flavor>/. Skipping the Google Services and Firebase Crashlytics " +
            "Gradle plugins. Firebase Analytics/Crashlytics/Auth will remain inert at " +
            "runtime until this is added. See Docs/setup/firebase.md.",
    )
}

android {
    namespace = "com.ideaclan.infinityforgetrackingtestkotlin"

    defaultConfig {
        applicationId = "com.ideaclan.infinityforgetrackingtestkotlin"
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // External configuration, never invented: the real OAuth web client ID for
        // Google Sign-In comes from the Firebase console (see Docs/setup/firebase.md).
        // Empty by default; FirebaseAuthRepository treats blank as "Google sign-in not
        // configured" and reports AppError.Auth.NotConfigured rather than crashing.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${project.findProperty("FACTORY_GOOGLE_WEB_CLIENT_ID") ?: ""}\"",
        )

        // Same external-configuration pattern as GOOGLE_WEB_CLIENT_ID above: never
        // invented, blank by default. See Docs/setup/revenuecat.md.
        buildConfigField(
            "String",
            "REVENUECAT_API_KEY",
            "\"${project.findProperty("FACTORY_REVENUECAT_API_KEY") ?: ""}\"",
        )
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"https://dev.api.example.com/\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"dev\"")
        }
        create("staging") {
            dimension = "env"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "BASE_URL", "\"https://staging.api.example.com/\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"staging\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://api.example.com/\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"prod\"")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // No signingConfig is declared here on purpose: this factory never
            // generates or embeds a production signing key. See Docs/release/signing.md.
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-navigation"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-datastore"))
    implementation(project(":core:core-logging"))
    implementation(project(":core:core-analytics"))
    implementation(project(":core:core-tracking"))
    implementation(project(":core:core-common"))

    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-auth"))
    implementation(project(":feature:feature-onboarding"))

    implementation(project(":ads:ads-api"))
    implementation(project(":ads:ads-admob"))
    implementation(project(":purchases:purchases-api"))
    implementation(project(":purchases:purchases-revenuecat"))

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.navigation.compose)

    // FactoryApplication calls MobileAds.initialize(...) and Purchases.configure(...)
    // directly (the one and only place either SDK is initialized) — see AGENTS.md §6/§7.
    implementation(libs.google.mobile.ads)
    implementation(libs.revenuecat.purchases)

    testImplementation(libs.junit4)
    testImplementation(project(":core:core-testing"))
    androidTestImplementation(project(":core:core-testing"))
}
