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

        val unRead        = runBlocking { settingsStore.unRead.first() }
        val unType        = runBlocking { settingsStore.unType.first() }
        val ghostOnline   = runBlocking { settingsStore.ghostOnline.first() }
        val antiTelemetry = runBlocking { settingsStore.antiTelemetry.first() }
        val ghostStory    = runBlocking { settingsStore.ghostStory.first() }
        val typeStatus    = runBlocking { settingsStore.typeStatus.first() }
        val deviceUa      = runBlocking { settingsStore.deviceUa.first() }

        // Anti-Telemetry
        if (antiTelemetry) {
            if (TELEMETRY_DOMAINS.any { host.contains(it) })
                return fakeOk(chain, """{"response":1}""")
            if (TELEMETRY_METHODS.any { method == it })
                return fakeOk(chain, """{"response":1}""")
        }

        // UnRead
        if (unRead && method == "messages.markAsRead")
            return fakeOk(chain, """{"response":1}""")

        // Ghost View Story
        if (ghostStory && method == "stories.markAsViewed")
            return fakeOk(chain, """{"response":1}""")

        // Ghost Online
        if (ghostOnline && method == "account.setOnline")
            return fakeOk(chain, """{"response":1}""")

        // UnType / Type Status Changer
        if (method == "messages.setActivity") {
            val incomingType = request.url.queryParameter("type")
            if (incomingType == "typing") {
                return when (typeStatus) {
                    TypeStatus.NONE.value -> fakeOk(chain, """{"response":1}""") // UnType — block
                    "typing" -> chain.proceed(request.newBuilder().header("User-Agent", deviceUa).build()) // pass through
                    else -> {
                        // Substitute type
                        val newUrl = request.url.newBuilder()
                            .setQueryParameter("type", typeStatus)
                            .build()
                        val newRequest = request.newBuilder()
                            .url(newUrl)
                            .header("User-Agent", deviceUa)
                            .build()
                        chain.proceed(newRequest)
                    }
                }
            }
        }

        return chain.proceed(
            request.newBuilder().header("User-Agent", deviceUa).build()
        )
    }

    private fun fakeOk(chain: Interceptor.Chain, json: String): Response =
        Response.Builder()
            .code(200).message("OK")
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
}
