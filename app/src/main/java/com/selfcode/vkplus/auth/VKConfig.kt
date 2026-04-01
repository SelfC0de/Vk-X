package com.selfcode.vkplus.auth

object VKConfig {
    // Available client IDs for auth
    const val CLIENT_ID_KATE    = 2685278  // Kate Mobile
    const val CLIENT_ID_VINT    = 3071348  // Vint
    const val CLIENT_ID_VKLITE  = 3140623  // VK Lite
    const val CLIENT_ID_IPAD    = 3682744  // iPad App

    // Active client (Kate Mobile by default)
    var CLIENT_ID = CLIENT_ID_KATE

    const val API_VERSION = "5.199"
    const val BASE_URL = "https://api.vk.com/method/"
    const val REDIRECT_URI = "https://oauth.vk.com/blank.html"
    const val SCOPE = "friends,wall,messages,photos,audio,video,notifications,stats,offline"

    fun authUrl(): String = buildString {
        append("https://oauth.vk.com/authorize")
        append("?client_id=$CLIENT_ID")
        append("&display=mobile")
        append("&redirect_uri=$REDIRECT_URI")
        append("&scope=$SCOPE")
        append("&response_type=token")
        append("&v=$API_VERSION")
        append("&revoke=1")
    }

    val clientLabels = listOf(
        "Kate Mobile (2685278)"  to CLIENT_ID_KATE,
        "Vint (3071348)"         to CLIENT_ID_VINT,
        "VK Lite (3140623)"      to CLIENT_ID_VKLITE,
        "iPad App (3682744)"     to CLIENT_ID_IPAD
    )
}
