plugins {
    alias(libs.plugins.factory.android.library)
}

android {
    namespace = "com.factory.purchases.api"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.android)
}
