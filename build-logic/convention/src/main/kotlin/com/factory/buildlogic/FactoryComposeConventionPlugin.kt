package com.factory.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Applied by any module (feature modules and `core-designsystem`) that uses Jetpack
 * Compose. Applies the Compose Compiler Kotlin plugin, turns on the `compose`
 * build feature, and adds the Compose BOM plus the common UI/Material3/tooling/test
 * dependencies so individual modules don't repeat this boilerplate.
 */
class FactoryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            extensions.configure(CommonExtension::class.java) {
                buildFeatures.compose = true
            }

            val bom = libs.findLibrary("compose-bom").get()
            dependencies {
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))

                add("implementation", libs.findLibrary("compose-ui").get())
                add("implementation", libs.findLibrary("compose-ui-graphics").get())
                add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
                add("implementation", libs.findLibrary("compose-material3").get())
                add("implementation", libs.findLibrary("compose-material-icons-extended").get())
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())

                add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
                add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())

                add("androidTestImplementation", libs.findLibrary("compose-ui-test-junit4").get())
                add("androidTestImplementation", libs.findLibrary("androidx-test-ext-junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-test-espresso-core").get())
            }
        }
    }
}
