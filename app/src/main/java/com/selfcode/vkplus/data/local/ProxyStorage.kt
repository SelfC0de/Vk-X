package com.selfcode.vkplus.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.proxyDataStore by preferencesDataStore("vkplus_proxy")

data class ProxyConfig(
    val id: String = "",
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 443,
    val type: String = "SOCKS5",
    val user: String = "",
    val pass: String = "",
    val secret: String = "",
    val pingMs: Long = -1L
)

@Singleton
class ProxyStorage @Inject constructor(@ApplicationContext private val ctx: Context) {

    private val gson = Gson()

    companion object {
        val KEY_ACTIVE_ID = stringPreferencesKey("proxy_active_id")
        val KEY_LIST      = stringPreferencesKey("proxy_list")
    }

    val activeId: Flow<String> = ctx.proxyDataStore.data.map { it[KEY_ACTIVE_ID] ?: "" }

    val list: Flow<List<ProxyConfig>> = ctx.proxyDataStore.data.map { p ->
        val json = p[KEY_LIST] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<ProxyConfig>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // Активный прокси = первый с id == activeId
    val activeConfig: Flow<ProxyConfig?> = ctx.proxyDataStore.data.map { p ->
        val activeId = p[KEY_ACTIVE_ID] ?: ""
        if (activeId.isBlank()) return@map null
        val json = p[KEY_LIST] ?: return@map null
        try {
            val type = object : TypeToken<List<ProxyConfig>>() {}.type
            val list: List<ProxyConfig> = gson.fromJson(json, type) ?: return@map null
            list.find { it.id == activeId }
        } catch (e: Exception) { null }
    }

    suspend fun addProxy(cfg: ProxyConfig) {
        ctx.proxyDataStore.edit { p ->
            val current = parseList(p[KEY_LIST])
            val newId = System.currentTimeMillis().toString()
            val newList = current + cfg.copy(id = newId)
            p[KEY_LIST] = gson.toJson(newList)
        }
    }

    suspend fun removeProxy(id: String) {
        ctx.proxyDataStore.edit { p ->
            val current = parseList(p[KEY_LIST]).filter { it.id != id }
            p[KEY_LIST] = gson.toJson(current)
            if (p[KEY_ACTIVE_ID] == id) p[KEY_ACTIVE_ID] = ""
        }
    }

    suspend fun setActive(id: String) {
        ctx.proxyDataStore.edit { it[KEY_ACTIVE_ID] = id }
    }

    suspend fun updatePing(id: String, pingMs: Long) {
        ctx.proxyDataStore.edit { p ->
            val current = parseList(p[KEY_LIST]).map {
                if (it.id == id) it.copy(pingMs = pingMs) else it
            }
            p[KEY_LIST] = gson.toJson(current)
        }
    }

    private fun parseList(json: String?): List<ProxyConfig> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<ProxyConfig>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
