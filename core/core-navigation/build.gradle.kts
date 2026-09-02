plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.factory.core.navigation"
}

dependencies {
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
