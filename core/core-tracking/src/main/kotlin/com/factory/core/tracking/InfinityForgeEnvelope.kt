package com.factory.core.tracking

/**
 * InfinityForge Tracking — canonical wire types.
 *
 * Mirrors infinityforge-tracking-module's schema/common-types.yaml,
 * schema/event-envelope.yaml, and schema/metric-envelope.yaml field-for-field. This
 * file defines only shape — no business logic, no vendor concepts. See
 * `core-analytics`'s `AnalyticsTracker`/`AnalyticsEvent` for this factory's separate,
 * older, closed-taxonomy analytics capability (a different concern entirely — see
 * Docs/INFINITYFORGE_TRACKING.md for why these two are not merged).
 */

/** A JSON-shaped event property value. Deliberately excludes `null` —
 * specification/conventions.md: an inapplicable optional property is omitted, never
 * sent as null. */
sealed interface InfinityForgePropertyValue {
    data class StringValue(val value: String) : InfinityForgePropertyValue
    data class IntValue(val value: Int) : InfinityForgePropertyValue
    data class NumberValue(val value: Double) : InfinityForgePropertyValue
    data class BooleanValue(val value: Boolean) : InfinityForgePropertyValue
    data class ArrayValue(val value: List<InfinityForgePropertyValue>) : InfinityForgePropertyValue
    data class ObjectValue(val value: Map<String, InfinityForgePropertyValue>) : InfinityForgePropertyValue
}

/** A metric dimension value — deliberately narrower than a property value
 * (schema/metric-dimensions.yaml: string/integer/boolean/enum only, no object/array/
 * floating-point number). An `enum` dimension's allowed values are still represented
 * as [StringValue] at this type level; the closed set itself is enforced by
 * validation, not by this type. */
sealed interface InfinityForgeDimensionValue {
    data class StringValue(val value: String) : InfinityForgeDimensionValue
    data class IntValue(val value: Int) : InfinityForgeDimensionValue
    data class BooleanValue(val value: Boolean) : InfinityForgeDimensionValue
}

/** schema/common-types.yaml `environment` enum. Exactly these three values —
 * specification/metadata.md is explicit that a fourth value must never be added
 * without a CONTRIBUTING.md review of the contract itself. */
enum class InfinityForgeEnvironment(val wireValue: String) {
    DEVELOPMENT("development"),
    PREVIEW("preview"),
    PRODUCTION("production"),
}

/** schema/common-types.yaml `platform` enum — the device's OS, not this adapter's own
 * identity (see [InfinityForgeMetadata.SDK_NAME]). */
enum class InfinityForgePlatform(val wireValue: String) {
    IOS("ios"),
    ANDROID("android"),
    WEB("web"),
    OTHER("other"),
}

/** schema/common-types.yaml `metric_unit` enum. Fixed per metric definition — never
 * chosen per call for a canonical metric. */
enum class InfinityForgeMetricUnit(val wireValue: String) {
    CURRENCY("currency"),
    COUNT("count"),
    IMPRESSION("impression"),
    MILLISECOND("millisecond"),
    SECOND("second"),
    OTHER("other"),
}

/** schema/common-types.yaml `metric_source` enum. A general category, never a specific
 * vendor name (specification/metric-envelope.md). */
enum class InfinityForgeMetricSource(val wireValue: String) {
    APPLICATION("application"),
    BILLING_SYSTEM("billing_system"),
    ADVERTISING_SYSTEM("advertising_system"),
    OPERATING_SYSTEM("operating_system"),
    NETWORK("network"),
    PROVIDER("provider"),
    OTHER("other"),
}

/** The canonical event envelope — schema/event-envelope.yaml, field-for-field.
 * `required` there: event, schemaVersion, timestamp, appId, environment, platform,
 * sdkVersion, appVersion, anonymousId. `sdkName`/`userId` are optional; `properties`
 * defaults to empty. */
data class InfinityForgeEventEnvelope(
    val event: String,
    val schemaVersion: Int,
    val timestamp: String,
    val appId: String,
    val environment: InfinityForgeEnvironment,
    val platform: InfinityForgePlatform,
    val sdkVersion: String,
    val sdkName: String?,
    val appVersion: String,
    val userId: String?,
    val anonymousId: String,
    val properties: Map<String, InfinityForgePropertyValue>,
)

/** The canonical metric envelope — schema/metric-envelope.yaml, field-for-field.
 * Reuses the exact same identity/metadata fields and meaning as the event envelope
 * (specification/metric-envelope.md); `currency` is required iff `unit == CURRENCY`,
 * forbidden otherwise — enforced by validation, not by this type, matching the
 * reference Swift/RN implementations' own approach. */
data class InfinityForgeMetricEnvelope(
    val metricName: String,
    val schemaVersion: Int,
    val value: Double,
    val unit: InfinityForgeMetricUnit,
    val currency: String?,
    val source: InfinityForgeMetricSource,
    val referenceId: String?,
    val timestamp: String,
    val appId: String,
    val environment: InfinityForgeEnvironment,
    val platform: InfinityForgePlatform,
    val sdkVersion: String,
    val sdkName: String?,
    val appVersion: String,
    val userId: String?,
    val anonymousId: String,
    val dimensions: Map<String, InfinityForgeDimensionValue>,
)

/** Envelope field names a property/dimension key must never collide with —
 * specification/conventions.md and specification/metric-envelope.md. */
object InfinityForgeReservedFields {
    val event: Set<String> = setOf(
        "event",
        "schema_version",
        "timestamp",
        "app_id",
        "environment",
        "platform",
        "sdk_version",
        "sdk_name",
        "app_version",
        "user_id",
        "anonymous_id",
        "properties",
    )

    val metric: Set<String> = setOf(
        "metric_name",
        "schema_version",
        "value",
        "unit",
        "currency",
        "source",
        "reference_id",
        "timestamp",
        "app_id",
        "environment",
        "platform",
        "sdk_version",
        "sdk_name",
        "app_version",
        "user_id",
        "anonymous_id",
        "dimensions",
    )
}
