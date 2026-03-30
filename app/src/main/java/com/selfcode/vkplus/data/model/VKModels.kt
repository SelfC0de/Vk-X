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
    @SerializedName("deactivated") val deactivated: String? = null
) {
    val fullName get() = "$firstName $lastName"
    val isOnline get() = online == 1
    val isBanned get() = deactivated != null
}

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
    @SerializedName("link") val link: VKLink? = null
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
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null
)

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
    @SerializedName("out") val out: Int = 0
) {
    val isOutgoing get() = out == 1
}
