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
    @SerializedName("owner_id") val ownerId: Int,
    @SerializedName("from_id") val fromId: Int,
    @SerializedName("date") val date: Long,
    @SerializedName("text") val text: String,
    @SerializedName("likes") val likes: VKLikes? = null,
    @SerializedName("reposts") val reposts: VKReposts? = null,
    @SerializedName("views") val views: VKViews? = null,
    @SerializedName("attachments") val attachments: List<VKAttachment>? = null,
    @SerializedName("comments") val comments: VKComments? = null
)

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
    @SerializedName("link") val link: VKLink? = null,
    @SerializedName("audio_message") val audioMessage: VKAudioMessage? = null
)

data class VKPhoto(
    @SerializedName("id") val id: Int,
    @SerializedName("sizes") val sizes: List<VKPhotoSize>
) {
    fun bestSize(): VKPhotoSize? = sizes.maxByOrNull { it.width * it.height }
}

data class VKPhotoSize(
    @SerializedName("type") val type: String,
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

data class VKVideo(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("image") val image: List<VKPhotoSize>? = null
)

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

data class VKConversation(
    @SerializedName("peer") val peer: VKPeer,
    @SerializedName("unread_count") val unreadCount: Int = 0
)

data class VKPeer(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String
)

data class VKMessage(
    @SerializedName("id") val id: Int,
    @SerializedName("from_id") val fromId: Int,
    @SerializedName("text") val text: String,
    @SerializedName("date") val date: Long,
    @SerializedName("out") val out: Int = 0,
    @SerializedName("attachments") val attachments: List<VKAttachment>? = null
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
