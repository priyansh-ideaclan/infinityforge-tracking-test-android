package com.factory.core.tracking

/** Dimension-only enum values — mirrors metrics/ *.yaml's `allowed_values` lists
 * exactly. [InfinityForgeLaunchType] (app_launch_duration) is shared with the event
 * catalog's identical `launch_type` semantics. */
enum class InfinityForgeTransactionType(val wireValue: String) {
    CHARGE("charge"), REFUND("refund"), ADJUSTMENT("adjustment")
}

enum class InfinityForgeBillingType(val wireValue: String) {
    SUBSCRIPTION("subscription"), ONE_TIME("one_time"), OTHER("other")
}

enum class InfinityForgeAdFormat(val wireValue: String) {
    BANNER("banner"), INTERSTITIAL("interstitial"), REWARDED("rewarded"), NATIVE("native"), OTHER("other")
}

enum class InfinityForgeAdRevenuePrecision(val wireValue: String) { EXACT("exact"), ESTIMATED("estimated") }

enum class InfinityForgeOperationCategory(val wireValue: String) {
    API("api"), NETWORK("network"), STORAGE("storage"), OTHER("other")
}

enum class InfinityForgeOperationOutcome(val wireValue: String) { SUCCESS("success"), FAILURE("failure") }

enum class InfinityForgeHandledErrorCategory(val wireValue: String) {
    RECOVERABLE_ERROR("recoverable_error"), OPERATIONAL_FAILURE("operational_failure")
}

/** Canonical metric names and their contract-defined shape, used by
 * [InfinityForgeMetricValidation] — mirrors [InfinityForgeEventCatalog]'s role for
 * events. */
object InfinityForgeMetricCatalog {
    sealed interface Kind {
        data object StringKind : Kind
        data object IntKind : Kind
        data object BooleanKind : Kind
        data class EnumKind(val allowedValues: Set<String>) : Kind
    }

    data class DimensionSpec(val kind: Kind, val required: Boolean)

    val schemaVersions: Map<String, Int> = mapOf(
        "revenue" to 1,
        "ad_impression" to 1,
        "ad_revenue" to 1,
        "session_duration" to 1,
        "app_launch_duration" to 1,
        "screen_load_duration" to 1,
        "operation_duration" to 1,
        "handled_error" to 1,
    )

    val units: Map<String, InfinityForgeMetricUnit> = mapOf(
        "revenue" to InfinityForgeMetricUnit.CURRENCY,
        "ad_impression" to InfinityForgeMetricUnit.IMPRESSION,
        "ad_revenue" to InfinityForgeMetricUnit.CURRENCY,
        "session_duration" to InfinityForgeMetricUnit.SECOND,
        "app_launch_duration" to InfinityForgeMetricUnit.MILLISECOND,
        "screen_load_duration" to InfinityForgeMetricUnit.MILLISECOND,
        "operation_duration" to InfinityForgeMetricUnit.MILLISECOND,
        "handled_error" to InfinityForgeMetricUnit.COUNT,
    )

    val typicalSources: Map<String, InfinityForgeMetricSource> = mapOf(
        "revenue" to InfinityForgeMetricSource.BILLING_SYSTEM,
        "ad_impression" to InfinityForgeMetricSource.ADVERTISING_SYSTEM,
        "ad_revenue" to InfinityForgeMetricSource.ADVERTISING_SYSTEM,
        "session_duration" to InfinityForgeMetricSource.APPLICATION,
        "app_launch_duration" to InfinityForgeMetricSource.APPLICATION,
        "screen_load_duration" to InfinityForgeMetricSource.APPLICATION,
        "operation_duration" to InfinityForgeMetricSource.APPLICATION,
        "handled_error" to InfinityForgeMetricSource.APPLICATION,
    )

    /** Machine-readable `fixed_value` fields mirrored from the contract. */
    val fixedValues: Map<String, Double> = mapOf("ad_impression" to 1.0, "handled_error" to 1.0)

    val dimensions: Map<String, Map<String, DimensionSpec>> = mapOf(
        "revenue" to mapOf(
            "transaction_type" to DimensionSpec(
                Kind.EnumKind(setOf("charge", "refund", "adjustment")),
                required = true,
            ),
            "billing_type" to DimensionSpec(
                Kind.EnumKind(setOf("subscription", "one_time", "other")),
                required = false,
            ),
            "product_id" to DimensionSpec(Kind.StringKind, required = false),
        ),
        "ad_impression" to mapOf(
            "placement" to DimensionSpec(Kind.StringKind, required = false),
            "ad_format" to DimensionSpec(
                Kind.EnumKind(setOf("banner", "interstitial", "rewarded", "native", "other")),
                required = false,
            ),
            "network" to DimensionSpec(Kind.StringKind, required = false),
        ),
        "ad_revenue" to mapOf(
            "placement" to DimensionSpec(Kind.StringKind, required = false),
            "ad_format" to DimensionSpec(
                Kind.EnumKind(setOf("banner", "interstitial", "rewarded", "native", "other")),
                required = false,
            ),
            "network" to DimensionSpec(Kind.StringKind, required = false),
            "precision" to DimensionSpec(Kind.EnumKind(setOf("exact", "estimated")), required = false),
        ),
        "session_duration" to emptyMap(),
        "app_launch_duration" to mapOf(
            "launch_type" to DimensionSpec(Kind.EnumKind(setOf("cold", "warm")), required = false),
        ),
        "screen_load_duration" to mapOf(
            "screen_name" to DimensionSpec(Kind.StringKind, required = true),
        ),
        "operation_duration" to mapOf(
            "operation" to DimensionSpec(Kind.StringKind, required = true),
            "operation_category" to DimensionSpec(
                Kind.EnumKind(setOf("api", "network", "storage", "other")),
                required = false,
            ),
            "outcome" to DimensionSpec(Kind.EnumKind(setOf("success", "failure")), required = false),
        ),
        "handled_error" to mapOf(
            "category" to DimensionSpec(
                Kind.EnumKind(setOf("recoverable_error", "operational_failure")),
                required = true,
            ),
            "error_code" to DimensionSpec(Kind.StringKind, required = false),
        ),
    )

    fun isCanonical(name: String): Boolean = schemaVersions.containsKey(name)
}
