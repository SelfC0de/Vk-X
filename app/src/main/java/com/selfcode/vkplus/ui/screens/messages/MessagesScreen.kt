package com.selfcode.vkplus.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadConversations()
            isRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)
            state.error != null -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.error ?: "", color = ErrorRed)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.loadConversations() }) { Text("Повторить") }
            }
            state.dialogs.isEmpty() -> Text("Нет диалогов", color = OnSurfaceMuted, modifier = Modifier.align(Alignment.Center))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    // Pull hint
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { isRefreshing = true }) {
                            Text("Обновить", color = CyberBlue, fontSize = 12.sp)
                        }
                    }
                }
                items(state.dialogs, key = { it.conversation.peer.id }) { dialog ->
                    val peerId = dialog.conversation.peer.id
                    val profile = state.profiles[peerId]
                    val peerName = resolveName(dialog, profile)
                    val peerPhoto = profile?.photo100
                    DialogRow(dialog, peerName, peerPhoto) {
                        onOpenChat(peerId, peerName, peerPhoto)
                    }
                }
            }
        }
    }
}

private fun resolveName(dialog: VKDialog, profile: VKUser?): String {
    return profile?.fullName ?: when (dialog.conversation.peer.type) {
        "chat" -> "Беседа ${dialog.conversation.peer.id}"
        "group" -> "Сообщество"
        else -> "Пользователь ${dialog.conversation.peer.id}"
    }
}

@Composable
private fun DialogRow(dialog: VKDialog, peerName: String, peerPhoto: String?, onClick: () -> Unit) {
    val unread = dialog.conversation.unreadCount
    val time = remember(dialog.lastMessage.date) {
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(dialog.lastMessage.date * 1000L))
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = peerPhoto, contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(SurfaceVariant),
                contentScale = ContentScale.Crop
            )
            if (unread > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp).background(CyberBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (unread > 99) "99+" else unread.toString(), color = Background, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(peerName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(time, color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                dialog.lastMessage.text.ifBlank { "Вложение" },
                color = if (unread > 0) OnSurface else OnSurfaceMuted,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (unread > 0) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
    HorizontalDivider(color = Divider.copy(alpha = 0.5f), modifier = Modifier.padding(start = 76.dp))
}
