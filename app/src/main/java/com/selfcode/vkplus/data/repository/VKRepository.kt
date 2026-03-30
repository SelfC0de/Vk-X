package com.selfcode.vkplus.data.repository

import com.selfcode.vkplus.data.api.VKApi
import com.selfcode.vkplus.data.api.VKNewsfeedResponse
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
        val data = resp.response ?: return VKResult.Error("Empty response")
        VKResult.Success(data)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getFriends(): VKResult<VKFriendsResponse> = runCatching {
        val resp = api.getFriends(token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val data = resp.response ?: return VKResult.Error("Empty response")
        VKResult.Success(data)
    }.getOrElse { VKResult.Error(it.message ?: "Unknown error") }

    suspend fun getConversations(): VKResult<com.selfcode.vkplus.data.api.VKConversationsResponse> = runCatching {
        val resp = api.getConversations(token = token())
        if (resp.error != null) return VKResult.Error(resp.error.message, resp.error.code)
        val data = resp.response ?: return VKResult.Error("Empty response")
        VKResult.Success(data)
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
}
