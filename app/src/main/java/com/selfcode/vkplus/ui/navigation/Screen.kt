package com.selfcode.vkplus.ui.navigation

sealed class Screen(val route: String) {
    object Feed        : Screen("feed")
    object Profile     : Screen("profile")
    object Messages    : Screen("messages")
    object Communities : Screen("communities")
    object Settings    : Screen("settings")
    object About       : Screen("about")
    object VKPlus      : Screen("vkplus")   // Combined: Exploits + Tools + Search + Photos + Blacklist
    object Friends     : Screen("friends")
    object Photos      : Screen("photos")
    object Blacklist   : Screen("blacklist")
    object SearchPeople: Screen("search_people")
    object Tools       : Screen("tools")
    object Exploits    : Screen("exploits")
    data class Community(val groupId: Int) : Screen("community/$groupId")
}
