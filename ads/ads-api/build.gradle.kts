plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.compose)
}

android {
    namespace = "com.factory.ads.api"
}

dependencies {
    implementation(project(":core:core-common"))
}
