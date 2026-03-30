package com.selfcode.vkplus.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vkplus_prefs")

@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("access_token")
        private val KEY_USER_ID = intPreferencesKey("user_id")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userId: Flow<Int?> = context.dataStore.data.map { it[KEY_USER_ID] }

    suspend fun saveToken(token: String, userId: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.clear() }
    }
}
