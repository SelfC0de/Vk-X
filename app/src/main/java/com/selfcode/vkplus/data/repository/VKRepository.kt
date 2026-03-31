package com.selfcode.vkplus.data.repository

import com.google.gson.Gson
import com.selfcode.vkplus.data.api.VKApi
import com.selfcode.vkplus.data.api.VKConversationsResponse
import com.selfcode.vkplus.data.api.VKNewsfeedResponse
import com.selfcode.vkplus.data.api.VKQrCheckResponse
import com.selfcode.vkplus.data.api.VKQrCodeResponse
import com.selfcode.vkplus.data.local.TokenStorage
import com.selfcode.vkplus.data.model.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed class VKResult<out T> {
    data class Success<T>(val data: T) : VKResult<T>()
    data class Error(val message: String, val code: Int = 0) : VKResult<Nothing>()
}

@Singleton
class VKRepository @Inject constructor(
    private val api: VKApi,
    private val tokenStorage: TokenStorage
) {
    private suspend fun token(): String = tokenStorage.accessToken.first() ?: ""

    suspend fun getCurrentUser(): VKResult<VKUser> = runCatching {
        val resp = api.getUsers(token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val user = resp.response?.firstOrNull() ?: return VKResult.Error("Empty response")
        VKResult.Success(user)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getNewsfeed(startFrom: String? = null): VKResult<VKNewsfeedResponse> = runCatching {
        val resp = api.getNewsfeed(token = token(), startFrom = startFrom)
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: return VKResult.Error("Empty response"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getFriends(): VKResult<VKFriendsResponse> = runCatching {
        val resp = api.getFriends(token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: return VKResult.Error("Empty response"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getConversations(): VKResult<VKConversationsResponse> = runCatching {
        val resp = api.getConversations(token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: return VKResult.Error("Empty response"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getMessagesExecute(peerId: Int): VKResult<List<VKMessage>> = runCatching {
        val t = token()
        // Wrap in execute to avoid triggering setOnline
        val code = """API.messages.getHistory({"peer_id":$peerId,"count":50,"v":"5.199","access_token":"$t"})"""
        val resp = api.execute(code = code, token = t)
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val json = resp.response?.toString() ?: return VKResult.Error("Empty response")
        val parsed = Gson().fromJson(json, com.selfcode.vkplus.data.api.VKHistoryResponse::class.java)
        VKResult.Success(parsed.items)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getMessages(peerId: Int): VKResult<List<VKMessage>> = runCatching {
        val resp = api.getHistory(peerId = peerId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response?.items ?: emptyList())
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun deleteMessage(peerId: Int, messageId: Int): VKResult<Unit> = runCatching {
        val resp = api.deleteMessage(messageIds = messageId.toString(), peerId = peerId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(Unit)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun editMessage(peerId: Int, messageId: Int, text: String): VKResult<Int> = runCatching {
        val resp = api.editMessage(peerId = peerId, messageId = messageId, message = text, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: 0)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun sendMessage(peerId: Int, text: String): VKResult<Int> = runCatching {
        val resp = api.sendMessage(peerId = peerId, message = text, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: 0)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun toggleLike(post: VKPost): VKResult<Int> = runCatching {
        val t = token()
        val resp = if (post.likes?.isLiked == true) {
            api.deleteLike(ownerId = post.ownerId, itemId = post.id, token = t)
        } else {
            api.addLike(ownerId = post.ownerId, itemId = post.id, token = t)
        }
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response?.likes ?: 0)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getUserExtended(userId: String): VKResult<com.selfcode.vkplus.data.api.VKUserExtended> = runCatching {
        val resp = api.getUserExtended(userIds = userId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response?.firstOrNull() ?: return VKResult.Error("Not found"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getUserById(userId: String): VKResult<com.selfcode.vkplus.data.model.VKUser> = runCatching {
        val resp = api.getUsers(userIds = userId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response?.firstOrNull() ?: return VKResult.Error("Not found"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getUserBypassAdult(userId: Int): VKResult<com.selfcode.vkplus.data.model.VKUser> = runCatching {
        // Use desktop UA to bypass adult content filter
        val resp = api.getUsers(userIds = userId.toString(), token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response?.firstOrNull() ?: return VKResult.Error("Not found"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getUserStickers(userId: Int): VKResult<List<String>> = runCatching {
        val resp = api.getUserStickers(userId = userId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val packs = resp.response?.items?.map { it.pack?.title?.takeIf { t -> t.isNotBlank() } ?: it.title.takeIf { t -> t.isNotBlank() } ?: "Pack #${it.id}" } ?: emptyList()
        VKResult.Success(if (packs.isEmpty()) listOf("Стикерпаки не найдены или доступ закрыт") else packs)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun checkGroupMembership(userId: Int, groupId: Int): VKResult<Pair<Boolean, String>> = runCatching {
        val memberResp = api.isGroupMember(groupId = groupId, userId = userId, token = token())
        val isMember = memberResp.response == 1
        val groupResp = api.getGroupById(groupId = groupId, token = token())
        val groupName = groupResp.response?.firstOrNull()?.name ?: "Группа $groupId"
        VKResult.Success(Pair(isMember, groupName))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getStickerKeywords(userId: Int): VKResult<List<String>> = runCatching {
        val resp = api.getStickerKeywords(userId = userId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val items = resp.response?.items ?: emptyList()
        if (items.isEmpty()) return VKResult.Success(listOf("Данные не получены — метод ограничен для этого юзера"))
        // Collect unique pack titles from user_stickers
        val packs = items.flatMap { it.userStickers }
            .mapNotNull { it.product?.title?.takeIf { t -> t.isNotBlank() } }
            .distinct()
        val keywords = items.take(5).flatMap { it.words }.distinct().take(10)
        val result = mutableListOf<String>()
        if (packs.isNotEmpty()) result.add("🎭 Стикерпаки (${packs.size}):
" + packs.joinToString("
") { "• $it" })
        if (keywords.isNotEmpty()) result.add("🔑 Ключевые слова: " + keywords.joinToString(", "))
        if (result.isEmpty()) result.add("Стикеры найдены, но данные скрыты или пусты")
        VKResult.Success(result)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun executeScript(code: String): VKResult<String> = runCatching {
        val resp = api.execute(code = code, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val raw = resp.response?.toString() ?: "null"
        // Parse user last_seen from response
        val result = StringBuilder()
        try {
            val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
            val users = json.getAsJsonArray("user")
            if (users != null && users.size() > 0) {
                val user = users[0].asJsonObject
                val firstName = user.get("first_name")?.asString ?: ""
                val lastName = user.get("last_name")?.asString ?: ""
                result.appendLine("👤 $firstName $lastName")
                val lastSeen = user.getAsJsonObject("last_seen")
                if (lastSeen != null) {
                    val time = lastSeen.get("time")?.asLong ?: 0L
                    val platform = lastSeen.get("platform")?.asInt ?: 0
                    val platformName = when(platform) { 1 -> "мобильный сайт" 2 -> "iPhone" 3 -> "iPad" 4 -> "Android" 5 -> "Windows Phone" 6 -> "Windows 10" 7 -> "сайт" else -> "неизвестно" }
                    val dateStr = java.text.SimpleDateFormat("d MMM HH:mm:ss", java.util.Locale("ru")).format(java.util.Date(time * 1000))
                    result.appendLine("🕐 Последний визит: $dateStr")
                    result.appendLine("📱 Платформа: $platformName")
                } else {
                    result.appendLine("🕐 last_seen: скрыт")
                }
            }
            val commonCount = json.get("common_count")?.asInt ?: 0
            result.appendLine("👥 Общих друзей: $commonCount")
        } catch (e: Exception) {
            result.appendLine("Сырой ответ: $raw")
        }
        VKResult.Success(result.toString().trim())
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getLegacyNewsfeed(): VKResult<List<String>> = runCatching {
        val resp = api.getLegacyNewsfeed(token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val items = resp.response?.items ?: emptyList()
        val profiles = resp.response?.profiles?.associateBy { it.id } ?: emptyMap()
        val result = items.take(15).map { item ->
            val author = profiles[item.sourceId]?.fullName ?: "id${item.sourceId}"
            val text = item.text.take(100).ifBlank { "(без текста)" }
            "[$author]
$text"
        }
        VKResult.Success(if (result.isEmpty()) listOf("Лента пуста") else result)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getChatMembers(chatId: Int): VKResult<List<String>> = runCatching {
        val resp = api.getChat(chatId = chatId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val chat = resp.response ?: return VKResult.Error("Пустой ответ")
        val result = mutableListOf("💬 ${chat.title}")
        chat.users.forEach { member ->
            val role = if (member.isAdmin) "👑 Админ" else "👤 Участник"
            val online = if (member.online == 1) " 🟢" else ""
            result.add("$role: ${member.fullName} (id${member.id})$online")
        }
        VKResult.Success(result)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getBannedFriends(): VKResult<List<com.selfcode.vkplus.data.model.VKUser>> = runCatching {
        val resp = api.getFriendsWithStatus(token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val banned = (resp.response?.items ?: emptyList()).filter { it.isBanned }
        VKResult.Success(banned)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun removeFriend(userId: Int): VKResult<Int> = runCatching {
        val resp = api.deleteFriend(userId = userId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(1)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getGifts(userId: Int? = null): VKResult<com.selfcode.vkplus.data.api.VKGiftsResponse> = runCatching {
        val resp = api.getGifts(userId = userId, token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: return VKResult.Error("Empty response"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun setOffline(): VKResult<Int> = runCatching {
        val resp = api.setOffline(token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: 0)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getQrCode(): VKResult<VKQrCodeResponse> = runCatching {
        val resp = api.getQrCode(clientId = 2685278)
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: return VKResult.Error("Empty response"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun checkQrCode(authHash: String): VKResult<VKQrCheckResponse> = runCatching {
        val resp = api.checkQrCode(authHash = authHash)
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        VKResult.Success(resp.response ?: return VKResult.Error("Empty response"))
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }
}
