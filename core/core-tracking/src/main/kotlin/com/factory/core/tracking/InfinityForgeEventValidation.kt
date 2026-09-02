package com.factory.core.tracking

/**
 * Result of a validation check. Never thrown — specification/errors.md: malformed
 * input must be rejected/handled, never surfaced as a crash or exception the
 * application must catch. Callers (`InfinityForgeTrackingClient`) decide what to do
 * with an invalid result — log a diagnostic and drop the call before it ever reaches a
 * provider.
 */
data class InfinityForgeValidationResult(val isValid: Boolean, val issues: List<String>) {
    companion object {
        val VALID = InfinityForgeValidationResult(isValid = true, issues = emptyList())
        fun invalid(issues: List<String>) = InfinityForgeValidationResult(isValid = false, issues = issues)
    }
}

/** specification/conventions.md's identifier rule: lowercase snake_case, no
 * leading/trailing underscore, at least 2 characters. Shared by event and metric
 * validation. */
object InfinityForgeIdentifierPattern {
    private val regex = Regex("^[a-z][a-z0-9_]*[a-z0-9]$")

    fun matches(value: String): Boolean = value.length >= 2 && regex.matches(value)
}

/**
 * Runtime validation for the six-operation event API, aligned with
 * infinityforge-tracking-module's schema/event-envelope.yaml,
 * schema/event-properties.yaml, and specification/conventions.md. This is not a
 * second, independently-authored schema — every check derives from
 * [InfinityForgeEventCatalog], the same registry [InfinityForgeEvent]'s factories are
 * built from.
 */
object InfinityForgeEventValidation {
    /** Validates a `track()`/`screen()` call. `eventName` may be a canonical event
     * (validated against its full property schema) or an app-specific event (only
     * checked for a snake_case name and non-reserved properties —
     * specification/events.md permits app-specific events without requiring them to be
     * registered here). */
    fun validateTrackCall(
        eventName: String,
        properties: Map<String, InfinityForgePropertyValue>,
    ): InfinityForgeValidationResult {
        val issues = mutableListOf<String>()

        if (!InfinityForgeIdentifierPattern.matches(eventName)) {
            issues += "event name \"$eventName\" is not snake_case (specification/conventions.md)"
        }
        issues += reservedFieldCollisions(properties)
        issues += priceCurrencyPairingIssues(eventName, properties)
        InfinityForgeEventCatalog.properties[eventName]?.let { spec ->
            issues += canonicalPropertyIssues(spec, properties)
        }

        return if (issues.isEmpty()) {
            InfinityForgeValidationResult.VALID
        } else {
            InfinityForgeValidationResult.invalid(issues)
        }
    }

    fun validateUserId(userId: String): InfinityForgeValidationResult =
        if (userId.isBlank()) {
            InfinityForgeValidationResult.invalid(listOf("user_id must be a non-empty string (specification/api.md)"))
        } else {
            InfinityForgeValidationResult.VALID
        }

    fun validateScreenName(screenName: String): InfinityForgeValidationResult =
        if (screenName.isBlank()) {
            InfinityForgeValidationResult.invalid(
                listOf("screen_name must be a non-empty string (specification/screen-tracking.md)"),
            )
        } else {
            InfinityForgeValidationResult.VALID
        }

    fun validateUserProperties(properties: Map<String, InfinityForgePropertyValue>): InfinityForgeValidationResult {
        val issues = reservedFieldCollisions(properties)
        return if (issues.isEmpty()) {
            InfinityForgeValidationResult.VALID
        } else {
            InfinityForgeValidationResult.invalid(issues)
        }
    }

    /** True when `eventName` is not part of the canonical registry — used only for
     * development diagnostics, never to reject the call. */
    fun isAppSpecificEvent(eventName: String): Boolean = !InfinityForgeEventCatalog.isCanonical(eventName)

    private fun reservedFieldCollisions(properties: Map<String, InfinityForgePropertyValue>): List<String> =
        properties.keys
            .filter { InfinityForgeReservedFields.event.contains(it) }
            .sorted()
            .map { "property \"$it\" collides with a reserved envelope field name" }

    private fun priceCurrencyPairingIssues(
        eventName: String,
        properties: Map<String, InfinityForgePropertyValue>,
    ): List<String> {
        if (!InfinityForgeEventCatalog.priceCurrencyPairedEvents.contains(eventName)) return emptyList()
        if (properties["price"] == null || properties["currency"] != null) return emptyList()
        return listOf(
            "property \"price\" is present without a sibling \"currency\" property (specification/conventions.md)",
        )
    }

    private fun canonicalPropertyIssues(
        spec: Map<String, InfinityForgeEventCatalog.PropertySpec>,
        properties: Map<String, InfinityForgePropertyValue>,
    ): List<String> {
        val issues = mutableListOf<String>()
        for ((key, propertySpec) in spec) {
            if (propertySpec.required && properties[key] == null) {
                issues += "$key: required property is missing"
            }
        }
        for ((key, value) in properties) {
            val propertySpec = spec[key] ?: continue
            typeIssue(key, propertySpec.kind, value)?.let { issues += it }
        }
        return issues
    }

    private fun typeIssue(
        key: String,
        kind: InfinityForgeEventCatalog.Kind,
        value: InfinityForgePropertyValue,
    ): String? =
        when (kind) {
            InfinityForgeEventCatalog.Kind.StringKind ->
                if (value is InfinityForgePropertyValue.StringValue) null else typeMismatch(key)
            InfinityForgeEventCatalog.Kind.IntKind ->
                if (value is InfinityForgePropertyValue.IntValue) null else typeMismatch(key)
            InfinityForgeEventCatalog.Kind.NumberKind ->
                if (value is InfinityForgePropertyValue.NumberValue || value is InfinityForgePropertyValue.IntValue) {
                    null
                } else {
                    typeMismatch(key)
                }
            InfinityForgeEventCatalog.Kind.BooleanKind ->
                if (value is InfinityForgePropertyValue.BooleanValue) null else typeMismatch(key)
            is InfinityForgeEventCatalog.Kind.EnumKind ->
                if (value is InfinityForgePropertyValue.StringValue && kind.allowedValues.contains(value.value)) {
                    null
                } else if (value is InfinityForgePropertyValue.StringValue) {
                    "$key: must be one of ${kind.allowedValues.sorted().joinToString(", ")}"
                } else {
                    typeMismatch(key)
                }
        }

    private fun typeMismatch(key: String) = "$key: does not match the expected type for this property"
}
