package com.selfcode.vkplus.data.api

import com.selfcode.vkplus.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface VKApi {

    @GET("users.get")
    suspend fun getUsers(
        @Query("user_ids") userIds: String? = null,
        @Query("fields") fields: String = "photo_200,photo_100,online,last_seen,status,city,followers_count,counters",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<List<VKUser>>

    @GET("newsfeed.get")
    suspend fun getNewsfeed(
        @Query("filters") filters: String = "post,photo",
        @Query("count") count: Int = 30,
        @Query("start_from") startFrom: String? = null,
        @Query("fields") fields: String = "photo_100,online,status",
        @Query("return_banned") returnBanned: Int = 0,
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

    @GET("messages.getHistory")
    suspend fun getHistory(
        @Query("peer_id") peerId: Int,
        @Query("count") count: Int = 50,
        @Query("extended") extended: Int = 1,
        @Query("fields") fields: String = "photo_100",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKHistoryResponse>

    @GET("messages.send")
    suspend fun sendMessage(
        @Query("peer_id") peerId: Int,
        @Query("message") message: String,
        @Query("random_id") randomId: Long = System.currentTimeMillis(),
        @Query("reply_to") replyTo: Int? = null,
        @Query("forward_messages") forwardMessages: String? = null,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

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

    @GET("friends.get")
    suspend fun getFriendsWithStatus(
        @Query("fields") fields: String = "deactivated,photo_100,online",
        @Query("count") count: Int = 5000,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKFriendsResponse>

    @GET("friends.delete")
    suspend fun deleteFriend(
        @Query("user_id") userId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKDeleteFriendResult>

    @GET("messages.markAsListened")
    suspend fun markAsListened(
        @Query("message_id") messageId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("gifts.get")
    suspend fun getGifts(
        @Query("user_id") userId: Int? = null,
        @Query("count") count: Int = 100,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKGiftsResponse>

    @GET("wall.post")
    suspend fun wallPost(
        @Query("owner_id") ownerId: Int? = null,
        @Query("message") message: String,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKWallPostResult>

    @GET("messages.delete")
    suspend fun deleteMessage(
        @Query("message_ids") messageIds: String,
        @Query("delete_for_all") deleteForAll: Int = 1,
        @Query("peer_id") peerId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.google.gson.JsonElement>

    @GET("messages.edit")
    suspend fun editMessage(
        @Query("peer_id") peerId: Int,
        @Query("message_id") messageId: Int,
        @Query("message") message: String,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("users.get")
    suspend fun getUserExtended(
        @Query("user_ids") userIds: String,
        @Query("fields") fields: String = "photo_200,photo_100,online,last_seen,status,city,bdate,followers_count,counters,occupation,relation",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<List<VKUserExtended>>

    @GET("store.getStickersKeywords")
    suspend fun getStickersKeywords(
        @Query("user_id") userId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKStickersResponse>

    @GET("store.getProducts")
    suspend fun getUserStickers(
        @Query("user_id") userId: Int,
        @Query("type") type: String = "stickers",
        @Query("filters") filters: String = "purchased",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKStickersResponse>

    @GET("groups.isMember")
    suspend fun isGroupMember(
        @Query("group_id") groupId: Int,
        @Query("user_id") userId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("groups.getById")
    suspend fun getGroupById(
        @Query("group_id") groupId: Int,
        @Query("fields") fields: String = "name",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<List<VKGroupFull>>

    @GET("newsfeed.get")
    suspend fun getLegacyNewsfeed(
        @Query("filters") filters: String = "post",
        @Query("count") count: Int = 20,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.92"
    ): VKResponse<VKLegacyFeedResponse>

    @GET("messages.getChat")
    suspend fun getChat(
        @Query("chat_id") chatId: Int,
        @Query("fields") fields: String = "online,last_seen",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.103"
    ): VKResponse<VKChatResponse>

    @GET("stickers.getKeywords")
    suspend fun getStickerKeywords(
        @Query("user_id") userId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.131"
    ): VKResponse<VKStickerKeywordsResponse>

    @GET("translations.translate")
    suspend fun translateText(
        @Query("texts") texts: String,
        @Query("translation_language") lang: String = "ru",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.131"
    ): VKResponse<VKTranslateResponse>

    @GET("messages.getLastActivity")
    suspend fun getLastActivity(
        @Query("user_id") userId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKLastActivity>

    @GET("messages.restore")
    suspend fun restoreMessage(
        @Query("message_id") messageId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("friends.getOnline")
    suspend fun getFriendsOnline(
        @Query("user_id") userId: Int? = null,
        @Query("fields") fields: String = "photo_100,status",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.google.gson.JsonElement>

    @GET("users.getFollowers")
    suspend fun getFollowers(
        @Query("user_id") userId: Int,
        @Query("count") count: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = "photo_100,online",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKUsersResponse>

    @GET("wall.getComments")
    suspend fun getWallComments(
        @Query("owner_id") ownerId: Int,
        @Query("post_id") postId: Int,
        @Query("count") count: Int = 20,
        @Query("extended") extended: Int = 1,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKCommentsResponse>

    @GET("messages.search")
    suspend fun searchMessages(
        @Query("q") query: String,
        @Query("count") count: Int = 20,
        @Query("peer_id") peerId: Int? = null,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKMessagesResponse>

    @GET("messages.getConversations")
    suspend fun getConversationsPaged(
        @Query("count") count: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("filter") filter: String = "all",
        @Query("extended") extended: Int = 1,
        @Query("fields") fields: String = "photo_100,online",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.selfcode.vkplus.data.model.VKConversationsResponse>

    // Photo albums
    @GET("photos.getAlbums")
    suspend fun getPhotoAlbums(
        @Query("owner_id") ownerId: Int? = null,
        @Query("need_system") needSystem: Int = 1,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.selfcode.vkplus.data.model.VKAlbumsResponse>

    @GET("photos.get")
    suspend fun getPhotos(
        @Query("owner_id") ownerId: Int,
        @Query("album_id") albumId: String = "wall",
        @Query("count") count: Int = 50,
        @Query("extended") extended: Int = 1,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.selfcode.vkplus.data.model.VKPhotosResponse>

    @GET("photos.save")
    suspend fun savePhoto(
        @Query("album_id") albumId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.google.gson.JsonElement>

    // Communities
    @GET("groups.get")
    suspend fun getUserGroups(
        @Query("user_id") userId: Int? = null,
        @Query("extended") extended: Int = 1,
        @Query("fields") fields: String = "photo_100,members_count,activity",
        @Query("count") count: Int = 50,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.selfcode.vkplus.data.model.VKCommunitiesResponse>

    @GET("groups.join")
    suspend fun joinGroup(
        @Query("group_id") groupId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("groups.leave")
    suspend fun leaveGroup(
        @Query("group_id") groupId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("groups.search")
    suspend fun searchGroups(
        @Query("q") query: String,
        @Query("count") count: Int = 20,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.selfcode.vkplus.data.model.VKCommunitiesResponse>

    // People search
    @GET("users.search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("count") count: Int = 20,
        @Query("fields") fields: String = "photo_100,online,status,city",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.selfcode.vkplus.data.model.VKUsersSearchResponse>

    // Blacklist
    @GET("account.getBanned")
    suspend fun getBannedUsers(
        @Query("count") count: Int = 50,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKBannedResponse>

    @GET("account.ban")
    suspend fun banUser(
        @Query("owner_id") userId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("account.unban")
    suspend fun unbanUser(
        @Query("owner_id") userId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    // Followers
    @GET("users.getFollowers")
    suspend fun getOwnFollowers(
        @Query("count") count: Int = 50,
        @Query("fields") fields: String = "photo_100,online",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.selfcode.vkplus.data.api.VKUsersResponse>

    // Friend request: add
    @GET("friends.add")
    suspend fun addFriend(
        @Query("user_id") userId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    // Mutual friends
    @GET("friends.getMutual")
    suspend fun getMutualFriends(
        @Query("target_uid") targetId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<List<Int>>

    // Birthdays
    @GET("friends.get")
    suspend fun getFriendsWithBdays(
        @Query("fields") fields: String = "bdate,photo_100",
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKFriendsResponse>

    // Edit profile
    @GET("account.saveProfileInfo")
    suspend fun saveProfileInfo(
        @Query("first_name") firstName: String? = null,
        @Query("last_name") lastName: String? = null,
        @Query("status") status: String? = null,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKSaveProfileResponse>

    // Message reactions
    @GET("messages.sendReaction")
    suspend fun sendReaction(
        @Query("peer_id") peerId: Int,
        @Query("cmid") cmid: Int,
        @Query("reaction_id") reactionId: Int,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("messages.getLongPollServer")
    suspend fun getLongPollServer(
        @Query("lp_version") lpVersion: Int = 3,
        @Query("need_pts") needPts: Int = 0,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKLongPollServer>

    @GET("execute")
    suspend fun execute(
        @Query("code") code: String,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<com.google.gson.JsonElement>

    @GET("auth.getAuthCode")
    suspend fun getQrCode(
        @Query("client_id") clientId: Int,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKQrCodeResponse>

    @GET("account.setOffline")
    suspend fun setOffline(
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<Int>

    @GET("auth.checkAuthCode")
    suspend fun checkQrCode(
        @Query("auth_hash") authHash: String,
        @Query("v") version: String = "5.199"
    ): VKResponse<VKQrCheckResponse>
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

data class VKHistoryResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int,
    @com.google.gson.annotations.SerializedName("items") val items: List<VKMessage>
)

data class VKLikeResult(
    @com.google.gson.annotations.SerializedName("likes") val likes: Int
)

data class VKQrCodeResponse(
    @com.google.gson.annotations.SerializedName("auth_hash") val authHash: String,
    @com.google.gson.annotations.SerializedName("url") val url: String? = null
)

data class VKQrCheckResponse(
    @com.google.gson.annotations.SerializedName("status") val status: Int,
    @com.google.gson.annotations.SerializedName("access_token") val accessToken: String? = null,
    @com.google.gson.annotations.SerializedName("user_id") val userId: Int? = null
)

data class VKDeleteFriendResult(
    @com.google.gson.annotations.SerializedName("success") val success: Int = 0
)

data class VKGiftsResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int,
    @com.google.gson.annotations.SerializedName("items") val items: List<VKGift>
)

data class VKGift(
    @com.google.gson.annotations.SerializedName("id") val id: Int,
    @com.google.gson.annotations.SerializedName("from_id") val fromId: Int? = null,
    @com.google.gson.annotations.SerializedName("message") val message: String? = null,
    @com.google.gson.annotations.SerializedName("date") val date: Long = 0,
    @com.google.gson.annotations.SerializedName("gift") val gift: VKGiftItem? = null,
    @com.google.gson.annotations.SerializedName("privacy") val privacy: Int = 0
) {
    val isAnonymous get() = privacy == 1
    val hasRealSender get() = fromId != null && fromId != 0 && isAnonymous
}

data class VKGiftItem(
    @com.google.gson.annotations.SerializedName("id") val id: Int,
    @com.google.gson.annotations.SerializedName("thumb_256") val thumb256: String? = null
)

data class VKWallPostResult(
    @com.google.gson.annotations.SerializedName("post_id") val postId: Int
)

data class VKLongPollServer(
    @com.google.gson.annotations.SerializedName("key") val key: String,
    @com.google.gson.annotations.SerializedName("server") val server: String,
    @com.google.gson.annotations.SerializedName("ts") val ts: String
)

data class VKStickersResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int = 0,
    @com.google.gson.annotations.SerializedName("items") val items: List<VKStickerPack> = emptyList()
)

data class VKStickerPack(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("title") val title: String = "",
    @com.google.gson.annotations.SerializedName("type") val type: String = "",
    @com.google.gson.annotations.SerializedName("pack") val pack: VKStickerPackInfo? = null
)

data class VKStickerPackInfo(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("title") val title: String = ""
)

data class VKGroupFull(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("name") val name: String = ""
)

data class VKUserExtended(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("first_name") val firstName: String = "",
    @com.google.gson.annotations.SerializedName("last_name") val lastName: String = "",
    @com.google.gson.annotations.SerializedName("photo_200") val photo200: String? = null,
    @com.google.gson.annotations.SerializedName("photo_100") val photo100: String? = null,
    @com.google.gson.annotations.SerializedName("online") val online: Int = 0,
    @com.google.gson.annotations.SerializedName("status") val status: String? = null,
    @com.google.gson.annotations.SerializedName("city") val city: com.selfcode.vkplus.data.model.VKCity? = null,
    @com.google.gson.annotations.SerializedName("bdate") val bdate: String? = null,
    @com.google.gson.annotations.SerializedName("followers_count") val followersCount: Int = 0,
    @com.google.gson.annotations.SerializedName("occupation") val occupation: VKOccupation? = null,
    @com.google.gson.annotations.SerializedName("relation") val relation: Int = 0,
    @com.google.gson.annotations.SerializedName("deactivated") val deactivated: String? = null
) {
    val fullName get() = "$firstName $lastName".trim()
    val isOnline get() = online == 1
}

data class VKOccupation(
    @com.google.gson.annotations.SerializedName("type") val type: String = "",
    @com.google.gson.annotations.SerializedName("name") val name: String = ""
)

data class VKLegacyFeedResponse(
    @com.google.gson.annotations.SerializedName("items") val items: List<VKFeedItem> = emptyList(),
    @com.google.gson.annotations.SerializedName("profiles") val profiles: List<com.selfcode.vkplus.data.model.VKUser> = emptyList()
)

data class VKFeedItem(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("source_id") val sourceId: Int = 0,
    @com.google.gson.annotations.SerializedName("text") val text: String = "",
    @com.google.gson.annotations.SerializedName("date") val date: Long = 0
)

data class VKChatResponse(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("title") val title: String = "",
    @com.google.gson.annotations.SerializedName("users") val users: List<VKChatMember> = emptyList()
)

data class VKChatMember(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("first_name") val firstName: String = "",
    @com.google.gson.annotations.SerializedName("last_name") val lastName: String = "",
    @com.google.gson.annotations.SerializedName("online") val online: Int = 0,
    @com.google.gson.annotations.SerializedName("type") val type: String = "member"
) {
    val fullName get() = "$firstName $lastName".trim()
    val isAdmin get() = type == "admin"
}

data class VKStickerKeywordsResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int = 0,
    @com.google.gson.annotations.SerializedName("items") val items: List<VKStickerKeywordItem> = emptyList()
)

data class VKStickerKeywordItem(
    @com.google.gson.annotations.SerializedName("words") val words: List<String> = emptyList(),
    @com.google.gson.annotations.SerializedName("user_stickers") val userStickers: List<VKStickerRef> = emptyList()
)

data class VKStickerRef(
    @com.google.gson.annotations.SerializedName("product") val product: VKStickerProductRef? = null
)

data class VKStickerProductRef(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("title") val title: String = "",
    @com.google.gson.annotations.SerializedName("type") val type: String = ""
)

data class VKTranslateResponse(
    @com.google.gson.annotations.SerializedName("texts") val texts: List<String> = emptyList(),
    @com.google.gson.annotations.SerializedName("source_lang") val sourceLang: String = ""
)

data class VKLastActivity(
    @com.google.gson.annotations.SerializedName("online") val online: Int = 0,
    @com.google.gson.annotations.SerializedName("time") val time: Long = 0
)

data class VKUsersResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int = 0,
    @com.google.gson.annotations.SerializedName("items") val items: List<com.selfcode.vkplus.data.model.VKUser> = emptyList()
)

data class VKCommentsResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int = 0,
    @com.google.gson.annotations.SerializedName("items") val items: List<VKComment> = emptyList(),
    @com.google.gson.annotations.SerializedName("profiles") val profiles: List<com.selfcode.vkplus.data.model.VKUser> = emptyList()
)

data class VKComment(
    @com.google.gson.annotations.SerializedName("id") val id: Int = 0,
    @com.google.gson.annotations.SerializedName("from_id") val fromId: Int = 0,
    @com.google.gson.annotations.SerializedName("date") val date: Long = 0,
    @com.google.gson.annotations.SerializedName("text") val text: String = ""
)

data class VKMessagesResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int = 0,
    @com.google.gson.annotations.SerializedName("items") val items: List<com.selfcode.vkplus.data.model.VKMessage> = emptyList()
)

data class VKBannedResponse(
    @com.google.gson.annotations.SerializedName("count") val count: Int = 0,
    @com.google.gson.annotations.SerializedName("items") val items: List<com.selfcode.vkplus.data.model.VKUser> = emptyList()
)

data class VKSaveProfileResponse(
    @com.google.gson.annotations.SerializedName("changed") val changed: Int = 0
)
