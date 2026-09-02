package com.factory.purchases.revenuecat

import com.factory.core.logging.AndroidLogger
import com.factory.core.testing.FakeFeatureFlagProvider
import com.factory.purchases.api.FakePurchasesController
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One of the factory's "module combination" checks (see Docs/testing/module-matrix.md).
 * Only the "purchases disabled" branch is exercised here: the "enabled" branch
 * constructs a real `RevenueCatPurchasesController`, whose `init` block reads
 * `Purchases.sharedInstance` — that singleton is only set by a real
 * `Purchases.configure(...)` call, which this factory correctly refuses to invent (see
 * AGENTS.md §7). Mocking that static singleton is possible but not done in this pass;
 * recorded honestly rather than faked.
 */
class PurchasesModuleTest {

    private val logger = AndroidLogger(isDebug = true)

    @Test
    fun `purchases disabled selects the fake controller`() {
        val flags = FakeFeatureFlagProvider(enabledFlags = emptySet())
        val entitlementId = PremiumEntitlementId("premium")

        val controller = PurchasesModule.providePurchasesController(flags, entitlementId, logger)

        assertTrue(controller is FakePurchasesController)
    }
}
