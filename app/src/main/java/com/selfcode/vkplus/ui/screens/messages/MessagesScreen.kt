package com.selfcode.vkplus.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.selfcode.vkplus.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(viewModel: MessagesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedDialog by remember { mutableStateOf<VKDialog?>(null) }

    if (selectedDialog != null) {
        val dialog = selectedDialog!!
        val peerId = dialog.conversation.peer.id
        val profile = state.profiles[peerId]
        ChatScreen(
            peerName = profile?.fullName ?: "Диалог",
            peerPhoto = profile?.photo100,
            peerId = peerId,
            onBack = { selectedDialog = null },
            viewModel = viewModel
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = CyberBlue
            )
            state.error != null -> Text(
                state.error ?: "",
                color = ErrorRed,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.dialogs, key = { it.conversation.peer.id }) { dialog ->
                    DialogRow(
                        dialog = dialog,
                        authorName = state.profiles[dialog.conversation.peer.id]?.fullName ?: "Диалог",
                        authorPhoto = state.profiles[dialog.conversation.peer.id]?.photo100,
                        onClick = { selectedDialog = dialog }
                    )
                    HorizontalDivider(
                        color = Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 72.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DialogRow(
    dialog: VKDialog,
    authorName: String,
    authorPhoto: String?,
    onClick: () -> Unit
) {
    val date = remember(dialog.lastMessage.date) {
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(dialog.lastMessage.date * 1000))
    }
    val unread = dialog.conversation.unreadCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = authorPhoto,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(CircleShape).background(SurfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = authorName,
                    color = OnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = date, color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dialog.lastMessage.text.ifBlank { "Вложение" },
                    color = OnSurfaceMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (unread > 0) {
                    Box(
                        modifier = Modifier
                            .background(CyberBlue, RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (unread > 99) "99+" else unread.toString(),
                            color = Background,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
