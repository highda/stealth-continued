package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

data class DataPreferences(
    val enablePrivacyEnhancer: Boolean
) {
    object PreferencesKeys {
        val PRIVACY_ENHANCER = booleanPreferencesKey("privacy_enhancer")
    }
}
