package com.factory.core.designsystem.theme

/** Persisted via `core-datastore` as a plain string (`ThemeMode.name`); UI-only concept. */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        fun fromStorageValue(value: String?): ThemeMode =
            entries.find { it.name == value } ?: SYSTEM
    }
}
