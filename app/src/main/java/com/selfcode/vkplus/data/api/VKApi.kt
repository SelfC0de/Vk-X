package com.selfcode.vkplus.data.api

import com.selfcode.vkplus.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface VKApi {

    @GET("users.get")
    suspend fun getUsers(
        @Query("user_ids") userIds: String? = null,
        @Query("fields") fields: String = "photo_200,photo_100,online,status,city",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<List<VKUser>>

    @GET("newsfeed.get")
    suspend fun getNewsfeed(
        @Query("filters") filters: String = "post",
        @Query("count") count: Int = 30,
        @Query("start_from") startFrom: String? = null,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKNewsfeedResponse>

    @GET("wall.get")
    suspend fun getWall(
        @Query("owner_id") ownerId: Int? = null,
        @Query("count") count: Int = 30,
        @Query("offset") offset: Int = 0,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKWallResponse>

    @GET("friends.get")
    suspend fun getFriends(
        @Query("fields") fields: String = "photo_100,online,status",
        @Query("order") order: String = "hints",
        @Query("count") count: Int = 100,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKFriendsResponse>

    @GET("messages.getConversations")
    suspend fun getConversations(
        @Query("count") count: Int = 30,
        @Query("extended") extended: Int = 1,
        @Query("fields") fields: String = "photo_100,online",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKConversationsResponse>

    @GET("likes.add")
    suspend fun addLike(
        @Query("type") type: String = "post",
        @Query("owner_id") ownerId: Int,
        @Query("item_id") itemId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKLikeResult>

    @GET("likes.delete")
    suspend fun deleteLike(
        @Query("type") type: String = "post",
        @Query("owner_id") ownerId: Int,
        @Query("item_id") itemId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKLikeResult>
}

data class VKNewsfeedResponse(
    @com.google.gson.annotations.SerializedName("items") val items: List<VKPost>,
    @com.google.gson.annotations.SerializedName("profiles") val profiles: List<VKUser>,
    @com.google.gson.annotations.SerializedName("groups") val groups: List<VKGroup>,
    @com.google.gson.annotations.SerializedName("next_from") val nextFrom: String? = null
)

data class VKGroup(
    @com.google.gson.annotations.SerializedName("id") val id: Int,
    @com.google.gson.annotations.SerializedName("name") val name: String,
    @com.google.gson.annotations.SerializedName("photo_100") val photo100: String? = null
)

data class VKConversationsResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int,
    @com.google.gson.annotations.SerializedName("items") val items: List<VKDialog>,
    @com.google.gson.annotations.SerializedName("profiles") val profiles: List<VKUser>? = null
)

data class VKLikeResult(
    @com.google.gson.annotations.SerializedName("likes") val likes: Int
)
