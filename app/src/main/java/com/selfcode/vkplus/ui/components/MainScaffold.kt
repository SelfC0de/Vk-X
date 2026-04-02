package com.selfcode.vkplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.ui.navigation.Screen
import com.selfcode.vkplus.ui.theme.*
import kotlinx.coroutines.launch

data class DrawerItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val mainDrawerItems = listOf(
    DrawerItem(Screen.Feed,        "Новости",     Icons.Outlined.Home,     Icons.Filled.Home),
    DrawerItem(Screen.Profile,     "Профиль",     Icons.Outlined.Person,   Icons.Filled.Person),
    DrawerItem(Screen.Messages,    "Сообщения",   Icons.Outlined.Message,  Icons.Filled.Message),
    DrawerItem(Screen.Friends,     "Друзья",      Icons.Outlined.People,   Icons.Filled.People),
    DrawerItem(Screen.Communities, "Сообщества",  Icons.Outlined.Group,    Icons.Filled.Group),
    DrawerItem(Screen.Music,       "Музыка",      Icons.Outlined.MusicNote, Icons.Filled.MusicNote),
    DrawerItem(Screen.Settings,    "Настройки",   Icons.Outlined.Settings, Icons.Filled.Settings),
    DrawerItem(Screen.About,       "About Dev",   Icons.Outlined.Info,     Icons.Filled.Info),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    onAddAccount: () -> Unit = {},
    onSwitchAccount: () -> Unit = {},
    currentScreen: Screen,
    user: VKUser?,
    mirrorName: String = "",
    mirrorPhoto: String = "",
    isMirrorActive: Boolean = false,
    onNavigate: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(270.dp),
                drawerContainerColor = Surface,
                drawerContentColor = OnSurface
            ) {
                Spacer(Modifier.height(20.dp))

                // User header
                user?.let {
                    val displayPhoto = if (isMirrorActive && mirrorPhoto.isNotBlank()) mirrorPhoto else it.photo100
                    val displayName  = if (isMirrorActive && mirrorName.isNotBlank()) mirrorName else it.fullName

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = displayPhoto, contentDescription = null,
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(SurfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(displayName, color = if (isMirrorActive) Color(0xFFFF6B35) else OnSurface,
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (isMirrorActive) { Spacer(Modifier.width(4.dp)); Text("🎭", fontSize = 12.sp) }
                            }
                            Text(if (it.isOnline) "онлайн" else "не в сети",
                                color = if (it.isOnline) CyberAccent else OnSurfaceMuted, fontSize = 11.sp)
                        }
                        IconButton(onClick = onSwitchAccount, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.SwapHoriz, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(color = Divider, modifier = Modifier.padding(horizontal = 12.dp))
                    Spacer(Modifier.height(4.dp))
                }

                // Main nav items
                mainDrawerItems.forEach { item ->
                    val selected = currentScreen == item.screen
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CyberBlue.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                onNavigate(item.screen)
                                scope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (selected) item.selectedIcon else item.icon,
                            null,
                            tint = if (selected) CyberBlue else OnSurfaceMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(item.label, color = if (selected) CyberBlue else OnSurface,
                            fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }

                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = Divider, modifier = Modifier.padding(horizontal = 12.dp))
                Spacer(Modifier.height(4.dp))

                // VK+ special tab
                val vkPlusSelected = currentScreen == Screen.VKPlus
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (vkPlusSelected)
                                Brush.horizontalGradient(listOf(CyberBlue.copy(0.15f), CyberAccent.copy(0.15f)))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable {
                            onNavigate(Screen.VKPlus)
                            scope.launch { drawerState.close() }
                        }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null,
                        tint = if (vkPlusSelected) CyberAccent else CyberAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("VK+", color = if (vkPlusSelected) CyberAccent else CyberAccent.copy(alpha = 0.7f),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.background(CyberAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("PLUS", color = CyberAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen) {
                                Screen.Feed -> "Новости"
                                Screen.Profile -> "Профиль"
                                Screen.Messages -> "Сообщения"
                                Screen.Friends -> "Друзья"
                                Screen.Communities -> "Сообщества"
                                Screen.Settings -> "Настройки"
                                Screen.About -> "About Dev"
                                Screen.Music -> "Музыка"
                                Screen.VKPlus -> "VK+"
                                else -> "VK+"
                            },
                            color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, null, tint = OnSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
                )
            },
            containerColor = Background
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                content()
            }
        }
    }
}
