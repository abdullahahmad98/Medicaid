package com.example.medicaid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "whisper_preferences")

class WhisperPreferencesManager(private val context: Context) {

    companion object {
        private val SELECTED_MODEL_KEY = stringPreferencesKey("selected_model")
        private const val DEFAULT_MODEL = "base"
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MODEL_KEY] ?: DEFAULT_MODEL
    }

    suspend fun setSelectedModel(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MODEL_KEY] = modelName
        }
    }

    suspend fun getSelectedModel(): String {
        return context.dataStore.data.map { preferences ->
            preferences[SELECTED_MODEL_KEY] ?: DEFAULT_MODEL
        }.collect { it }
    }
}
