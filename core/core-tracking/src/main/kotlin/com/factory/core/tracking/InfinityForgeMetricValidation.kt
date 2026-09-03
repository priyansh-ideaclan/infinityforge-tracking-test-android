package com.factory.core.tracking

/**
 * Runtime validation for the Metrics capability, aligned with
 * infinityforge-tracking-module's schema/metric-envelope.yaml,
 * schema/metric-dimensions.yaml, and specification/metrics.md section 9 ("Validation
 * rules"). Mirrors [InfinityForgeEventValidation]'s "always return a result, never
 * throw" contract — a malformed metric must be rejected/handled, never surfaced as an
 * exception, and must never reach a provider (specification/errors.md's "Metrics"
 * subsection).
 */
object InfinityForgeMetricValidation {
    private val currencyCodeRegex = Regex("^[A-Z]{3}$")

    /** Validates a `recordMetric()` call. `metric.metricName` may be a canonical metric
     * (validated against its full unit/fixed-value/dimension schema) or an
     * app-specific metric (only checked for a snake_case name, a documented unit, and
     * metric-shaped dimensions — specification/metrics.md permits app-specific metrics
     * without requiring them to be registered here). */
    fun validateRecordMetricCall(metric: InfinityForgeMetric): InfinityForgeValidationResult {
        val issues = mutableListOf<String>()
        issues += basicFieldIssues(metric)
        issues += reservedDimensionCollisions(metric.dimensions)
        issues += nonSnakeCaseDimensionKeys(metric.dimensions)

        val effectiveUnit = if (InfinityForgeMetricCatalog.isCanonical(metric.metricName)) {
            issues += canonicalMetricIssues(metric)
            InfinityForgeMetricCatalog.units[metric.metricName] ?: metric.unit
        } else {
            metric.unit
        }
        issues += currencyIssue(effectiveUnit, metric.currency)

        return if (issues.isEmpty()) {
            InfinityForgeValidationResult.VALID
        } else {
            InfinityForgeValidationResult.invalid(issues)
        }
    }

    /** The three checks that apply to every `recordMetric()` call regardless of
     * canonical/app-specific status: snake_case name (specification/conventions.md),
     * a non-negative finite value, and, when present, a non-blank reference_id — all
     * three from specification/metric-envelope.md's "Value and unit semantics". */
    private fun basicFieldIssues(metric: InfinityForgeMetric): List<String> {
        val issues = mutableListOf<String>()
        if (!InfinityForgeIdentifierPattern.matches(metric.metricName)) {
            issues += "metric name \"${metric.metricName}\" is not snake_case (specification/conventions.md)"
        }
        if (!metric.value.isFinite() || metric.value < 0) {
            issues += "value must be a non-negative, finite number (specification/metric-envelope.md)"
        }
        if (metric.referenceId != null && metric.referenceId.isBlank()) {
            issues += "reference_id, when present, must be a non-empty string"
        }
        return issues
    }

    /** Every canonical-metric-only check: fixed unit, fixed value (when defined), and
     * dimension shape — only evaluated once [InfinityForgeMetricCatalog.isCanonical]
     * is already true. */
    private fun canonicalMetricIssues(metric: InfinityForgeMetric): List<String> {
        val issues = mutableListOf<String>()
        val expectedUnit = InfinityForgeMetricCatalog.units[metric.metricName]
        if (metric.unit != expectedUnit) {
            issues += "unit must be \"${expectedUnit?.wireValue ?: "?"}\" for metric \"${metric.metricName}\" " +
                "(fixed by its definition, not chosen per-call)"
        }
        InfinityForgeMetricCatalog.fixedValues[metric.metricName]?.let { fixedValue ->
            if (metric.value != fixedValue) {
                issues += "value must equal $fixedValue for metric \"${metric.metricName}\" " +
                    "(its canonical definition fixes this value)"
            }
        }
        issues += dimensionIssues(
            InfinityForgeMetricCatalog.dimensions[metric.metricName] ?: emptyMap(),
            metric.dimensions,
        )
        return issues
    }

    /** currency: required iff unit == CURRENCY, forbidden otherwise —
     * specification/metric-envelope.md's "Currency semantics". */
    private fun currencyIssue(effectiveUnit: InfinityForgeMetricUnit, currency: String?): List<String> = when {
        effectiveUnit == InfinityForgeMetricUnit.CURRENCY && !currencyCodeRegex.matches(currency ?: "") ->
            listOf("currency is required and must be an uppercase ISO 4217 code when unit is \"currency\"")
        effectiveUnit != InfinityForgeMetricUnit.CURRENCY && currency != null ->
            listOf(
                "currency must not be present when unit is not \"currency\" " +
                    "(got unit \"${effectiveUnit.wireValue}\")",
            )
        else -> emptyList()
    }

    /** True when `metricName` is not part of the canonical registry — used only for
     * development diagnostics, never to reject the call. */
    fun isAppSpecificMetric(metricName: String): Boolean = !InfinityForgeMetricCatalog.isCanonical(metricName)

    private fun reservedDimensionCollisions(dimensions: Map<String, InfinityForgeDimensionValue>): List<String> =
        dimensions.keys
            .filter { InfinityForgeReservedFields.metric.contains(it) }
            .sorted()
            .map { "dimension \"$it\" collides with a reserved envelope field name" }

    private fun nonSnakeCaseDimensionKeys(dimensions: Map<String, InfinityForgeDimensionValue>): List<String> =
        dimensions.keys
            .filter { !InfinityForgeIdentifierPattern.matches(it) }
            .sorted()
            .map { "dimension key \"$it\" is not snake_case (specification/conventions.md)" }

    private fun dimensionIssues(
        spec: Map<String, InfinityForgeMetricCatalog.DimensionSpec>,
        dimensions: Map<String, InfinityForgeDimensionValue>,
    ): List<String> {
        val issues = mutableListOf<String>()
        for ((key, dimensionSpec) in spec) {
            if (dimensionSpec.required && dimensions[key] == null) {
                issues += "dimensions.$key: required dimension is missing"
            }
        }
        for ((key, value) in dimensions) {
            val dimensionSpec = spec[key] ?: continue
            typeIssue(key, dimensionSpec.kind, value)?.let { issues += it }
        }
        return issues
    }

    private fun typeIssue(
        key: String,
        kind: InfinityForgeMetricCatalog.Kind,
        value: InfinityForgeDimensionValue,
    ): String? = when (kind) {
        InfinityForgeMetricCatalog.Kind.StringKind ->
            if (value is InfinityForgeDimensionValue.StringValue) null else typeMismatch(key)
        InfinityForgeMetricCatalog.Kind.IntKind ->
            if (value is InfinityForgeDimensionValue.IntValue) null else typeMismatch(key)
        InfinityForgeMetricCatalog.Kind.BooleanKind ->
            if (value is InfinityForgeDimensionValue.BooleanValue) null else typeMismatch(key)
        is InfinityForgeMetricCatalog.Kind.EnumKind ->
            if (value is InfinityForgeDimensionValue.StringValue && kind.allowedValues.contains(value.value)) {
                null
            } else if (value is InfinityForgeDimensionValue.StringValue) {
                "dimensions.$key: must be one of ${kind.allowedValues.sorted().joinToString(", ")}"
            } else {
                typeMismatch(key)
            }
    }

    private fun typeMismatch(key: String) = "dimensions.$key: does not match the expected type for this dimension"
}
