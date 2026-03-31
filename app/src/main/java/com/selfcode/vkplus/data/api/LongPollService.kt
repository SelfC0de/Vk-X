package com.selfcode.vkplus.data.api

import com.selfcode.vkplus.data.local.SettingsStore
import com.selfcode.vkplus.data.local.TokenStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class LongPollMessage(
    val peerId: Int,
    val fromId: Int,
    val text: String,
    val timestamp: Long
)

@Singleton
class LongPollService @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val settingsStore: SettingsStore,
    private val api: VKApi,
    private val httpClient: OkHttpClient
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messages = MutableSharedFlow<LongPollMessage>(replay = 0, extraBufferCapacity = 64)
    val messages: SharedFlow<LongPollMessage> = _messages

    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch { runLoop() }
    }

    fun stop() {
        isRunning = false
    }

    private suspend fun runLoop() {
        val token = tokenStorage.accessToken.first() ?: return
        try {
            // Get LongPoll server
            val serverResp = api.getLongPollServer(token = token)
            val server = serverResp.response ?: return
            var ts = server.ts
            val key = server.key
            val serverUrl = server.server

            while (isRunning) {
                val longPollOnly = settingsStore.longPollOnly.first()
                val url = "https://$serverUrl?act=a_check&key=$key&ts=$ts&wait=25&mode=2&version=3"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: continue

                val json = JSONObject(body)
                if (json.has("failed")) {
                    // Reconnect on failure
                    break
                }

                ts = json.getString("ts")
                val updates = json.optJSONArray("updates") ?: continue

                for (i in 0 until updates.length()) {
                    val update = updates.getJSONArray(i)
                    val code = update.getInt(0)
                    // code 4 = new message
                    if (code == 4) {
                        val msgId   = update.getInt(1)
                        val flags   = update.getInt(2)
                        val peerId  = update.getInt(3)
                        val ts2     = update.getLong(4)
                        val text    = update.optString(5, "")
                        val fromId  = if (peerId > 2000000000) update.optJSONObject(6)?.optInt("from", peerId) ?: peerId else peerId

                        _messages.emit(LongPollMessage(peerId, fromId, text, ts2))
                    }
                }
            }
        } catch (e: Exception) {
            // Retry after delay
            delay(5000)
            if (isRunning) runLoop()
        }
    }
}
