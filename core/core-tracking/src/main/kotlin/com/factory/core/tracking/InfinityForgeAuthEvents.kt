package com.factory.core.tracking

/**
 * Authentication-category canonical event factories for [InfinityForgeEvent.Companion]
 * — split into this file only to keep that companion object under Detekt's
 * per-object function-count ceiling (complexity.TooManyFunctions); see
 * [InfinityForgeEvent]'s own doc comment. Every function below is a Kotlin extension
 * function on the companion object, so it resolves exactly as
 * `InfinityForgeEvent.signupStarted(...)` etc., identical to a member.
 */
fun InfinityForgeEvent.Companion.signupStarted(method: InfinityForgeAuthMethod? = null) =
    InfinityForgeEvent("signup_started", 1, methodProperty(method), isCanonical = true)

fun InfinityForgeEvent.Companion.signupCompleted(method: InfinityForgeAuthMethod? = null) =
    InfinityForgeEvent("signup_completed", 1, methodProperty(method), isCanonical = true)

fun InfinityForgeEvent.Companion.loginStarted(method: InfinityForgeAuthMethod? = null) =
    InfinityForgeEvent("login_started", 1, methodProperty(method), isCanonical = true)

fun InfinityForgeEvent.Companion.loginCompleted(method: InfinityForgeAuthMethod? = null) =
    InfinityForgeEvent("login_completed", 1, methodProperty(method), isCanonical = true)

private fun methodProperty(method: InfinityForgeAuthMethod?): Map<String, InfinityForgePropertyValue> =
    method?.let { mapOf("method" to InfinityForgePropertyValue.StringValue(it.wireValue)) } ?: emptyMap()
