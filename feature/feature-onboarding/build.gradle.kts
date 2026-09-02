plugins {
    alias(libs.plugins.factory.android.feature)
}

android {
    namespace = "com.factory.feature.onboarding"
}

dependencies {
    implementation(project(":core:core-datastore"))
}
