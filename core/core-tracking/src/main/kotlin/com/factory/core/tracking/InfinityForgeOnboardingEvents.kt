package com.factory.core.tracking

/**
 * Onboarding-category canonical event factories for [InfinityForgeEvent.Companion] —
 * split into this file only to keep that companion object under Detekt's per-object
 * function-count ceiling (complexity.TooManyFunctions); see [InfinityForgeEvent]'s own
 * doc comment. Every function below is a Kotlin extension function on the companion
 * object, so it resolves exactly as `InfinityForgeEvent.onboardingStarted(...)` etc.,
 * identical to a member.
 */
fun InfinityForgeEvent.Companion.onboardingStarted() =
    InfinityForgeEvent("onboarding_started", 1, emptyMap(), isCanonical = true)

fun InfinityForgeEvent.Companion.onboardingCompleted(stepCount: Int? = null): InfinityForgeEvent {
    val properties = buildMap {
        stepCount?.let { put("step_count", InfinityForgePropertyValue.IntValue(it)) }
    }
    return InfinityForgeEvent("onboarding_completed", 1, properties, isCanonical = true)
}
