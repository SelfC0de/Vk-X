package com.selfcode.vkplus.data.api

import com.selfcode.vkplus.data.local.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

private val TELEMETRY_DOMAINS = listOf(
    "stats.vk-portal.net",
    "tns-counter.ru",
    "counter.yadro.ru",
    "mc.yandex.ru"
)

private val TELEMETRY_METHODS = listOf(
    "stats.trackEvents",
    "stats.addPost",
    "auth.saveMetrics"
)

@Singleton
class PrivacyInterceptor @Inject constructor(
    private val settingsStore: SettingsStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val host = request.url.host
        val method = url.substringAfter("method/").substringBefore("?")

        val unRead         = runBlocking { settingsStore.unRead.first() }
        val unType         = runBlocking { settingsStore.unType.first() }
        val ghostOnline    = runBlocking { settingsStore.ghostOnline.first() }
        val antiTelemetry  = runBlocking { settingsStore.antiTelemetry.first() }
        val deviceUa       = runBlocking { settingsStore.deviceUa.first() }

        if (antiTelemetry) {
            if (TELEMETRY_DOMAINS.any { host.contains(it) }) {
                return fakeOk(chain, """{"response":1}""")
            }
            if (TELEMETRY_METHODS.any { method == it }) {
                return fakeOk(chain, """{"response":1}""")
            }
        }

        if (unRead && method == "messages.markAsRead") {
            return fakeOk(chain, """{"response":1}""")
        }

        if (unType && method == "messages.setActivity") {
            if (request.url.queryParameter("type") == "typing") {
                return fakeOk(chain, """{"response":1}""")
            }
        }

        if (ghostOnline && method == "account.setOnline") {
            return fakeOk(chain, """{"response":1}""")
        }

        val newRequest = request.newBuilder()
            .header("User-Agent", deviceUa)
            .build()

        return chain.proceed(newRequest)
    }

    private fun fakeOk(chain: Interceptor.Chain, json: String): Response =
        Response.Builder()
            .code(200)
            .message("OK")
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
}
