plugins {
    alias(libs.plugins.factory.android.feature)
}

android {
    namespace = "com.factory.feature.auth"
}

dependencies {
    implementation(libs.firebase.auth)
    implementation(platform(libs.firebase.bom))
    implementation(libs.play.services.auth)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
