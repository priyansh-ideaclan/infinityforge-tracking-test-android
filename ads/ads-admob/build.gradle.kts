plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.compose)
    alias(libs.plugins.factory.hilt)
}

android {
    namespace = "com.factory.ads.admob"
}

dependencies {
    implementation(project(":ads:ads-api"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-logging"))

    implementation(libs.google.mobile.ads)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:core-testing"))
}
