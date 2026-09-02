package com.factory.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Applied only by `app/build.gradle.kts`. Configures the single application module:
 * Android Application + Kotlin Android plugins, shared compile/min SDK (see
 * [configureAndroidCommon]), and factory-wide static analysis (factory.lint).
 */
class FactoryAndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9+ bundles Kotlin compilation itself; applying
            // org.jetbrains.kotlin.android alongside it is now rejected outright
            // (see https://kotl.in/gradle/agp-built-in-kotlin). JVM target/toolchain is
            // set via the top-level `kotlin {}` extension in configureAndroidCommon.
            apply(plugin = "com.android.application")
            apply(plugin = "factory.lint")

            extensions.configure(ApplicationExtension::class.java) {
                configureAndroidCommon(this)
                defaultConfig.targetSdk = 36
                buildFeatures.buildConfig = true
            }

            dependencies {
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
            }
        }
    }
}
