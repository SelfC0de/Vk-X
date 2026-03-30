package com.selfcode.vkplus.auth

object VKConfig {
    const val CLIENT_ID = 2685278
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
}
