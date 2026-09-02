package com.factory.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Shared Android configuration applied to both application and library modules:
 * compile/min SDK, JDK 17 toolchain, core library desugaring, and lint defaults.
 * Kept as one function so `minSdk`/`compileSdk`/JVM target can never drift between the
 * application and library convention plugins.
 */
internal fun Project.configureAndroidCommon(
    extension: CommonExtension,
) {
    // AGP 9's `com.android.build.api.dsl` interfaces expose these as plain mutable
    // properties (no `defaultConfig { }`/`compileOptions { }`/`lint { }` block-style
    // configuration methods), so every nested block is set via direct property access.
    // compileSdk is 37, not 36: Compose BOM 2026.08.00's artifacts require compiling
    // against API 37+ (found via a real `assembleDebug` failure listing every
    // Compose/Lifecycle artifact requiring it). targetSdk stays 36 to match Google
    // Play's actual current requirement — compileSdk and targetSdk are independent
    // (see D-002 in the plan).
    extension.compileSdk = 37
    extension.defaultConfig.minSdk = 26

    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.isCoreLibraryDesugaringEnabled = true

    extension.lint.abortOnError = true
    extension.lint.checkDependencies = true
    extension.lint.xmlReport = true
    extension.lint.htmlReport = true

    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("android-desugar-jdk-libs").get())
    }
}
