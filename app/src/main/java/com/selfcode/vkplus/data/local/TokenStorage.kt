package com.selfcode.vkplus.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vkplus_prefs")

data class SavedAccount(
    val userId: Int,
    val token: String,
    val name: String = "",
    val photo: String = ""
)

@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    companion object {
        private val KEY_TOKEN      = stringPreferencesKey("access_token")
        private val KEY_USER_ID    = intPreferencesKey("user_id")
        private val KEY_ACCOUNTS   = stringPreferencesKey("saved_accounts")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userId: Flow<Int?>         = context.dataStore.data.map { it[KEY_USER_ID] }

    val savedAccounts: Flow<List<SavedAccount>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_ACCOUNTS] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<SavedAccount>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun saveToken(token: String, userId: Int, name: String = "", photo: String = "") {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN]   = token
            prefs[KEY_USER_ID] = userId
            // Update saved accounts list
            val current = getSavedAccountsSync(prefs)
            val updated = current.toMutableList()
            val idx = updated.indexOfFirst { it.userId == userId }
            val account = SavedAccount(userId, token, name, photo)
            if (idx >= 0) updated[idx] = account else updated.add(0, account)
            prefs[KEY_ACCOUNTS] = gson.toJson(updated)
        }
    }

    suspend fun switchAccount(account: SavedAccount) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN]   = account.token
            prefs[KEY_USER_ID] = account.userId
            // Move to front of list
            val current = getSavedAccountsSync(prefs).toMutableList()
            current.removeIf { it.userId == account.userId }
            current.add(0, account)
            prefs[KEY_ACCOUNTS] = gson.toJson(current)
        }
    }

    suspend fun updateAccountInfo(userId: Int, name: String, photo: String) {
        context.dataStore.edit { prefs ->
            val current = getSavedAccountsSync(prefs).toMutableList()
            val idx = current.indexOfFirst { it.userId == userId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(name = name, photo = photo)
                prefs[KEY_ACCOUNTS] = gson.toJson(current)
            }
        }
    }

    suspend fun removeAccount(userId: Int) {
        context.dataStore.edit { prefs ->
            val current = getSavedAccountsSync(prefs).toMutableList()
            current.removeIf { it.userId == userId }
            prefs[KEY_ACCOUNTS] = gson.toJson(current)
            // If removed current account, switch to next or clear
            val currentId = prefs[KEY_USER_ID]
            if (currentId == userId) {
                if (current.isNotEmpty()) {
                    prefs[KEY_TOKEN]   = current[0].token
                    prefs[KEY_USER_ID] = current[0].userId
                } else {
                    prefs.remove(KEY_TOKEN)
                    prefs.remove(KEY_USER_ID)
                }
            }
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER_ID)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun getSavedAccountsSync(prefs: Preferences): List<SavedAccount> {
        val json = prefs[KEY_ACCOUNTS] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedAccount>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
