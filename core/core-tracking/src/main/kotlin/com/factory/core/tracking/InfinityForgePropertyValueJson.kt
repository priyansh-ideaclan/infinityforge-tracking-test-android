package com.factory.core.tracking

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure conversion between [InfinityForgePropertyValue] and kotlinx.serialization's
 * [JsonElement] tree — used only to persist `setUserProperties()` state to disk
 * (`InfinityForgeIdentity`) via `kotlinx.serialization.json.Json`'s built-in
 * `JsonElement` (de)serializer. Not used for any envelope emitted on the wire — the
 * wire shape is [InfinityForgeEventEnvelope]/[InfinityForgeMetricEnvelope] directly,
 * never a JSON string.
 */
internal fun InfinityForgePropertyValue.toJsonElement(): JsonElement = when (this) {
    is InfinityForgePropertyValue.StringValue -> JsonPrimitive(value)
    is InfinityForgePropertyValue.IntValue -> JsonPrimitive(value)
    is InfinityForgePropertyValue.NumberValue -> JsonPrimitive(value)
    is InfinityForgePropertyValue.BooleanValue -> JsonPrimitive(value)
    is InfinityForgePropertyValue.ArrayValue -> JsonArray(value.map { it.toJsonElement() })
    is InfinityForgePropertyValue.ObjectValue -> JsonObject(value.mapValues { it.value.toJsonElement() })
}

/**
 * The inverse of [toJsonElement]. A malformed/foreign JSON value that cannot be mapped
 * (for example, a bare JSON `null`, which this contract never emits — see
 * specification/conventions.md) is dropped rather than thrown — callers
 * ([InfinityForgeIdentity]) treat an unreadable persisted value as "no value", never as
 * a crash.
 */
internal fun JsonElement.toPropertyValueOrNull(): InfinityForgePropertyValue? = when (this) {
    is JsonNull -> null
    is JsonObject -> InfinityForgePropertyValue.ObjectValue(
        this.jsonObject.mapNotNull { (key, element) -> element.toPropertyValueOrNull()?.let { key to it } }.toMap(),
    )
    is JsonArray -> InfinityForgePropertyValue.ArrayValue(this.jsonArray.mapNotNull { it.toPropertyValueOrNull() })
    is JsonPrimitive -> {
        val primitive = this.jsonPrimitive
        val boolean = primitive.booleanOrNull
        val integer = primitive.intOrNull
        val number = primitive.doubleOrNull
        when {
            primitive.isString -> InfinityForgePropertyValue.StringValue(primitive.content)
            boolean != null -> InfinityForgePropertyValue.BooleanValue(boolean)
            integer != null -> InfinityForgePropertyValue.IntValue(integer)
            number != null -> InfinityForgePropertyValue.NumberValue(number)
            else -> null
        }
    }
}
