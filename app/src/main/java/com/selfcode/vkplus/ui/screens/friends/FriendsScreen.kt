package com.selfcode.vkplus.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.ui.screens.messages.ChatScreen
import com.selfcode.vkplus.ui.screens.messages.MessagesViewModel
import com.selfcode.vkplus.ui.screens.profile.UserProfileScreen
import com.selfcode.vkplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = hiltViewModel(),
    messagesViewModel: MessagesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var chatUserId by remember { mutableStateOf<Int?>(null) }
    var chatUserName by remember { mutableStateOf("") }
    var chatUserPhoto by remember { mutableStateOf<String?>(null) }

    if (chatUserId != null) {
        ChatScreen(peerName = chatUserName, peerPhoto = chatUserPhoto, peerId = chatUserId!!, onBack = { chatUserId = null }, viewModel = messagesViewModel)
        return
    }

    if (selectedUserId != null) {
        UserProfileScreen(
            userId = selectedUserId!!,
            onBack = { selectedUserId = null },
            onWriteMessage = { uid ->
                val user = state.friends.find { it.id == uid }
                chatUserName = user?.fullName ?: "Диалог"
                chatUserPhoto = user?.photo100
                chatUserId = uid
                selectedUserId = null
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Tabs
        TabRow(selectedTabIndex = selectedTab,
            containerColor = Surface, contentColor = CyberBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberBlue
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; viewModel.load() },
                text = { Text("Все друзья", fontSize = 13.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; viewModel.loadOnlineFriends() },
                text = { Text("Онлайн ${state.onlineCount}", fontSize = 13.sp) })
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Поиск друзей", color = OnSurfaceMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceMuted) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!state.isLoading) {
                Text("Друзья ${state.friends.size} · Онлайн ${state.onlineCount}", color = OnSurfaceMuted, fontSize = 12.sp)
            }
            TextButton(onClick = { viewModel.refresh() }) {
                Text("↻  Обновить", color = CyberBlue, fontSize = 12.sp)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)
                state.error != null -> Text(state.error ?: "", color = ErrorRed, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.filtered, key = { it.id }) { user ->
                        FriendRow(
                            user = user,
                            onClick = { selectedUserId = user.id.toString() },
                            onWriteMessage = {
                                chatUserName = user.fullName
                                chatUserPhoto = user.photo100
                                chatUserId = user.id
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRow(user: VKUser, onClick: () -> Unit, onWriteMessage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = user.photo100, contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceVariant),
                contentScale = ContentScale.Crop
            )
            if (user.isOnline) {
                Box(modifier = Modifier.size(13.dp).align(Alignment.BottomEnd)
                    .background(Background, CircleShape).padding(2.dp).background(CyberAccent, CircleShape))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            val sub = if (user.isOnline) "онлайн" else user.status?.takeIf { it.isNotBlank() } ?: "не в сети"
            Text(sub, color = if (user.isOnline) CyberBlue else OnSurfaceMuted, fontSize = 12.sp)
        }
        IconButton(onClick = onWriteMessage, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Send, "Написать", tint = CyberBlue, modifier = Modifier.size(18.dp))
        }
    }
    HorizontalDivider(color = Divider.copy(alpha = 0.4f), modifier = Modifier.padding(start = 72.dp))
}
