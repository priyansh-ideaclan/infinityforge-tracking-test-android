package com.factory.core.tracking

/**
 * Product-category canonical event factories for [InfinityForgeEvent.Companion] —
 * split into this file only to keep that companion object under Detekt's per-object
 * function-count ceiling (complexity.TooManyFunctions); see [InfinityForgeEvent]'s own
 * doc comment. The function below is a Kotlin extension function on the companion
 * object, so it resolves exactly as `InfinityForgeEvent.featureUsed(...)`, identical
 * to a member.
 */
fun InfinityForgeEvent.Companion.featureUsed(featureName: String, featureCategory: String? = null): InfinityForgeEvent {
    val properties = buildMap {
        put("feature_name", InfinityForgePropertyValue.StringValue(featureName))
        featureCategory?.let { put("feature_category", InfinityForgePropertyValue.StringValue(it)) }
    }
    return InfinityForgeEvent("feature_used", 1, properties, isCanonical = true)
}
