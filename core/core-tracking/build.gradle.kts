plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.factory.core.tracking"
}

dependencies {
    // InfinityForge Tracking Contract implementation (infinityforge-tracking-module).
    // See Docs/modules/README.md and Docs/INFINITYFORGE_TRACKING.md for the full
    // boundary this module owns.
    implementation(project(":core:core-common"))
    implementation(project(":core:core-logging"))
    implementation(project(":core:core-datastore"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Firebase provider (specification's "provider" concept — this contract itself
    // names no vendor). Compiles unconditionally, exactly like core-analytics's own
    // Firebase dependency; inert at runtime until a real google-services.json exists
    // (see FirebaseAvailability in this module and Docs/setup/firebase.md).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    testImplementation(project(":core:core-testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
