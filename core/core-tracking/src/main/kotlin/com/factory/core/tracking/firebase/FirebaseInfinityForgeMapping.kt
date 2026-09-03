package com.factory.core.tracking.firebase

import com.factory.core.tracking.InfinityForgeDimensionValue
import com.factory.core.tracking.InfinityForgeMetricEnvelope
import com.factory.core.tracking.InfinityForgeMetricUnit
import com.factory.core.tracking.InfinityForgePropertyValue

/**
 * Pure translation from infinityforge-tracking-module's envelopes
 * (schema/event-envelope.yaml, schema/metric-envelope.yaml) to Firebase Analytics'
 * own SDK shapes. Deliberately has zero dependency on `com.google.firebase.analytics`
 * — every function here is a plain, synchronous function of its inputs, mirroring the
 * reference Swift implementation's `FirebaseInfinityForgeMapping` and the reference RN
 * implementation's `firebase-mapping.ts` (including their own reasoning for this
 * separation: Firebase's data model — naming rules, reserved prefixes, length/count
 * ceilings — is not this contract's envelope, and this is the one place that decides,
 * testably, what crosses that boundary and how).
 */
object FirebaseInfinityForgeMapping {
    // Firebase Analytics' own documented limits, as of 2026 —
    // https://support.google.com/analytics/answer/9267744 (event/parameter limits) and
    // https://support.google.com/analytics/answer/13316687 (reserved prefixes/names).
    const val EVENT_NAME_MAX_LENGTH = 40
    const val PARAM_NAME_MAX_LENGTH = 40
    const val PARAM_VALUE_MAX_LENGTH = 100
    const val MAX_EVENT_PARAMS = 25
    const val USER_PROPERTY_NAME_MAX_LENGTH = 24
    const val USER_PROPERTY_VALUE_MAX_LENGTH = 36
    const val MAX_USER_PROPERTIES = 25
    const val USER_ID_MAX_LENGTH = 256

    private val reservedPrefixes = listOf("firebase_", "google_", "ga_", "gtag.", "_")

    fun hasReservedPrefix(name: String): Boolean = reservedPrefixes.any { name.startsWith(it) }

    /** Disambiguates a name colliding with a Firebase-reserved prefix by adding a
     * distinguishing prefix, rather than dropping it — this contract's own naming
     * rules already guarantee every name starts with a lowercase letter, so a
     * collision here is a defensive fallback, not a normal path. */
    fun disambiguate(name: String): String = if (hasReservedPrefix(name)) "app_$name" else name

    fun truncate(value: String, maxLength: Int): String = if (value.length > maxLength) value.take(maxLength) else value

    /** Firebase event/screen parameters only accept string or number values — no
     * native boolean, object, or array parameter type. A Firebase-specific
     * limitation, not a contract one: the contract itself
     * (specification/conventions.md) is unambiguous that true/false are the only
     * valid boolean representation *in the emitted InfinityForge event*; this
     * function only decides what crosses onward to this one vendor's API. */
    fun toFirebaseParamValue(value: InfinityForgePropertyValue): Any = when (value) {
        is InfinityForgePropertyValue.StringValue -> truncate(value.value, PARAM_VALUE_MAX_LENGTH)
        is InfinityForgePropertyValue.IntValue -> value.value
        is InfinityForgePropertyValue.NumberValue ->
            if (value.value.isFinite()) value.value else truncate(value.value.toString(), PARAM_VALUE_MAX_LENGTH)
        is InfinityForgePropertyValue.BooleanValue -> if (value.value) "true" else "false"
        is InfinityForgePropertyValue.ArrayValue, is InfinityForgePropertyValue.ObjectValue ->
            truncate(jsonEncode(value), PARAM_VALUE_MAX_LENGTH)
    }

    private fun jsonEncode(value: InfinityForgePropertyValue): String = when (value) {
        is InfinityForgePropertyValue.StringValue -> "\"${escapeJson(value.value)}\""
        is InfinityForgePropertyValue.IntValue -> value.value.toString()
        is InfinityForgePropertyValue.NumberValue -> value.value.toString()
        is InfinityForgePropertyValue.BooleanValue -> if (value.value) "true" else "false"
        is InfinityForgePropertyValue.ArrayValue -> "[" + value.value.joinToString(",") { jsonEncode(it) } + "]"
        is InfinityForgePropertyValue.ObjectValue ->
            "{" + value.value.entries.sortedBy { it.key }
                .joinToString(",") { "\"${escapeJson(it.key)}\":${jsonEncode(it.value)}" } + "}"
    }

    private fun escapeJson(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    data class MappedEvent(val name: String, val params: Map<String, Any>, val dropped: List<String>)

    data class MappedScreenView(val screenName: String, val screenClass: String)

    // -- Metrics -> Firebase Analytics mapping --
    //
    // Firebase/GA4 has no dedicated "metric" concept, and none of this contract's 8
    // canonical metrics map onto one of GA4's reserved recommended events without
    // reinterpreting fields (an items[] array, ad-mediation-specific
    // ad_platform/ad_source/ad_unit_name) this contract's normalized envelope does not
    // carry — inventing those here would be exactly the "distort the central contract
    // to make Firebase support everything" this phase was told not to do. Every metric
    // is therefore sent as a custom Firebase event named after the metric:
    //
    // - unit == CURRENCY metrics (revenue, ad_revenue) map onto GA4's own reserved
    //   value/currency parameters (which feed GA4's revenue reports for *any* custom
    //   event), and reference_id maps onto GA4's reserved transaction_id parameter —
    //   a genuine, non-distorting parameter-level mapping.
    // - Every other metric has no reserved-parameter equivalent in GA4; value is sent
    //   as a plain custom metric_value parameter so a non-monetary measurement never
    //   accidentally feeds GA4's revenue reporting.
    data class MappedMetricEvent(val name: String, val params: Map<String, Any>, val dropped: List<String>)

    fun mapMetric(envelope: InfinityForgeMetricEnvelope): MappedMetricEvent {
        val isMonetary = envelope.unit == InfinityForgeMetricUnit.CURRENCY
        val params = linkedMapOf<String, Any>(
            "schema_version" to envelope.schemaVersion,
            "environment" to envelope.environment.wireValue,
            "source" to envelope.source.wireValue,
            "unit" to envelope.unit.wireValue,
            "infinityforge_anonymous_id" to truncate(envelope.anonymousId, PARAM_VALUE_MAX_LENGTH),
        )
        if (isMonetary) {
            params["value"] = envelope.value
            envelope.currency?.let { params["currency"] = it }
            envelope.referenceId?.let { params["transaction_id"] = truncate(it, PARAM_VALUE_MAX_LENGTH) }
        } else {
            params["metric_value"] = envelope.value
            envelope.referenceId?.let { params["reference_id"] = truncate(it, PARAM_VALUE_MAX_LENGTH) }
        }

        val entries = envelope.dimensions.entries.sortedBy { it.key }
        val remainingBudget = (MAX_EVENT_PARAMS - params.size).coerceAtLeast(0)
        val kept = entries.take(remainingBudget)
        val dropped = entries.drop(remainingBudget).map { it.key }
        for ((key, value) in kept) {
            params[sanitizeParamName(key)] = toFirebaseDimensionValue(value)
        }
        return MappedMetricEvent(sanitizeEventName(envelope.metricName), params, dropped)
    }

    private fun toFirebaseDimensionValue(value: InfinityForgeDimensionValue): Any = when (value) {
        is InfinityForgeDimensionValue.StringValue -> truncate(value.value, PARAM_VALUE_MAX_LENGTH)
        is InfinityForgeDimensionValue.IntValue -> value.value
        is InfinityForgeDimensionValue.BooleanValue -> if (value.value) "true" else "false"
    }

    data class MappedUserProperties(val properties: Map<String, String>, val dropped: List<String>)

    /** Firebase user properties: <=25 total, name <=24 chars, value <=36 chars, same
     * reserved-prefix rule as event parameters. Overflow entries are dropped, not
     * truncated into a collision. */
    fun mapUserProperties(properties: Map<String, InfinityForgePropertyValue>): MappedUserProperties {
        val entries = properties.entries.sortedBy { it.key }
        val kept = entries.take(MAX_USER_PROPERTIES)
        val dropped = entries.drop(MAX_USER_PROPERTIES).map { it.key }
        val result = linkedMapOf<String, String>()
        for ((key, value) in kept) {
            val name = truncate(disambiguate(key), USER_PROPERTY_NAME_MAX_LENGTH)
            val stringValue = when (value) {
                is InfinityForgePropertyValue.StringValue -> value.value
                is InfinityForgePropertyValue.IntValue -> value.value.toString()
                is InfinityForgePropertyValue.NumberValue -> value.value.toString()
                is InfinityForgePropertyValue.BooleanValue -> if (value.value) "true" else "false"
                is InfinityForgePropertyValue.ArrayValue, is InfinityForgePropertyValue.ObjectValue -> jsonEncode(value)
            }
            result[name] = truncate(stringValue, USER_PROPERTY_VALUE_MAX_LENGTH)
        }
        return MappedUserProperties(result, dropped)
    }
}
