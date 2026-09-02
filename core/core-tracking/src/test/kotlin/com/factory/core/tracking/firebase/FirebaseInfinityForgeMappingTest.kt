package com.factory.core.tracking.firebase

import com.factory.core.tracking.InfinityForgeDimensionValue
import com.factory.core.tracking.InfinityForgeEnvironment
import com.factory.core.tracking.InfinityForgeEventEnvelope
import com.factory.core.tracking.InfinityForgeMetricEnvelope
import com.factory.core.tracking.InfinityForgeMetricSource
import com.factory.core.tracking.InfinityForgeMetricUnit
import com.factory.core.tracking.InfinityForgePlatform
import com.factory.core.tracking.InfinityForgePropertyValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseInfinityForgeMappingTest {

    private fun eventEnvelope(
        event: String = "feature_used",
        properties: Map<String, InfinityForgePropertyValue> = emptyMap(),
    ) = InfinityForgeEventEnvelope(
        event = event, schemaVersion = 1, timestamp = "2026-01-01T00:00:00Z", appId = "com.example.app",
        environment = InfinityForgeEnvironment.DEVELOPMENT, platform = InfinityForgePlatform.ANDROID,
        sdkVersion = "1.0.0", sdkName = "kotlin", appVersion = "1.0", userId = null, anonymousId = "anon_1",
        properties = properties,
    )

    private fun metricEnvelope(
        unit: InfinityForgeMetricUnit = InfinityForgeMetricUnit.CURRENCY,
        currency: String? = "USD",
        dimensions: Map<String, InfinityForgeDimensionValue> = emptyMap(),
        referenceId: String? = null,
    ) = InfinityForgeMetricEnvelope(
        metricName = "revenue", schemaVersion = 1, value = 9.99, unit = unit, currency = currency,
        source = InfinityForgeMetricSource.BILLING_SYSTEM, referenceId = referenceId,
        timestamp = "2026-01-01T00:00:00Z", appId = "com.example.app",
        environment = InfinityForgeEnvironment.DEVELOPMENT,
        platform = InfinityForgePlatform.ANDROID, sdkVersion = "1.0.0", sdkName = "kotlin", appVersion = "1.0",
        userId = null, anonymousId = "anon_1", dimensions = dimensions,
    )

    // -- reserved prefixes --

    @Test
    fun `a name with a reserved firebase prefix is disambiguated`() {
        assertEquals("app_firebase_screen", FirebaseInfinityForgeMapping.sanitizeEventName("firebase_screen"))
        assertEquals("app_google_ads", FirebaseInfinityForgeMapping.sanitizeEventName("google_ads"))
    }

    @Test
    fun `a name without a reserved prefix is unchanged`() {
        assertEquals("feature_used", FirebaseInfinityForgeMapping.sanitizeEventName("feature_used"))
    }

    // -- truncation --

    @Test
    fun `an event name over the length limit is truncated`() {
        val long = "a".repeat(50)
        val sanitized = FirebaseInfinityForgeMapping.sanitizeEventName(long)
        assertEquals(FirebaseInfinityForgeMapping.EVENT_NAME_MAX_LENGTH, sanitized.length)
    }

    // -- mapEvent: traceability params + property forwarding --

    @Test
    fun `mapEvent includes schema_version, environment, and anonymous_id`() {
        val mapped = FirebaseInfinityForgeMapping.mapEvent(eventEnvelope())

        assertEquals(1, mapped.params["schema_version"])
        assertEquals("development", mapped.params["environment"])
        assertEquals("anon_1", mapped.params["infinityforge_anonymous_id"])
    }

    @Test
    fun `mapEvent forwards properties under their own key`() {
        val mapped = FirebaseInfinityForgeMapping.mapEvent(
            eventEnvelope(properties = mapOf("feature_name" to InfinityForgePropertyValue.StringValue("export"))),
        )

        assertEquals("export", mapped.params["feature_name"])
    }

    @Test
    fun `mapEvent drops properties beyond firebase's parameter budget`() {
        val properties = (1..30).associate { "prop_$it" to InfinityForgePropertyValue.IntValue(it) }
        val mapped = FirebaseInfinityForgeMapping.mapEvent(eventEnvelope(properties = properties))

        assertTrue(mapped.params.size <= FirebaseInfinityForgeMapping.MAX_EVENT_PARAMS)
        assertTrue(mapped.dropped.isNotEmpty())
    }

    @Test
    fun `mapEvent maps a boolean property to the string literals true or false`() {
        val mapped = FirebaseInfinityForgeMapping.mapEvent(
            eventEnvelope(properties = mapOf("flag" to InfinityForgePropertyValue.BooleanValue(true))),
        )
        assertEquals("true", mapped.params["flag"])
    }

    // -- mapScreenView --

    @Test
    fun `mapScreenView extracts screen_name and mirrors it as screen_class`() {
        val envelope = eventEnvelope(
            event = "screen_viewed",
            properties = mapOf("screen_name" to InfinityForgePropertyValue.StringValue("home")),
        )
        val mapped = FirebaseInfinityForgeMapping.mapScreenView(envelope)

        assertEquals("home", mapped.screenName)
        assertEquals("home", mapped.screenClass)
    }

    // -- mapMetric: monetary vs non-monetary --

    @Test
    fun `a currency metric maps value and currency onto firebase's reserved parameters`() {
        val mapped = FirebaseInfinityForgeMapping.mapMetric(metricEnvelope(referenceId = "txn_1"))

        assertEquals(9.99, mapped.params["value"])
        assertEquals("USD", mapped.params["currency"])
        assertEquals("txn_1", mapped.params["transaction_id"])
        assertFalse(mapped.params.containsKey("metric_value"))
    }

    @Test
    fun `a non-currency metric maps value onto a plain metric_value parameter`() {
        val mapped = FirebaseInfinityForgeMapping.mapMetric(
            metricEnvelope(unit = InfinityForgeMetricUnit.COUNT, currency = null, referenceId = "ref_1"),
        )

        assertEquals(9.99, mapped.params["metric_value"])
        assertEquals("ref_1", mapped.params["reference_id"])
        assertFalse(mapped.params.containsKey("value"))
        assertFalse(mapped.params.containsKey("currency"))
    }

    @Test
    fun `mapMetric forwards dimensions`() {
        val mapped = FirebaseInfinityForgeMapping.mapMetric(
            metricEnvelope(dimensions = mapOf("transaction_type" to InfinityForgeDimensionValue.StringValue("charge"))),
        )
        assertEquals("charge", mapped.params["transaction_type"])
    }

    // -- user properties --

    @Test
    fun `mapUserProperties truncates name and value and drops overflow entries`() {
        val properties = (1..30).associate { "user_prop_$it" to InfinityForgePropertyValue.StringValue("v$it") }
        val mapped = FirebaseInfinityForgeMapping.mapUserProperties(properties)

        assertTrue(mapped.properties.size <= FirebaseInfinityForgeMapping.MAX_USER_PROPERTIES)
        assertTrue(mapped.dropped.isNotEmpty())
        assertTrue(
            mapped.properties.keys.all { it.length <= FirebaseInfinityForgeMapping.USER_PROPERTY_NAME_MAX_LENGTH },
        )
    }

    @Test
    fun `sanitizeUserId truncates to firebase's 256 character limit`() {
        val long = "u".repeat(300)
        assertEquals(
            FirebaseInfinityForgeMapping.USER_ID_MAX_LENGTH,
            FirebaseInfinityForgeMapping.sanitizeUserId(long).length,
        )
    }
}
