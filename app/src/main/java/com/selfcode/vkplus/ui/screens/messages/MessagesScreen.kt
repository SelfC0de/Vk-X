package com.selfcode.vkplus.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKConversation
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
    val pullState = rememberPullToRefreshState()

    if (pullState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.loadConversations()
            pullState.endRefresh()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background)
            .nestedScroll(pullState.nestedScrollConnection)
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)
            state.error != null -> Text(state.error ?: "", color = ErrorRed, modifier = Modifier.align(Alignment.Center))
            state.conversations.isEmpty() -> Text("Нет диалогов", color = OnSurfaceMuted, modifier = Modifier.align(Alignment.Center))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.conversations, key = { it.peerId }) { conv ->
                    ConversationRow(conv) { onOpenChat(conv.peerId, conv.peerName, conv.peerPhoto) }
                }
            }
        }

        PullToRefreshContainer(
            state = pullState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = Surface,
            contentColor = CyberBlue
        )
    }
}

@Composable
private fun ConversationRow(conv: VKConversation, onClick: () -> Unit) {
    val time = remember(conv.lastMessage?.date) {
        conv.lastMessage?.date?.let {
            SimpleDateFormat("HH:mm", Locale("ru")).format(Date(it * 1000L))
        } ?: ""
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = conv.peerPhoto, contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(SurfaceVariant),
                contentScale = ContentScale.Crop
            )
            if (conv.unreadCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                        .background(CyberBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (conv.unreadCount > 99) "99+" else conv.unreadCount.toString(),
                        color = Background, fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(conv.peerName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(time, color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                conv.lastMessage?.text?.ifBlank { "Вложение" } ?: "",
                color = if (conv.unreadCount > 0) OnSurface else OnSurfaceMuted,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (conv.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
    HorizontalDivider(color = Divider.copy(alpha = 0.5f), modifier = Modifier.padding(start = 76.dp))
}
