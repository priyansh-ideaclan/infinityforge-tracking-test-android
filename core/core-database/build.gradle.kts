plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.factory.core.database"

    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.generateKotlin", "true")
        }
    }
}

dependencies {
    implementation(project(":core:core-common"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
