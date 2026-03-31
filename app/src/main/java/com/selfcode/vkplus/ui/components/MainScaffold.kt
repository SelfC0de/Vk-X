package com.selfcode.vkplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val drawerItems = listOf(
    DrawerItem(Screen.Feed, "Новости", Icons.Outlined.Home, Icons.Filled.Home),
    DrawerItem(Screen.Messages, "Сообщения", Icons.Outlined.Message, Icons.Filled.Message),
    DrawerItem(Screen.Friends, "Друзья", Icons.Outlined.People, Icons.Filled.People),
    DrawerItem(Screen.Profile, "Профиль", Icons.Outlined.Person, Icons.Filled.Person),
    DrawerItem(Screen.Settings, "Настройки", Icons.Outlined.Settings, Icons.Filled.Settings),
    DrawerItem(Screen.Tools, "Инструменты", Icons.Outlined.Build, Icons.Filled.Build),
    DrawerItem(Screen.About, "About Dev", Icons.Outlined.Person, Icons.Filled.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    currentScreen: Screen,
    user: VKUser?,
    onNavigate: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = Surface,
                drawerContentColor = OnSurface
            ) {
                Spacer(Modifier.height(24.dp))

                user?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = it.photo100,
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                it.fullName,
                                color = OnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (it.isOnline) "онлайн" else "не в сети",
                                color = if (it.isOnline) CyberBlue else OnSurfaceMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(color = Divider, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))
                }

                drawerItems.forEach { item ->
                    val selected = currentScreen == item.screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .background(
                                if (selected) CyberBlue.copy(alpha = 0.12f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                scope.launch { drawerState.close() }
                                onNavigate(item.screen)
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = if (selected) CyberBlue else OnSurfaceMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = item.label,
                            color = if (selected) CyberBlue else OnSurface,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "VK+ by SelfCode",
                        color = OnSurfaceMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = drawerItems.find { it.screen == currentScreen }?.label ?: "VK+",
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = CyberBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Surface
                    )
                )
            },
            containerColor = Background
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                content()
            }
        }
    }
}
