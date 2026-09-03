package com.factory.core.tracking.firebase

import com.factory.core.tracking.InfinityForgeEventEnvelope
import com.factory.core.tracking.InfinityForgePropertyValue
import com.factory.core.tracking.firebase.FirebaseInfinityForgeMapping.MappedEvent
import com.factory.core.tracking.firebase.FirebaseInfinityForgeMapping.MappedScreenView

/**
 * Event/screen-view-facing extensions of [FirebaseInfinityForgeMapping] — split out of
 * that object only to keep it under Detekt's per-object function-count ceiling
 * (complexity.TooManyFunctions, `thresholdInObjects`). Every function below still
 * resolves as `FirebaseInfinityForgeMapping.xyz(...)` exactly as if it were a member
 * (that's what a Kotlin extension function on an object gives you), and each depends
 * only on that object's already-public API (`truncate`/`disambiguate`/
 * `toFirebaseParamValue`/the `*_MAX_LENGTH` constants), never on its private internals
 * — so this split changes nothing about visibility, call sites, or behavior. See
 * [FirebaseInfinityForgeMapping]'s own doc comment for the mapping's design rationale.
 */
fun FirebaseInfinityForgeMapping.sanitizeEventName(name: String): String =
    truncate(disambiguate(name), EVENT_NAME_MAX_LENGTH)

fun FirebaseInfinityForgeMapping.sanitizeParamName(name: String): String =
    truncate(disambiguate(name), PARAM_NAME_MAX_LENGTH)

/** Maps one non-screen event envelope onto a Firebase `logEvent()` call. Only
 * `properties` plus three small, fixed traceability parameters cross onto the wire:
 * `schema_version` (lets a downstream export distinguish which version of an event's
 * shape produced a row), `environment` (lets a shared Firebase project filter
 * development/preview noise from production analysis), and
 * `infinityforge_anonymous_id` (this contract's `anonymous_id` is a distinct concept
 * from Firebase's own auto-generated App Instance ID — see
 * `FirebaseInfinityForgeProvider`'s identity-mapping notes — and has no other home in
 * Firebase's data model). NOT forwarded: `timestamp`/`app_id`/`app_version`/
 * `platform`/`sdk_name`/`sdk_version` (Firebase's own SDK already attaches these to
 * every event it logs), `event` (becomes the Firebase event name itself),
 * `user_id`/`anonymous_id` beyond the one traceability parameter (`user_id` reaches
 * Firebase via `setUserId`). */
fun FirebaseInfinityForgeMapping.mapEvent(envelope: InfinityForgeEventEnvelope): MappedEvent {
    val params = linkedMapOf<String, Any>(
        "schema_version" to envelope.schemaVersion,
        "environment" to envelope.environment.wireValue,
        "infinityforge_anonymous_id" to truncate(envelope.anonymousId, PARAM_VALUE_MAX_LENGTH),
    )
    val entries = envelope.properties.entries.sortedBy { it.key }
    val remainingBudget = (MAX_EVENT_PARAMS - params.size).coerceAtLeast(0)
    val kept = entries.take(remainingBudget)
    val dropped = entries.drop(remainingBudget).map { it.key }
    for ((key, value) in kept) {
        params[sanitizeParamName(key)] = toFirebaseParamValue(value)
    }
    return MappedEvent(sanitizeEventName(envelope.event), params, dropped)
}

/** Maps a `screen_viewed` envelope onto Firebase's reserved `screen_view`
 * event/parameters rather than a generic custom event — this is what populates
 * Firebase's built-in Screens/Realtime reports. `screen_class` has no equivalent
 * concept in this contract (screen-tracking.md defines only `screen_name`), so it is
 * set to the same value — a conservative choice that keeps Firebase's reports
 * populated without inventing meaning the contract doesn't define. Only
 * `screen_name` crosses onto this call; `previous_screen` and any other app-specific
 * screen properties are not forwarded here. */
fun FirebaseInfinityForgeMapping.mapScreenView(envelope: InfinityForgeEventEnvelope): MappedScreenView {
    val screenName = (envelope.properties["screen_name"] as? InfinityForgePropertyValue.StringValue)?.value ?: ""
    val sanitized = truncate(screenName, PARAM_VALUE_MAX_LENGTH)
    return MappedScreenView(sanitized, sanitized)
}

/** Firebase's `setUserId` accepts up to 256 characters. This contract's own
 * `user_id` is an opaque, application-controlled identifier with no documented
 * length ceiling, so truncation here is a defensive fallback for a pathological
 * input, not an expected path. */
fun FirebaseInfinityForgeMapping.sanitizeUserId(userId: String): String =
    truncate(userId, USER_ID_MAX_LENGTH)
