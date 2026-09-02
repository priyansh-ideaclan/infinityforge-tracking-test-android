package com.factory.buildlogic

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Applied by every Android application/library convention plugin. Configures Detekt and
 * ktlint once, consistently, so no module can silently opt out or drift to a different
 * ruleset/version. `./scripts/verify.sh` runs the aggregated `detekt`/`ktlintCheck` tasks
 * this produces at the root.
 */
class FactoryLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "io.gitlab.arturbosch.detekt")
            apply(plugin = "org.jlleitschuh.gradle.ktlint")

            extensions.configure(DetektExtension::class.java) {
                buildUponDefaultConfig = true
                allRules = false
                config.setFrom(rootProject.files("config/detekt/detekt.yml"))
                parallel = true
            }

            extensions.configure(KtlintExtension::class.java) {
                android.set(true)
                verbose.set(true)
                outputToConsole.set(true)
                ignoreFailures.set(false)
            }

            dependencies {
                add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:${libs.findVersion("detekt").get()}")
            }
        }
    }
}
