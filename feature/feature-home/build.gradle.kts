plugins {
    alias(libs.plugins.factory.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.factory.feature.home"
}

dependencies {
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))
    implementation(project(":ads:ads-api"))

    implementation(libs.retrofit.core)
    implementation(libs.kotlinx.serialization.json)
}
