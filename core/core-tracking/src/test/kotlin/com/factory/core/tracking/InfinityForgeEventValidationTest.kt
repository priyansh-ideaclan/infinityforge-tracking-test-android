package com.factory.core.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InfinityForgeEventValidationTest {

    @Before
    fun resetRegistry() {
        InfinityForgeAppSpecificSchemaVersions.resetForTests()
    }

    // -- Schema parity: every canonical event factory produces a call that validates --

    @Test
    fun `every canonical event factory produces a valid track call`() {
        val events = listOf(
            InfinityForgeEvent.appOpened(InfinityForgeLaunchType.COLD),
            InfinityForgeEvent.signupStarted(InfinityForgeAuthMethod.EMAIL),
            InfinityForgeEvent.signupCompleted(InfinityForgeAuthMethod.EMAIL),
            InfinityForgeEvent.loginStarted(InfinityForgeAuthMethod.SOCIAL),
            InfinityForgeEvent.loginCompleted(InfinityForgeAuthMethod.SOCIAL),
            InfinityForgeEvent.onboardingStarted(),
            InfinityForgeEvent.onboardingCompleted(stepCount = 3),
            InfinityForgeEvent.featureUsed(featureName = "export"),
            InfinityForgeEvent.paywallViewed(placement = "home"),
            InfinityForgeEvent.trialStarted(plan = "pro", trialLengthDays = 7),
            InfinityForgeEvent.subscriptionStarted(
                plan = "pro",
                price = 9.99,
                currency = "USD",
                billingCycle = InfinityForgeBillingCycle.MONTHLY,
                transactionId = "txn_1",
            ),
            InfinityForgeEvent.subscriptionCancelled(plan = "pro", transactionId = "txn_1"),
            InfinityForgeEvent.purchaseCompleted(
                productId = "sku_1",
                price = 4.99,
                currency = "USD",
                quantity = 1,
                transactionId = "txn_2",
            ),
        )
        for (event in events) {
            val result = InfinityForgeEventValidation.validateTrackCall(event.name, event.properties)
            assertTrue("${event.name} should validate: ${result.issues}", result.isValid)
        }
    }

    @Test
    fun `screenViewed requires screen_name and validates`() {
        val event = InfinityForgeEvent.screenViewed("home", previousScreen = null, extraProperties = emptyMap())
        val result = InfinityForgeEventValidation.validateTrackCall(event.name, event.properties)
        assertTrue(result.isValid)
        assertEquals("home", (event.properties["screen_name"] as InfinityForgePropertyValue.StringValue).value)
    }

    // -- Required-field enforcement --

    @Test
    fun `feature_used missing required feature_name is invalid`() {
        val result = InfinityForgeEventValidation.validateTrackCall("feature_used", emptyMap())
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("feature_name") })
    }

    @Test
    fun `subscription_started missing required plan is invalid`() {
        val result = InfinityForgeEventValidation.validateTrackCall("subscription_started", emptyMap())
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("plan") })
    }

    // -- Type checking --

    @Test
    fun `wrong type for a known property is rejected`() {
        val result = InfinityForgeEventValidation.validateTrackCall(
            "feature_used",
            mapOf(
                "feature_name" to InfinityForgePropertyValue.IntValue(1),
            ),
        )
        assertFalse(result.isValid)
    }

    @Test
    fun `unrecognized enum value is rejected`() {
        val result = InfinityForgeEventValidation.validateTrackCall(
            "signup_started",
            mapOf("method" to InfinityForgePropertyValue.StringValue("carrier_pigeon")),
        )
        assertFalse(result.isValid)
    }

    // -- price/currency pairing --

    @Test
    fun `price without currency on a paired event is rejected`() {
        val result = InfinityForgeEventValidation.validateTrackCall(
            "purchase_completed",
            mapOf(
                "product_id" to InfinityForgePropertyValue.StringValue("sku_1"),
                "price" to InfinityForgePropertyValue.NumberValue(4.99),
            ),
        )
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("currency") })
    }

    @Test
    fun `price with currency on a paired event is valid`() {
        val result = InfinityForgeEventValidation.validateTrackCall(
            "purchase_completed",
            mapOf(
                "product_id" to InfinityForgePropertyValue.StringValue("sku_1"),
                "price" to InfinityForgePropertyValue.NumberValue(4.99),
                "currency" to InfinityForgePropertyValue.StringValue("USD"),
            ),
        )
        assertTrue(result.isValid)
    }

    // -- Naming and reserved fields --

    @Test
    fun `non snake_case event name is rejected`() {
        val result = InfinityForgeEventValidation.validateTrackCall("SomeEvent", emptyMap())
        assertFalse(result.isValid)
    }

    @Test
    fun `property colliding with a reserved envelope field is rejected`() {
        val result = InfinityForgeEventValidation.validateTrackCall(
            "onboarding_started",
            mapOf("timestamp" to InfinityForgePropertyValue.StringValue("2026-01-01T00:00:00Z")),
        )
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("timestamp") })
    }

    // -- App-specific events --

    @Test
    fun `app-specific event with a valid name is valid and flagged non-canonical`() {
        val result = InfinityForgeEventValidation.validateTrackCall("my_app_event", emptyMap())
        assertTrue(result.isValid)
        assertTrue(InfinityForgeEventValidation.isAppSpecificEvent("my_app_event"))
        assertFalse(InfinityForgeEventValidation.isAppSpecificEvent("app_opened"))
    }

    @Test
    fun `custom event defaults to schema version 1 then honors registration`() {
        val defaulted = InfinityForgeEvent.custom("my_app_event")
        assertEquals(1, defaulted.schemaVersion)

        InfinityForgeAppSpecificSchemaVersions.registerEvent("my_app_event", version = 2)
        val versioned = InfinityForgeEvent.custom("my_app_event")
        assertEquals(2, versioned.schemaVersion)
    }

    @Test
    fun `registering an app-specific event with a schema version below 1 throws rather than silently no-opping`() {
        assertThrows(IllegalArgumentException::class.java) {
            InfinityForgeAppSpecificSchemaVersions.registerEvent("my_app_event", version = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            InfinityForgeAppSpecificSchemaVersions.registerEvent("my_app_event", version = -1)
        }
        // The rejected calls must not have partially registered — lookups still default to 1.
        assertEquals(1, InfinityForgeAppSpecificSchemaVersions.event("my_app_event"))
    }

    // -- identify()/setUserProperties() validation --

    @Test
    fun `blank user id is invalid`() {
        assertFalse(InfinityForgeEventValidation.validateUserId("  ").isValid)
        assertFalse(InfinityForgeEventValidation.validateUserId("").isValid)
    }

    @Test
    fun `non-empty user id is valid`() {
        assertTrue(InfinityForgeEventValidation.validateUserId("user_42").isValid)
    }

    @Test
    fun `blank screen name is invalid`() {
        assertFalse(InfinityForgeEventValidation.validateScreenName(" ").isValid)
    }

    @Test
    fun `user properties colliding with a reserved field are rejected`() {
        val result = InfinityForgeEventValidation.validateUserProperties(
            mapOf("user_id" to InfinityForgePropertyValue.StringValue("nope")),
        )
        assertFalse(result.isValid)
    }
}
