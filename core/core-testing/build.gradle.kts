plugins {
    alias(libs.plugins.factory.android.library)
}

android {
    namespace = "com.factory.core.testing"
}

dependencies {
    api(project(":core:core-common"))
    api(project(":core:core-analytics"))
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.mockk)
    api(libs.turbine)
    api(libs.androidx.test.ext.junit)
    api(libs.androidx.test.espresso.core)
}
