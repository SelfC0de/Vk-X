package com.selfcode.vkplus.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Feed : Screen("feed")
    object Messages : Screen("messages")
    object Friends : Screen("friends")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Tools : Screen("tools")
    object About : Screen("about")
    object Exploits : Screen("exploits")
}
