plugins {
    `kotlin-dsl`
}

group = "com.factory.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.kotlin.serialization.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "factory.android.application"
            implementationClass = "com.factory.buildlogic.FactoryAndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "factory.android.library"
            implementationClass = "com.factory.buildlogic.FactoryAndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "factory.android.feature"
            implementationClass = "com.factory.buildlogic.FactoryAndroidFeatureConventionPlugin"
        }
        register("compose") {
            id = "factory.compose"
            implementationClass = "com.factory.buildlogic.FactoryComposeConventionPlugin"
        }
        register("hilt") {
            id = "factory.hilt"
            implementationClass = "com.factory.buildlogic.FactoryHiltConventionPlugin"
        }
        register("lint") {
            id = "factory.lint"
            implementationClass = "com.factory.buildlogic.FactoryLintConventionPlugin"
        }
    }
}
