plugins {
    alias(libs.plugins.factory.android.library)
}

android {
    namespace = "com.factory.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
