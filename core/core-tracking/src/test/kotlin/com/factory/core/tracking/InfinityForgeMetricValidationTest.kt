package com.factory.core.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InfinityForgeMetricValidationTest {

    @Before
    fun resetRegistry() {
        InfinityForgeAppSpecificSchemaVersions.resetForTests()
    }

    @Test
    fun `every canonical metric factory produces a valid recordMetric call`() {
        val metrics = listOf(
            InfinityForgeMetric.revenue(
                9.99, "USD", InfinityForgeTransactionType.CHARGE, InfinityForgeBillingType.SUBSCRIPTION,
            ),
            InfinityForgeMetric.adImpression(placement = "home_banner", adFormat = InfinityForgeAdFormat.BANNER),
            InfinityForgeMetric.adRevenue(0.01, "USD", precision = InfinityForgeAdRevenuePrecision.ESTIMATED),
            InfinityForgeMetric.sessionDuration(seconds = 120.0),
            InfinityForgeMetric.appLaunchDuration(milliseconds = 800.0, launchType = InfinityForgeLaunchType.COLD),
            InfinityForgeMetric.screenLoadDuration(milliseconds = 50.0, screenName = "home"),
            InfinityForgeMetric.operationDuration(
                milliseconds = 120.0,
                operation = "api:get_profile",
                operationCategory = InfinityForgeOperationCategory.API,
                outcome = InfinityForgeOperationOutcome.SUCCESS,
            ),
            InfinityForgeMetric.handledError(
                category = InfinityForgeHandledErrorCategory.OPERATIONAL_FAILURE, errorCode = "E_TIMEOUT",
            ),
        )
        for (metric in metrics) {
            val result = InfinityForgeMetricValidation.validateRecordMetricCall(metric)
            assertTrue("${metric.metricName} should validate: ${result.issues}", result.isValid)
        }
    }

    // -- fixed unit / fixed value enforcement --

    @Test
    fun `ad_impression with wrong unit is rejected`() {
        val metric = InfinityForgeMetric.custom(
            name = "ad_impression", value = 1.0, unit = InfinityForgeMetricUnit.COUNT,
            source = InfinityForgeMetricSource.ADVERTISING_SYSTEM,
        )
        val result = InfinityForgeMetricValidation.validateRecordMetricCall(metric)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("unit") })
    }

    @Test
    fun `handled_error value must equal the fixed value of 1`() {
        val metric = InfinityForgeMetric.custom(
            name = "handled_error", value = 5.0, unit = InfinityForgeMetricUnit.COUNT,
            source = InfinityForgeMetricSource.APPLICATION,
            dimensions = mapOf("category" to InfinityForgeDimensionValue.StringValue("recoverable_error")),
        )
        val result = InfinityForgeMetricValidation.validateRecordMetricCall(metric)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("value must equal") })
    }

    // -- required dimensions --

    @Test
    fun `revenue missing required transaction_type dimension is rejected`() {
        val metric = InfinityForgeMetric.custom(
            name = "revenue", value = 9.99, unit = InfinityForgeMetricUnit.CURRENCY, currency = "USD",
            source = InfinityForgeMetricSource.BILLING_SYSTEM,
        )
        val result = InfinityForgeMetricValidation.validateRecordMetricCall(metric)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("transaction_type") })
    }

    @Test
    fun `screen_load_duration missing required screen_name dimension is rejected`() {
        val metric = InfinityForgeMetric.custom(
            name = "screen_load_duration", value = 50.0, unit = InfinityForgeMetricUnit.MILLISECOND,
            source = InfinityForgeMetricSource.APPLICATION,
        )
        val result = InfinityForgeMetricValidation.validateRecordMetricCall(metric)
        assertFalse(result.isValid)
    }

    // -- currency conditional requirement --

    @Test
    fun `currency required when unit is currency`() {
        val metric = InfinityForgeMetric.custom(
            name = "custom_revenue_like", value = 5.0, unit = InfinityForgeMetricUnit.CURRENCY,
            source = InfinityForgeMetricSource.BILLING_SYSTEM,
        )
        val result = InfinityForgeMetricValidation.validateRecordMetricCall(metric)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("currency") })
    }

    @Test
    fun `currency forbidden when unit is not currency`() {
        val metric = InfinityForgeMetric.custom(
            name = "custom_count", value = 1.0, unit = InfinityForgeMetricUnit.COUNT, currency = "USD",
            source = InfinityForgeMetricSource.APPLICATION,
        )
        val result = InfinityForgeMetricValidation.validateRecordMetricCall(metric)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("currency") })
    }

    @Test
    fun `lowercase currency code is rejected`() {
        val metric = InfinityForgeMetric.custom(
            name = "custom_revenue_like", value = 5.0, unit = InfinityForgeMetricUnit.CURRENCY, currency = "usd",
            source = InfinityForgeMetricSource.BILLING_SYSTEM,
        )
        assertFalse(InfinityForgeMetricValidation.validateRecordMetricCall(metric).isValid)
    }

    // -- value sanity --

    @Test
    fun `negative value is rejected`() {
        val metric = InfinityForgeMetric.custom(
            name = "custom_count", value = -1.0, unit = InfinityForgeMetricUnit.COUNT,
            source = InfinityForgeMetricSource.APPLICATION,
        )
        assertFalse(InfinityForgeMetricValidation.validateRecordMetricCall(metric).isValid)
    }

    @Test
    fun `non-finite value is rejected`() {
        val metric = InfinityForgeMetric.custom(
            name = "custom_count", value = Double.NaN, unit = InfinityForgeMetricUnit.COUNT,
            source = InfinityForgeMetricSource.APPLICATION,
        )
        assertFalse(InfinityForgeMetricValidation.validateRecordMetricCall(metric).isValid)
    }

    // -- app-specific metrics --

    @Test
    fun `app-specific metric is valid and flagged non-canonical`() {
        val metric = InfinityForgeMetric.custom(
            name = "custom_count", value = 1.0, unit = InfinityForgeMetricUnit.COUNT,
            source = InfinityForgeMetricSource.APPLICATION,
        )
        assertTrue(InfinityForgeMetricValidation.validateRecordMetricCall(metric).isValid)
        assertTrue(InfinityForgeMetricValidation.isAppSpecificMetric("custom_count"))
        assertFalse(InfinityForgeMetricValidation.isAppSpecificMetric("revenue"))
    }

    @Test
    fun `custom metric schema version defaults to 1 then honors registration`() {
        assertEquals(1, InfinityForgeAppSpecificSchemaVersions.metric("custom_count"))
        InfinityForgeAppSpecificSchemaVersions.registerMetric("custom_count", version = 3)
        assertEquals(3, InfinityForgeAppSpecificSchemaVersions.metric("custom_count"))
    }

    @Test
    fun `registering an app-specific metric with a schema version below 1 throws rather than silently no-opping`() {
        assertThrows(IllegalArgumentException::class.java) {
            InfinityForgeAppSpecificSchemaVersions.registerMetric("custom_count", version = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            InfinityForgeAppSpecificSchemaVersions.registerMetric("custom_count", version = -5)
        }
        // The rejected calls must not have partially registered — lookups still default to 1.
        assertEquals(1, InfinityForgeAppSpecificSchemaVersions.metric("custom_count"))
    }

    // -- reserved dimension / snake_case dimension keys --

    @Test
    fun `dimension colliding with a reserved envelope field is rejected`() {
        val metric = InfinityForgeMetric.custom(
            name = "custom_count", value = 1.0, unit = InfinityForgeMetricUnit.COUNT,
            source = InfinityForgeMetricSource.APPLICATION,
            dimensions = mapOf("value" to InfinityForgeDimensionValue.IntValue(1)),
        )
        assertFalse(InfinityForgeMetricValidation.validateRecordMetricCall(metric).isValid)
    }

    @Test
    fun `non snake_case dimension key is rejected`() {
        val metric = InfinityForgeMetric.custom(
            name = "custom_count", value = 1.0, unit = InfinityForgeMetricUnit.COUNT,
            source = InfinityForgeMetricSource.APPLICATION,
            dimensions = mapOf("BadKey" to InfinityForgeDimensionValue.IntValue(1)),
        )
        assertFalse(InfinityForgeMetricValidation.validateRecordMetricCall(metric).isValid)
    }
}
