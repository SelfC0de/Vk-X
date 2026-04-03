package com.selfcode.vkplus.data.api

import com.selfcode.vkplus.data.local.ProxyConfig
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyManager @Inject constructor(
    private val privacyInterceptor: PrivacyInterceptor,
    private val uaInterceptor: UserAgentSwitcherInterceptor
) {
    fun buildClient(
        cfg: ProxyConfig? = null,
        sniEnabled: Boolean = false,
        uaEnabled: Boolean = false
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(privacyInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })

        // SNI Spoofing
        if (sniEnabled) {
            try {
                val (sslFactory, trustManager) = SpoofedSSLSocketFactory.create("cloudflare.com")
                builder.sslSocketFactory(sslFactory, trustManager)
                builder.protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            } catch (e: Exception) {
                // SNI setup failed - continue without it
            }
        }

        // User-Agent Switcher
        if (uaEnabled) {
            builder.addInterceptor(uaInterceptor)
        }

        if (cfg != null && cfg.enabled && cfg.host.isNotBlank()) {
            val proxyType = if (cfg.type == "HTTP") Proxy.Type.HTTP else Proxy.Type.SOCKS
            val proxy = Proxy(proxyType, InetSocketAddress(cfg.host, cfg.port))
            builder.proxy(proxy)
            // Force HTTP/1.1 through proxy to avoid QUIC/UDP blocks
            builder.protocols(listOf(Protocol.HTTP_1_1))

            if (cfg.user.isNotBlank()) {
                builder.proxyAuthenticator(Authenticator { _, response ->
                    if (response.request.header("Proxy-Authorization") != null) return@Authenticator null
                    response.request.newBuilder()
                        .header("Proxy-Authorization", Credentials.basic(cfg.user, cfg.pass))
                        .build()
                })
            }
        }

        return builder.build()
    }
}
