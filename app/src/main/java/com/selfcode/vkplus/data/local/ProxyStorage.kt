package com.selfcode.vkplus.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.proxyDataStore by preferencesDataStore("vkplus_proxy")

data class ProxyConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 443,
    val type: String = "SOCKS5", // SOCKS5, HTTP
    val user: String = "",
    val pass: String = "",
    val secret: String = "" // MTProto secret (decoded)
)

@Singleton
class ProxyStorage @Inject constructor(@ApplicationContext private val ctx: Context) {

    companion object {
        val KEY_ENABLED = booleanPreferencesKey("proxy_enabled")
        val KEY_HOST    = stringPreferencesKey("proxy_host")
        val KEY_PORT    = intPreferencesKey("proxy_port")
        val KEY_TYPE    = stringPreferencesKey("proxy_type")
        val KEY_USER    = stringPreferencesKey("proxy_user")
        val KEY_PASS    = stringPreferencesKey("proxy_pass")
        val KEY_SECRET  = stringPreferencesKey("proxy_secret")
    }

    val config: Flow<ProxyConfig> = ctx.proxyDataStore.data.map { p ->
        ProxyConfig(
            enabled = p[KEY_ENABLED] ?: false,
            host    = p[KEY_HOST]    ?: "",
            port    = p[KEY_PORT]    ?: 443,
            type    = p[KEY_TYPE]    ?: "SOCKS5",
            user    = p[KEY_USER]    ?: "",
            pass    = p[KEY_PASS]    ?: "",
            secret  = p[KEY_SECRET]  ?: ""
        )
    }

    suspend fun save(cfg: ProxyConfig) {
        ctx.proxyDataStore.edit { p ->
            p[KEY_ENABLED] = cfg.enabled
            p[KEY_HOST]    = cfg.host
            p[KEY_PORT]    = cfg.port
            p[KEY_TYPE]    = cfg.type
            p[KEY_USER]    = cfg.user
            p[KEY_PASS]    = cfg.pass
            p[KEY_SECRET]  = cfg.secret
        }
    }

    suspend fun setEnabled(v: Boolean) {
        ctx.proxyDataStore.edit { it[KEY_ENABLED] = v }
    }
}
