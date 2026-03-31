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
