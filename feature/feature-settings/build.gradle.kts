plugins {
    alias(libs.plugins.factory.android.feature)
}

android {
    namespace = "com.factory.feature.settings"
}

dependencies {
    implementation(project(":core:core-datastore"))
    implementation(project(":purchases:purchases-api"))
}
