package com.factory.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Bundle convention for feature modules under `feature/`: Android library + Compose +
 * Hilt, plus the
 * always-needed core module dependencies (navigation, design system, common, logging,
 * analytics abstraction) and the test-only fakes module. Keeps every feature module's
 * `build.gradle.kts` to "apply this plugin, add feature-specific dependencies."
 */
class FactoryAndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "factory.android.library")
            apply(plugin = "factory.compose")
            apply(plugin = "factory.hilt")

            dependencies {
                add("implementation", project(":core:core-designsystem"))
                add("implementation", project(":core:core-navigation"))
                add("implementation", project(":core:core-common"))
                add("implementation", project(":core:core-logging"))
                add("implementation", project(":core:core-analytics"))
                add("testImplementation", project(":core:core-testing"))
                add("androidTestImplementation", project(":core:core-testing"))

                add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
                add("implementation", libs.findLibrary("navigation-compose").get())
            }
        }
    }
}
