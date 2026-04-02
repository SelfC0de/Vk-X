package com.selfcode.vkplus.data.model

import com.google.gson.annotations.SerializedName

data class VKResponse<T>(
    @SerializedName("response") val response: T?,
    @SerializedName("error") val error: VKError?
)

data class VKError(
    @SerializedName("error_code") val code: Int,
    @SerializedName("error_msg") val message: String
)

data class VKUser(
    @SerializedName("id") val id: Int,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("photo_200") val photo200: String? = null,
    @SerializedName("photo_100") val photo100: String? = null,
    @SerializedName("online") val online: Int = 0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("city") val city: VKCity? = null,
    @SerializedName("deactivated") val deactivated: String? = null,
    @SerializedName("last_seen") val lastSeen: VKLastSeen? = null,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("counters") val counters: VKCounters? = null
) {
    val fullName get() = "$firstName $lastName"
    val isOnline get() = online == 1
    val isBanned get() = deactivated != null
}

data class VKLastSeen(
    @SerializedName("time") val time: Long = 0,
    @SerializedName("platform") val platform: Int = 0
)

data class VKCounters(
    @SerializedName("friends") val friends: Int = 0,
    @SerializedName("followers") val followers: Int = 0,
    @SerializedName("photos") val photos: Int = 0,
    @SerializedName("videos") val videos: Int = 0
)

data class VKCity(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String
)

data class VKWallResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("items") val items: List<VKPost>
)

data class VKPost(
    @SerializedName("id") val id: Int,
    @SerializedName("owner_id") val ownerId: Int = 0,
    @SerializedName("from_id") val fromId: Int = 0,
    @SerializedName("source_id") val sourceId: Int = 0, // newsfeed.get uses source_id
    @SerializedName("date") val date: Long,
    @SerializedName("text") val text: String = "",
    @SerializedName("likes") val likes: VKLikes? = null,
    @SerializedName("reposts") val reposts: VKReposts? = null,
    @SerializedName("views") val views: VKViews? = null,
    @SerializedName("attachments") val attachments: List<VKAttachment>? = null,
    @SerializedName("comments") val comments: VKComments? = null,
    @SerializedName("post_type") val postType: String = "post",
    @SerializedName("is_pinned") val isPinned: Int = 0
) {
    // newsfeed uses source_id, wall uses from_id — resolve whichever is set
    val authorId get() = if (sourceId != 0) sourceId else if (fromId != 0) fromId else ownerId
}

data class VKLikes(
    @SerializedName("count") val count: Int,
    @SerializedName("user_likes") val userLikes: Int = 0
) {
    val isLiked get() = userLikes == 1
}

data class VKReposts(
    @SerializedName("count") val count: Int
)

data class VKViews(
    @SerializedName("count") val count: Int
)

data class VKComments(
    @SerializedName("count") val count: Int
)

data class VKAttachment(
    @SerializedName("type") val type: String,
    @SerializedName("photo") val photo: VKPhoto? = null,
    @SerializedName("video") val video: VKVideo? = null,
    @SerializedName("audio") val audio: VKAudio? = null,
    @SerializedName("doc") val doc: VKDoc? = null,
    @SerializedName("sticker") val sticker: VKSticker? = null,
    @SerializedName("link") val link: VKLink? = null,
    @SerializedName("audio_message") val audioMessage: VKAudioMessage? = null
)

data class VKAudio(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("owner_id") val ownerId: Int = 0,
    @SerializedName("artist") val artist: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("url") val url: String = ""
)

data class VKDoc(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("owner_id") val ownerId: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("size") val size: Int = 0,
    @SerializedName("ext") val ext: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("type") val type: Int = 0,
    @SerializedName("preview") val preview: VKDocPreview? = null
)

data class VKDocPreview(
    @SerializedName("photo") val photo: VKDocPhoto? = null,
    @SerializedName("video") val video: VKDocPreviewVideo? = null
)

data class VKDocPhoto(
    @SerializedName("sizes") val sizes: List<VKPhotoSize> = emptyList()
) {
    fun bestSize(): VKPhotoSize? = sizes.maxByOrNull { it.width * it.height }
}

data class VKDocPreviewVideo(
    @SerializedName("src") val src: String = "",
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0
)

data class VKSticker(
    @SerializedName("sticker_id") val stickerId: Int = 0,
    @SerializedName("product_id") val productId: Int = 0,
    @SerializedName("images") val images: List<VKPhotoSize>? = null,
    @SerializedName("images_with_background") val imagesWithBg: List<VKPhotoSize>? = null
) {
    fun bestImage(): VKPhotoSize? = (images ?: imagesWithBg)?.maxByOrNull { it.width * it.height }
}

data class VKPhoto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("owner_id") val ownerId: Int = 0,
    @SerializedName("album_id") val albumId: Int = 0,
    @SerializedName("sizes") val sizes: List<VKPhotoSize> = emptyList(),
    @SerializedName("text") val text: String = "",
    @SerializedName("date") val date: Long = 0
) {
    fun bestSize(): VKPhotoSize? = sizes.maxByOrNull { it.width * it.height }
}

data class VKPhotoSize(
    @SerializedName("type") val type: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0
)

data class VKVideo(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("owner_id") val ownerId: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("image") val image: List<VKPhotoSize>? = null,
    @SerializedName("player") val player: String? = null,
    @SerializedName("files") val files: VKVideoFiles? = null
)

data class VKVideoFiles(
    @SerializedName("mp4_1080") val mp4_1080: String? = null,
    @SerializedName("mp4_720") val mp4_720: String? = null,
    @SerializedName("mp4_480") val mp4_480: String? = null,
    @SerializedName("mp4_360") val mp4_360: String? = null,
    @SerializedName("mp4_240") val mp4_240: String? = null
) {
    fun bestUrl() = mp4_1080 ?: mp4_720 ?: mp4_480 ?: mp4_360 ?: mp4_240
}

data class VKLink(
    @SerializedName("url") val url: String,
    @SerializedName("target_url") val targetUrl: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null
) {
    val realUrl: String get() {
        val base = targetUrl ?: url
        return if (base.contains("vk.com/away") || base.contains("vk.com/link")) {
            runCatching {
                android.net.Uri.parse(base).getQueryParameter("to")
                    ?: android.net.Uri.parse(base).getQueryParameter("url")
                    ?: base
            }.getOrDefault(base)
        } else base
    }
}

data class VKFriendsResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("items") val items: List<VKUser>
)

data class VKMessagesResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("items") val items: List<VKDialog>
)

data class VKDialog(
    @SerializedName("conversation") val conversation: VKConversation,
    @SerializedName("last_message") val lastMessage: VKMessage
)

data class VKChatSettings(
    @SerializedName("title") val title: String = "",
    @SerializedName("photo") val photo: VKChatPhoto? = null
)

data class VKChatPhoto(
    @SerializedName("photo_100") val photo100: String? = null
)

data class VKConversation(
    @SerializedName("peer") val peer: VKPeer,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("chat_settings") val chatSettings: VKChatSettings? = null
)

data class VKPeer(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String
)

data class VKMessage(
    @SerializedName("id") val id: Int,
    @SerializedName("from_id") val fromId: Int,
    @SerializedName("text") val text: String = "",
    @SerializedName("date") val date: Long,
    @SerializedName("out") val out: Int = 0,
    @SerializedName("attachments") val attachments: List<VKAttachment>? = null,
    @SerializedName("reply_message") val replyMessage: VKReplyMessage? = null,
    @SerializedName("fwd_messages") val fwdMessages: List<VKMessage>? = null,
    @SerializedName("reaction_id") val reactionId: Int = 0,
    @SerializedName("is_deleted") val isDeleted: Int = 0
) {
    val isOutgoing get() = out == 1
    val voiceMessage get() = attachments?.firstOrNull { it.type == "audio_message" }?.audioMessage
}

data class VKAudioMessage(
    @SerializedName("id") val id: Int,
    @SerializedName("owner_id") val ownerId: Int,
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("link_mp3") val linkMp3: String? = null,
    @SerializedName("link_ogg") val linkOgg: String? = null,
    @SerializedName("was_listened") val wasListened: Boolean = false
)

data class VKConversationsResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("items") val items: List<VKDialog> = emptyList(),
    @SerializedName("profiles") val profiles: List<VKUser>? = null,
    @SerializedName("groups") val groups: List<com.selfcode.vkplus.data.api.VKGroup>? = null
)

// Reactions
data class VKReaction(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("count") val count: Int = 0
)

// Reply message
data class VKReplyMessage(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("from_id") val fromId: Int = 0,
    @SerializedName("text") val text: String = "",
    @SerializedName("date") val date: Long = 0
)

// Albums
data class VKAlbum(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("owner_id") val ownerId: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("size") val size: Int = 0,
    @SerializedName("thumb") val thumb: VKPhoto? = null,
    @SerializedName("description") val description: String = ""
)


data class VKAlbumsResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("items") val items: List<VKAlbum> = emptyList()
)

data class VKPhotosResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("items") val items: List<VKPhoto> = emptyList()
)

// Community / Group full
data class VKCommunity(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("screen_name") val screenName: String = "",
    @SerializedName("photo_200") val photo200: String? = null,
    @SerializedName("photo_100") val photo100: String? = null,
    @SerializedName("type") val type: String = "",
    @SerializedName("is_member") val isMember: Int = 0,
    @SerializedName("members_count") val membersCount: Int = 0,
    @SerializedName("description") val description: String = "",
    @SerializedName("activity") val activity: String = ""
)

data class VKCommunitiesResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("items") val items: List<VKCommunity> = emptyList()
)

// Birthday
data class VKBirthday(
    val userId: Int,
    val name: String,
    val photo: String?,
    val bdate: String
)

// Gift
data class VKGiftItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("from_id") val fromId: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("date") val date: Long = 0,
    @SerializedName("gift") val gift: VKGiftInfo? = null
)

data class VKGiftInfo(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("thumb_256") val thumb256: String = ""
)

data class VKGiftsResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("items") val items: List<VKGiftItem> = emptyList()
)

// People search
data class VKUsersSearchResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("items") val items: List<VKUser> = emptyList()
)
