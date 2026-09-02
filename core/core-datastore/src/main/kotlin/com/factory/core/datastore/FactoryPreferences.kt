package com.factory.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.factoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "factory_preferences",
)

/**
 * Typed preference keys live here, not scattered across feature modules, so it's
 * always obvious what this app persists to disk and there is exactly one
 * `preferencesDataStore` delegate for the whole app (a second one on the same file name
 * would throw at runtime).
 */
object PreferenceKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    // InfinityForge Tracking identity (specification/identity.md), owned by
    // core-tracking's InfinityForgeIdentity. Kept here, not in core-tracking itself,
    // matching this file's own rule: exactly one preferencesDataStore-backed key
    // registry for the whole app. An empty string is this trio's "absent" sentinel
    // (identify() already rejects an empty user_id, so it can never mean "identified
    // with a blank id"; user properties default to "" meaning "no persisted JSON yet").
    val TRACKING_ANONYMOUS_ID = stringPreferencesKey("infinityforge_tracking_anonymous_id")
    val TRACKING_USER_ID = stringPreferencesKey("infinityforge_tracking_user_id")
    val TRACKING_USER_PROPERTIES = stringPreferencesKey("infinityforge_tracking_user_properties")
}

/**
 * Thin, testable wrapper around the [DataStore] so calling code depends on this
 * interface (fakeable via `core-testing`'s `FakePreferencesDataSource`) instead of a
 * concrete `DataStore<Preferences>` instance.
 */
interface PreferencesDataSource {
    fun stringFlow(key: Preferences.Key<String>, default: String): Flow<String>
    fun booleanFlow(key: Preferences.Key<Boolean>, default: Boolean): Flow<Boolean>
    suspend fun setString(key: Preferences.Key<String>, value: String)
    suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean)
}

class DataStorePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesDataSource {

    override fun stringFlow(key: Preferences.Key<String>, default: String): Flow<String> =
        dataStore.data.map { it[key] ?: default }

    override fun booleanFlow(key: Preferences.Key<Boolean>, default: Boolean): Flow<Boolean> =
        dataStore.data.map { it[key] ?: default }

    override suspend fun setString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    override suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.factoryDataStore

    @Provides
    @Singleton
    fun providePreferencesDataSource(
        dataStore: DataStore<Preferences>,
    ): PreferencesDataSource = DataStorePreferencesDataSource(dataStore)
}
