plugins {
    alias(libs.plugins.factory.android.library)
    alias(libs.plugins.factory.hilt)
}

android {
    namespace = "com.factory.core.datastore"
}

dependencies {
    // api, not implementation: PreferencesDataSource's public signature exposes
    // Preferences.Key<T>, so consumers (feature-settings, feature-onboarding) need it
    // on their own compile classpath.
    api(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
