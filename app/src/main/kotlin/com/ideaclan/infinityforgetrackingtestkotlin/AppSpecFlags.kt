package com.ideaclan.infinityforgetrackingtestkotlin

/**
 * Machine-managed mirror of `APP_SPEC.yaml`'s capability toggles.
 *
 * `scripts/configure_app.py` rewrites the boolean literals below (and only them) to
 * match `APP_SPEC.yaml` on every run — see `AGENTS.md` §12: `APP_SPEC.yaml` is the
 * single product-configuration source of truth, this file just makes those values
 * available as plain Kotlin constants Hilt can bind against, since parsing YAML at
 * app runtime is unnecessary and adds a dependency this factory doesn't otherwise need.
 * Do not hand-edit these values without also updating `APP_SPEC.yaml` — they must
 * always match, and `scripts/validate_spec.py` checks that they do.
 */
// factory:app-spec-flags:start
object AppSpecFlags {
    const val AUTH_ENABLED = false
    const val AUTH_EMAIL_PASSWORD = false
    const val AUTH_GOOGLE = false
    const val AUTH_ANONYMOUS = false
    const val ONBOARDING_ENABLED = true
    const val ADS_ENABLED = false
    const val PURCHASES_ENABLED = false
    const val ANALYTICS_ENABLED = true
    const val CRASH_REPORTING_ENABLED = true
}
// factory:app-spec-flags:end
