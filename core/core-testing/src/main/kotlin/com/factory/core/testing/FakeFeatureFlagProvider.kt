package com.factory.core.testing

import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider

/**
 * Lets a test force any combination of flags — including ones the checked-in
 * `APP_SPEC.yaml` doesn't currently represent — which is what makes the
 * minimal/auth+ads/auth+purchases/full-v1 module-combination tests in
 * `Docs/testing/module-matrix.md` possible without four separately-configured apps.
 */
class FakeFeatureFlagProvider(
    private val enabledFlags: Set<FeatureFlag> = emptySet(),
) : FeatureFlagProvider {
    override fun isEnabled(flag: FeatureFlag): Boolean = flag in enabledFlags
}
