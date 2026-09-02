plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.hilt)
}

android {
    namespace = "com.factory.core.analytics"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-logging"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
