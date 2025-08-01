package com.ahm.mydalil.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahm.mydalil.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

class UserPreferences(private val context: Context) {

    private object Keys {
        val SELECTED_SURAHS = stringSetPreferencesKey("selected_surahs")
        val ALL_WORDS_REQUIRED = booleanPreferencesKey("all_words_required")
        val LOAD_HADITHS = booleanPreferencesKey("load_hadiths")
        val LOAD_VERSES = booleanPreferencesKey("load_verses")
    }

    val selectedSurahs: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_SURAHS] ?: emptySet()
    }

    val allWordsRequired: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ALL_WORDS_REQUIRED] ?: true // Default to true
    }

    val loadHadiths: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOAD_HADITHS] ?: true // Default to true
    }

    val loadVerses: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOAD_VERSES] ?: true // Default to true
    }

    suspend fun saveSelectedSurahs(surahs: Set<String>) {
        context.dataStore.edit { it[Keys.SELECTED_SURAHS] = surahs }
    }

    suspend fun saveAllWordsRequired(isRequired: Boolean) {
        context.dataStore.edit { it[Keys.ALL_WORDS_REQUIRED] = isRequired }
    }

    suspend fun saveFileSelection(loadHadiths: Boolean, loadVerses: Boolean) {
        context.dataStore.edit {
            it[Keys.LOAD_HADITHS] = loadHadiths
            it[Keys.LOAD_VERSES] = loadVerses
        }
    }
}
