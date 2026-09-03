package com.factory.core.testing

import androidx.datastore.preferences.core.Preferences
import com.factory.core.datastore.PreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [PreferencesDataSource] fake — promised by `FactoryPreferences.kt`'s own doc
 * comment ("fakeable via core-testing's FakePreferencesDataSource"), added here for
 * InfinityForgeIdentityTest (core-tracking) since no test previously exercised
 * `PreferencesDataSource` directly. No disk I/O, no `Context` required.
 */
class FakePreferencesDataSource : PreferencesDataSource {
    private val strings = MutableStateFlow<Map<Preferences.Key<String>, String>>(emptyMap())
    private val booleans = MutableStateFlow<Map<Preferences.Key<Boolean>, Boolean>>(emptyMap())

    override fun stringFlow(key: Preferences.Key<String>, default: String) =
        strings.map { it[key] ?: default }

    override fun booleanFlow(key: Preferences.Key<Boolean>, default: Boolean) =
        booleans.map { it[key] ?: default }

    override suspend fun setString(key: Preferences.Key<String>, value: String) {
        strings.value = strings.value + Pair(key, value)
    }

    override suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        booleans.value = booleans.value + Pair(key, value)
    }
}
