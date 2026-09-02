plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.factory.core.network"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-logging"))

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:core-testing"))
}
