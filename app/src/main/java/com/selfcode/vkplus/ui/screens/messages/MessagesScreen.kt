package com.selfcode.vkplus.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKDialog
import com.selfcode.vkplus.data.model.VKMessage
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel = hiltViewModel(),
    onOpenChat: (peerId: Int, name: String, photo: String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Pagination trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            last >= total - 3 && !state.isLoadingMore && state.dialogs.size < state.totalCount
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMoreConversations()
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            placeholder = { Text("Поиск сообщений", color = OnSurfaceMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceMuted) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
            ),
            shape = RoundedCornerShape(10.dp), singleLine = true
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)
                state.error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "", color = ErrorRed)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadConversations() }) { Text("Повторить") }
                }
                // Search results
                state.searchQuery.isNotBlank() -> {
                    if (state.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)
                    } else if (state.searchResults.isEmpty()) {
                        Text("Ничего не найдено", color = OnSurfaceMuted, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.searchResults, key = { it.id }) { msg ->
                                SearchResultRow(msg, state.profiles) { peerId ->
                                    val profile = state.profiles[peerId]
                                    onOpenChat(peerId, profile?.fullName ?: "id$peerId", profile?.photo100)
                                }
                            }
                        }
                    }
                }
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    // Pinned Favourites
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onOpenChat(-1, "Избранное", null) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(52.dp).background(CyberBlue.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Star, null, tint = CyberBlue, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Избранное", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("Сохранённые сообщения", color = OnSurfaceMuted, fontSize = 13.sp)
                            }
                        }
                        HorizontalDivider(color = Divider.copy(alpha = 0.5f), modifier = Modifier.padding(start = 76.dp))
                    }
                    items(state.dialogs, key = { it.conversation.peer.id }) { dialog ->
                        val peerId = dialog.conversation.peer.id
                        val profile = state.profiles[peerId]
                        val peerName = resolveName(dialog, profile)
                        val peerPhoto = resolvePhoto(dialog, profile)
                        DialogRow(dialog, peerName, peerPhoto) {
                            onOpenChat(peerId, peerName, peerPhoto)
                        }
                    }
                    // Load more indicator
                    if (state.isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CyberBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveName(dialog: VKDialog, profile: VKUser?): String =
    profile?.fullName ?: when (dialog.conversation.peer.type) {
        "chat" -> "Беседа ${dialog.conversation.peer.id}"
        "group" -> "Сообщество"
        else -> "Пользователь ${dialog.conversation.peer.id}"
    }

@Composable
private fun SearchResultRow(msg: VKMessage, profiles: Map<Int, VKUser>, onOpen: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onOpen(msg.fromId) }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = profiles[msg.fromId]?.photo100, contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceVariant),
            contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(profiles[msg.fromId]?.fullName ?: "id${msg.fromId}", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(msg.text, color = OnSurfaceMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(SimpleDateFormat("HH:mm", Locale("ru")).format(Date(msg.date * 1000L)), color = OnSurfaceMuted, fontSize = 12.sp)
    }
    HorizontalDivider(color = Divider.copy(alpha = 0.4f), modifier = Modifier.padding(start = 64.dp))
}

@Composable
private fun DialogRow(dialog: VKDialog, peerName: String, peerPhoto: String?, onClick: () -> Unit) {
    val unread = dialog.conversation.unreadCount
    val time = remember(dialog.lastMessage.date) {
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(dialog.lastMessage.date * 1000L))
    }
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box {
            AsyncImage(model = peerPhoto, contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(SurfaceVariant),
                contentScale = ContentScale.Crop)
            if (unread > 0) {
                Box(modifier = Modifier.align(Alignment.TopEnd).size(18.dp).background(CyberBlue, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(if (unread > 99) "99+" else unread.toString(), color = Background, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(peerName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(time, color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(dialog.lastMessage.text.ifBlank { "Вложение" },
                color = if (unread > 0) OnSurface else OnSurfaceMuted,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (unread > 0) FontWeight.Medium else FontWeight.Normal)
        }
    }
    HorizontalDivider(color = Divider.copy(alpha = 0.5f), modifier = Modifier.padding(start = 76.dp))
}
