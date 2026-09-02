package com.factory.core.tracking

/**
 * A validated metric, ready to become an [InfinityForgeMetricEnvelope]. Canonical
 * metrics are only constructible through this type's factory functions, which fix
 * `unit` (never a per-call choice — specification/metric-envelope.md) and, for
 * `ad_impression`/`handled_error`, `value` itself (specification/metrics.md's
 * `fixed_value`). [custom] is the only path for an app-specific metric, which must
 * supply `unit`/`source` itself since there is no canonical definition to default them
 * from — mirroring the reference RN implementation's `AppSpecificMetricPayload`.
 */
data class InfinityForgeMetric internal constructor(
    val metricName: String,
    val schemaVersion: Int,
    val value: Double,
    val unit: InfinityForgeMetricUnit,
    val currency: String?,
    val source: InfinityForgeMetricSource,
    val referenceId: String?,
    val dimensions: Map<String, InfinityForgeDimensionValue>,
    val isCanonical: Boolean,
) {
    companion object {
        // -- Monetization --

        fun revenue(
            value: Double,
            currency: String,
            transactionType: InfinityForgeTransactionType,
            billingType: InfinityForgeBillingType? = null,
            productId: String? = null,
            source: InfinityForgeMetricSource = InfinityForgeMetricSource.BILLING_SYSTEM,
            referenceId: String? = null,
        ): InfinityForgeMetric {
            val dimensions = buildMap<String, InfinityForgeDimensionValue> {
                put("transaction_type", InfinityForgeDimensionValue.StringValue(transactionType.wireValue))
                billingType?.let { put("billing_type", InfinityForgeDimensionValue.StringValue(it.wireValue)) }
                productId?.let { put("product_id", InfinityForgeDimensionValue.StringValue(it)) }
            }
            return InfinityForgeMetric(
                "revenue", 1, value, InfinityForgeMetricUnit.CURRENCY, currency, source, referenceId,
                dimensions, isCanonical = true,
            )
        }

        // -- Advertising --

        /** `value` is fixed to `1` by the metric's own definition
         * (specification/metrics.md) — not a caller-supplied parameter, unlike every
         * other metric here. */
        fun adImpression(
            placement: String? = null,
            adFormat: InfinityForgeAdFormat? = null,
            network: String? = null,
            source: InfinityForgeMetricSource = InfinityForgeMetricSource.ADVERTISING_SYSTEM,
            referenceId: String? = null,
        ): InfinityForgeMetric {
            val dimensions = buildMap<String, InfinityForgeDimensionValue> {
                placement?.let { put("placement", InfinityForgeDimensionValue.StringValue(it)) }
                adFormat?.let { put("ad_format", InfinityForgeDimensionValue.StringValue(it.wireValue)) }
                network?.let { put("network", InfinityForgeDimensionValue.StringValue(it)) }
            }
            return InfinityForgeMetric(
                "ad_impression", 1, 1.0, InfinityForgeMetricUnit.IMPRESSION, null, source, referenceId,
                dimensions, isCanonical = true,
            )
        }

        fun adRevenue(
            value: Double,
            currency: String,
            placement: String? = null,
            adFormat: InfinityForgeAdFormat? = null,
            network: String? = null,
            precision: InfinityForgeAdRevenuePrecision? = null,
            source: InfinityForgeMetricSource = InfinityForgeMetricSource.ADVERTISING_SYSTEM,
            referenceId: String? = null,
        ): InfinityForgeMetric {
            val dimensions = buildMap<String, InfinityForgeDimensionValue> {
                placement?.let { put("placement", InfinityForgeDimensionValue.StringValue(it)) }
                adFormat?.let { put("ad_format", InfinityForgeDimensionValue.StringValue(it.wireValue)) }
                network?.let { put("network", InfinityForgeDimensionValue.StringValue(it)) }
                precision?.let { put("precision", InfinityForgeDimensionValue.StringValue(it.wireValue)) }
            }
            return InfinityForgeMetric(
                "ad_revenue", 1, value, InfinityForgeMetricUnit.CURRENCY, currency, source, referenceId,
                dimensions, isCanonical = true,
            )
        }

        // -- Engagement --

        fun sessionDuration(
            seconds: Double,
            source: InfinityForgeMetricSource = InfinityForgeMetricSource.APPLICATION,
            referenceId: String? = null,
        ): InfinityForgeMetric = InfinityForgeMetric(
            "session_duration", 1, seconds, InfinityForgeMetricUnit.SECOND, null, source, referenceId,
            emptyMap(), isCanonical = true,
        )

        // -- Performance --

        fun appLaunchDuration(
            milliseconds: Double,
            launchType: InfinityForgeLaunchType? = null,
            source: InfinityForgeMetricSource = InfinityForgeMetricSource.APPLICATION,
            referenceId: String? = null,
        ): InfinityForgeMetric {
            val dimensions = buildMap<String, InfinityForgeDimensionValue> {
                launchType?.let { put("launch_type", InfinityForgeDimensionValue.StringValue(it.wireValue)) }
            }
            return InfinityForgeMetric(
                "app_launch_duration", 1, milliseconds, InfinityForgeMetricUnit.MILLISECOND, null, source,
                referenceId, dimensions, isCanonical = true,
            )
        }

        fun screenLoadDuration(
            milliseconds: Double,
            screenName: String,
            source: InfinityForgeMetricSource = InfinityForgeMetricSource.APPLICATION,
            referenceId: String? = null,
        ): InfinityForgeMetric = InfinityForgeMetric(
            "screen_load_duration", 1, milliseconds, InfinityForgeMetricUnit.MILLISECOND, null, source,
            referenceId,
            mapOf("screen_name" to InfinityForgeDimensionValue.StringValue(screenName)),
            isCanonical = true,
        )

        fun operationDuration(
            milliseconds: Double,
            operation: String,
            operationCategory: InfinityForgeOperationCategory? = null,
            outcome: InfinityForgeOperationOutcome? = null,
            source: InfinityForgeMetricSource = InfinityForgeMetricSource.APPLICATION,
            referenceId: String? = null,
        ): InfinityForgeMetric {
            val dimensions = buildMap<String, InfinityForgeDimensionValue> {
                put("operation", InfinityForgeDimensionValue.StringValue(operation))
                operationCategory?.let {
                    put("operation_category", InfinityForgeDimensionValue.StringValue(it.wireValue))
                }
                outcome?.let { put("outcome", InfinityForgeDimensionValue.StringValue(it.wireValue)) }
            }
            return InfinityForgeMetric(
                "operation_duration", 1, milliseconds, InfinityForgeMetricUnit.MILLISECOND, null, source,
                referenceId, dimensions, isCanonical = true,
            )
        }

        // -- Reliability --

        /** Each handled occurrence is a count measurement with value `1`, as required
         * by metrics/reliability.yaml and specification/metrics.md. */
        fun handledError(
            category: InfinityForgeHandledErrorCategory,
            errorCode: String? = null,
            source: InfinityForgeMetricSource = InfinityForgeMetricSource.APPLICATION,
            referenceId: String? = null,
        ): InfinityForgeMetric {
            val dimensions = buildMap<String, InfinityForgeDimensionValue> {
                put("category", InfinityForgeDimensionValue.StringValue(category.wireValue))
                errorCode?.let { put("error_code", InfinityForgeDimensionValue.StringValue(it)) }
            }
            return InfinityForgeMetric(
                "handled_error", 1, 1.0, InfinityForgeMetricUnit.COUNT, null, source, referenceId,
                dimensions, isCanonical = true,
            )
        }

        // -- App-specific --

        /** An app-specific (non-canonical) metric — specification/metrics.md's
         * "App-specific metrics" allowance. Unlike a canonical factory, `unit`/`source`
         * must be supplied explicitly (there is no canonical definition to default them
         * from); `schemaVersion` defaults to whatever
         * [InfinityForgeAppSpecificSchemaVersions] has on file for `name` (1 if never
         * registered). */
        fun custom(
            name: String,
            value: Double,
            unit: InfinityForgeMetricUnit,
            source: InfinityForgeMetricSource,
            currency: String? = null,
            referenceId: String? = null,
            dimensions: Map<String, InfinityForgeDimensionValue> = emptyMap(),
        ): InfinityForgeMetric = InfinityForgeMetric(
            name, InfinityForgeAppSpecificSchemaVersions.metric(name), value, unit, currency, source,
            referenceId, dimensions, isCanonical = false,
        )
    }
}
