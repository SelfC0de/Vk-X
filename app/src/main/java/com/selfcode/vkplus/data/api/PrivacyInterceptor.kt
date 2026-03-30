package com.selfcode.vkplus.data.api

import com.selfcode.vkplus.data.local.SettingsStore
import com.selfcode.vkplus.data.local.TypeStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private val TELEMETRY_DOMAINS = listOf("stats.vk-portal.net", "tns-counter.ru", "counter.yadro.ru", "mc.yandex.ru")
private val TELEMETRY_METHODS = listOf("stats.trackEvents", "stats.addPost", "auth.saveMetrics")

private val CLIENT_IDS = listOf(
    2685278,   // Kate Mobile
    2274003,   // VK Android
    3140623,   // VK iPhone
    3697615    // VK Windows Phone
)

@Singleton
class PrivacyInterceptor @Inject constructor(
    private val settingsStore: SettingsStore
) : Interceptor {

    private val clientIdIndex = AtomicInteger(0)
    private val requestCount = AtomicInteger(0)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val host = request.url.host
        val method = url.substringAfter("method/").substringBefore("?")

        val unRead        = runBlocking { settingsStore.unRead.first() }
        val unType        = runBlocking { settingsStore.unType.first() }
        val ghostOnline   = runBlocking { settingsStore.ghostOnline.first() }
        val antiTelemetry = runBlocking { settingsStore.antiTelemetry.first() }
        val ghostStory    = runBlocking { settingsStore.ghostStory.first() }
        val typeStatus    = runBlocking { settingsStore.typeStatus.first() }
        val silentVm      = runBlocking { settingsStore.silentVm.first() }
        val antiBan       = runBlocking { settingsStore.antiBan.first() }
        val deviceUa      = runBlocking { settingsStore.deviceUa.first() }

        if (antiTelemetry) {
            if (TELEMETRY_DOMAINS.any { host.contains(it) }) return fakeOk(chain, """{"response":1}""")
            if (TELEMETRY_METHODS.any { method == it }) return fakeOk(chain, """{"response":1}""")
        }

        if (unRead && method == "messages.markAsRead") return fakeOk(chain, """{"response":1}""")
        if (silentVm && method == "messages.markAsListened") return fakeOk(chain, """{"response":1}""")
        if (ghostStory && method == "stories.markAsViewed") return fakeOk(chain, """{"response":1}""")
        if (ghostOnline && method == "account.setOnline") return fakeOk(chain, """{"response":1}""")

        if (method == "messages.setActivity") {
            val incomingType = request.url.queryParameter("type")
            if (incomingType == "typing") {
                return when (typeStatus) {
                    TypeStatus.NONE.value -> fakeOk(chain, """{"response":1}""")
                    "typing" -> chain.proceed(request.newBuilder().header("User-Agent", deviceUa).build())
                    else -> {
                        val newUrl = request.url.newBuilder().setQueryParameter("type", typeStatus).build()
                        chain.proceed(request.newBuilder().url(newUrl).header("User-Agent", deviceUa).build())
                    }
                }
            }
        }

        // Anti-Ban: rotate client_id every 50 requests
        var finalRequest = request.newBuilder().header("User-Agent", deviceUa).build()
        if (antiBan && method.isNotEmpty()) {
            val count = requestCount.incrementAndGet()
            if (count % 50 == 0) {
                val nextIdx = (clientIdIndex.incrementAndGet()) % CLIENT_IDS.size
                clientIdIndex.set(nextIdx)
            }
            val currentClientId = CLIENT_IDS[clientIdIndex.get()]
            val newUrl = request.url.newBuilder()
                .setQueryParameter("client_id", currentClientId.toString())
                .build()
            finalRequest = request.newBuilder()
                .url(newUrl)
                .header("User-Agent", deviceUa)
                .build()
        }

        val response = chain.proceed(finalRequest)

        // Anti-Ban: on error 14 (captcha) or 29 (rate limit) — rotate immediately
        if (antiBan && (response.code == 200)) {
            val body = response.peekBody(512)
            val bodyStr = body.string()
            if (bodyStr.contains("\"error_code\":14") || bodyStr.contains("\"error_code\":29")) {
                val nextIdx = (clientIdIndex.incrementAndGet()) % CLIENT_IDS.size
                clientIdIndex.set(nextIdx)
            }
        }

        return response
    }

    private fun fakeOk(chain: Interceptor.Chain, json: String): Response =
        Response.Builder().code(200).message("OK")
            .request(chain.request()).protocol(Protocol.HTTP_1_1)
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
}
