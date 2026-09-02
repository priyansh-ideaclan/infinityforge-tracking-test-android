plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.hilt)
}

android {
    namespace = "com.factory.purchases.revenuecat"
}

dependencies {
    implementation(project(":purchases:purchases-api"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-logging"))

    implementation(libs.revenuecat.purchases)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:core-testing"))
}
