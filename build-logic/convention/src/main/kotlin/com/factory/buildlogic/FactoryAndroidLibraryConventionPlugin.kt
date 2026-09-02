package com.factory.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** Applied by every `core-*`, `ads-*`, and `purchases-*` Android library module. */
class FactoryAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // See FactoryAndroidApplicationConventionPlugin for why org.jetbrains.kotlin.android
            // is not applied here under AGP 9's built-in Kotlin support.
            apply(plugin = "com.android.library")
            apply(plugin = "factory.lint")

            extensions.configure(LibraryExtension::class.java) {
                configureAndroidCommon(this)
            }

            dependencies {
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("javax-inject").get())
                add("testImplementation", libs.findLibrary("junit4").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                add("testImplementation", libs.findLibrary("mockk").get())
                add("testImplementation", libs.findLibrary("turbine").get())
            }
        }
    }
}
