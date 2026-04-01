package com.selfcode.vkplus.ui.screens.messages

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKMessage
import com.selfcode.vkplus.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Reaction emojis: id 1-6 (VK standard)
private val REACTIONS = listOf("👍" to 1, "❤️" to 2, "🔥" to 3, "😂" to 4, "😢" to 5, "👎" to 6)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    peerName: String,
    peerPhoto: String?,
    peerId: Int,
    onBack: () -> Unit,
    viewModel: MessagesViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val messages = state.chatMessages[peerId] ?: emptyList()
    val isSending by viewModel.isSending.collectAsState()
    val translatedMessages by viewModel.translatedMessages.collectAsState()
    val translateLoading by viewModel.translateLoading.collectAsState()
    val lastActivity by viewModel.lastActivity.collectAsState()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<VKMessage?>(null) }
    var replyToMessage by remember { mutableStateOf<VKMessage?>(null) }
    var forwardMessage by remember { mutableStateOf<VKMessage?>(null) }
    var selectedMessage by remember { mutableStateOf<VKMessage?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var isTranslatingInput by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { inputText = "[Файл: ${getFileName(context, it)}]" }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "vkp_voice_${System.currentTimeMillis()}.mp4")
            recordingFile = file
            recorder = createRecorder(context, file)
            recorder?.start()
            isRecording = true
        }
    }

    LaunchedEffect(peerId) {
        viewModel.loadMessages(peerId)
        viewModel.loadLastActivity(peerId)
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Reaction picker overlay
    if (showReactions && selectedMessage != null) {
        val msg = selectedMessage!!
        AlertDialog(
            onDismissRequest = { showReactions = false },
            containerColor = Surface,
            title = { Text("Реакция", color = OnSurface, fontSize = 14.sp) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    REACTIONS.forEach { (emoji, id) ->
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(SurfaceVariant).clickable {
                                viewModel.sendReaction(peerId, msg.id, id)
                                showReactions = false; selectedMessage = null
                            }, contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Action sheet
    if (showActionSheet && selectedMessage != null) {
        val msg = selectedMessage!!
        val isTranslated = translatedMessages.containsKey(msg.id)
        ModalBottomSheet(onDismissRequest = { showActionSheet = false; selectedMessage = null },
            containerColor = Surface, contentColor = OnSurface) {
            Column(Modifier.padding(bottom = 32.dp)) {
                sheetItem("Ответить", Icons.Filled.Reply, CyberBlue) {
                    replyToMessage = msg; showActionSheet = false; selectedMessage = null
                }
                HorizontalDivider(color = Divider)
                sheetItem("Переслать", Icons.Filled.Forward, CyberBlue) {
                    forwardMessage = msg; inputText = "[Пересланное: ${msg.text.take(30)}]"
                    showActionSheet = false; selectedMessage = null
                }
                HorizontalDivider(color = Divider)
                sheetItem("Реакция", Icons.Filled.EmojiEmotions, Color(0xFFFFB800)) {
                    showActionSheet = false; showReactions = true
                }
                HorizontalDivider(color = Divider)
                sheetItem("Копировать", Icons.Filled.ContentCopy, OnSurfaceMuted) {
                    clipboard.setText(AnnotatedString(msg.text)); showActionSheet = false; selectedMessage = null
                }
                HorizontalDivider(color = Divider)
                sheetItem(if (isTranslated) "Скрыть перевод" else "Перевести", Icons.Filled.Translate, CyberBlue) {
                    if (isTranslated) viewModel.clearTranslation(msg.id)
                    else viewModel.translateMessage(msg.id, msg.text)
                    showActionSheet = false; selectedMessage = null
                }
                if (msg.isOutgoing) {
                    HorizontalDivider(color = Divider)
                    sheetItem("Редактировать", Icons.Default.Edit, CyberBlue) {
                        editingMessage = msg; inputText = msg.text; showActionSheet = false; selectedMessage = null
                    }
                    HorizontalDivider(color = Divider)
                    sheetItem("Восстановить", Icons.Filled.Restore, OnSurfaceMuted) {
                        viewModel.restoreMessage(peerId, msg.id); showActionSheet = false; selectedMessage = null
                    }
                    HorizontalDivider(color = Divider)
                    sheetItem("Удалить", Icons.Filled.Delete, ErrorRed) {
                        viewModel.deleteMessage(peerId, msg.id); showActionSheet = false; selectedMessage = null
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // TopBar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = peerPhoto, contentDescription = null,
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(SurfaceVariant),
                        contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(peerName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        val activity = lastActivity[peerId]
                        if (activity != null) Text(activity,
                            color = if (activity == "онлайн сейчас") CyberAccent else OnSurfaceMuted, fontSize = 11.sp)
                    }
                }
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = CyberBlue) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
        )

        // Edit banner
        AnimatedVisibility(visible = editingMessage != null, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
            Banner(icon = Icons.Default.Edit, color = CyberAccent, title = "Редактирование",
                subtitle = editingMessage?.text?.take(50) ?: "") { editingMessage = null; inputText = "" }
        }

        // Reply banner
        AnimatedVisibility(visible = replyToMessage != null && editingMessage == null, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
            Banner(icon = Icons.Filled.Reply, color = CyberBlue, title = "Ответ",
                subtitle = replyToMessage?.text?.take(60) ?: "") { replyToMessage = null }
        }

        // Forward banner
        AnimatedVisibility(visible = forwardMessage != null && replyToMessage == null && editingMessage == null, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
            Banner(icon = Icons.Filled.Forward, color = CyberBlue, title = "Пересланное сообщение",
                subtitle = forwardMessage?.text?.take(60) ?: "") { forwardMessage = null; inputText = "" }
        }

        // Messages list
        Box(modifier = Modifier.weight(1f)) {
            if (state.isChatLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            translation = translatedMessages[msg.id],
                            isTranslating = translateLoading.contains(msg.id),
                            onLongPress = { selectedMessage = msg; showActionSheet = true },
                            onReplyClick = { replyToMessage = it }
                        )
                    }
                }
            }
        }

        // Input bar
        Row(modifier = Modifier.fillMaxWidth().background(Surface).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = { filePicker.launch("*/*") }) {
                Icon(Icons.Filled.AttachFile, null, tint = OnSurfaceMuted, modifier = Modifier.size(22.dp))
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(when { editingMessage != null -> "Редактировать..."; replyToMessage != null -> "Ответить..."; else -> "Сообщение..." }, color = OnSurfaceMuted) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue),
                shape = RoundedCornerShape(20.dp), maxLines = 4
            )
            Spacer(Modifier.width(4.dp))

            if (inputText.isBlank() && editingMessage == null && replyToMessage == null && forwardMessage == null) {
                val recScale by animateFloatAsState(if (isRecording) 1.2f else 1f, spring(stiffness = Spring.StiffnessLow), label = "rs")
                IconButton(onClick = {
                    if (isRecording) {
                        recorder?.apply { stop(); release() }; recorder = null; isRecording = false
                        recordingFile?.let { inputText = "[Голосовое: ${it.name}]"; recordingFile = null }
                    } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }, modifier = Modifier.size(44.dp).scale(recScale).background(if (isRecording) ErrorRed else SurfaceVariant, CircleShape)) {
                    Icon(Icons.Filled.Mic, null, tint = if (isRecording) Color.White else CyberBlue, modifier = Modifier.size(20.dp))
                }
            } else {
                if (inputText.isNotBlank() && editingMessage == null) {
                    IconButton(onClick = {
                        isTranslatingInput = true
                        viewModel.translateInput(inputText, "en") { translated -> inputText = translated; isTranslatingInput = false }
                    }, modifier = Modifier.size(44.dp).background(SurfaceVariant, CircleShape)) {
                        if (isTranslatingInput) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberBlue, strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Translate, null, tint = OnSurfaceMuted, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isBlank()) return@IconButton
                        val editing = editingMessage
                        when {
                            editing != null -> { viewModel.editMessage(peerId, editing.id, text); editingMessage = null }
                            replyToMessage != null -> { viewModel.sendMessage(peerId, text, replyToId = replyToMessage!!.id); replyToMessage = null }
                            forwardMessage != null -> { viewModel.sendMessage(peerId, text, forwardIds = listOf(forwardMessage!!.id)); forwardMessage = null }
                            else -> viewModel.sendMessage(peerId, text)
                        }
                        inputText = ""
                    },
                    enabled = !isSending && inputText.isNotBlank(),
                    modifier = Modifier.size(44.dp).background(if (inputText.isNotBlank()) CyberBlue else SurfaceVariant, CircleShape)
                ) {
                    Icon(if (editingMessage != null) Icons.Default.Check else Icons.Default.Send, null,
                        tint = if (inputText.isNotBlank()) Color(0xFF050810) else OnSurfaceMuted, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Recording indicator
        AnimatedVisibility(visible = isRecording, enter = fadeIn(), exit = fadeOut()) {
            Row(modifier = Modifier.fillMaxWidth().background(ErrorRed.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                val pulse by rememberInfiniteTransition(label = "rp").animateFloat(0.5f, 1f,
                    infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p")
                Box(modifier = Modifier.size(8.dp).scale(pulse).background(ErrorRed, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("Запись голосового...", color = ErrorRed, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { recorder?.apply { stop(); release() }; recorder = null; isRecording = false; recordingFile?.delete(); recordingFile = null }) {
                    Text("Отмена", color = OnSurfaceMuted)
                }
            }
        }
    }
}

@Composable
private fun Banner(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, title: String, subtitle: String, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(color.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = OnSurfaceMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Clear, null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun sheetItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(label, color = if (tint == ErrorRed) ErrorRed else OnSurface) },
        leadingContent = { Icon(icon, null, tint = tint) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Surface))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: VKMessage, translation: String?, isTranslating: Boolean,
    onLongPress: () -> Unit, onReplyClick: (VKMessage) -> Unit) {
    val isOut = message.isOutgoing
    val time = remember(message.date) { SimpleDateFormat("HH:mm", Locale("ru")).format(Date(message.date * 1000)) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (isOut) Alignment.End else Alignment.Start) {
            // Reply quote
            message.replyMessage?.let { reply ->
                Box(modifier = Modifier.widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(CyberBlue.copy(alpha = 0.08f))
                    .clickable { onReplyClick(message) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Column {
                        Text("↩ Ответ", color = CyberBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(reply.text.take(80), color = OnSurfaceMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(1.dp))
            }

            // Forwarded
            if (!message.fwdMessages.isNullOrEmpty()) {
                Box(modifier = Modifier.widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Column {
                        Text("▶ Пересланное", color = OnSurfaceMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        message.fwdMessages.take(2).forEach { fwd ->
                            Text(fwd.text.take(80), color = OnSurface, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(1.dp))
            }

            // Main bubble
            Box(modifier = Modifier.widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = if (!isOut && message.replyMessage == null) 4.dp else 16.dp,
                    topEnd = if (isOut && message.replyMessage == null) 4.dp else 16.dp,
                    bottomStart = if (isOut) 16.dp else 4.dp, bottomEnd = if (isOut) 4.dp else 16.dp))
                .background(if (isOut) CyberBlue.copy(alpha = 0.85f) else SurfaceVariant)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                Column {
                    message.voiceMessage?.let { voice ->
                        VoiceMessagePlayer(audio = voice, isOutgoing = isOut)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (message.text.isNotBlank()) {
                        Text(message.text, color = if (isOut) Color(0xFF050810) else OnSurface, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                    if (isTranslating) { Spacer(Modifier.height(4.dp)); CircularProgressIndicator(modifier = Modifier.size(12.dp), color = if (isOut) Color(0xFF050810) else CyberBlue, strokeWidth = 1.5.dp) }
                    Text(time, color = if (isOut) Color(0xFF050810).copy(alpha = 0.6f) else OnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
                }
            }

            // Reaction display
            if (message.reactionId != 0) {
                val emoji = REACTIONS.find { it.second == message.reactionId }?.first ?: ""
                if (emoji.isNotEmpty()) {
                    Box(modifier = Modifier.offset(x = if (isOut) (-4).dp else 4.dp, y = (-4).dp)
                        .background(SurfaceVariant, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(emoji, fontSize = 14.sp)
                    }
                }
            }

            // Translation
            if (translation != null) {
                Spacer(Modifier.height(2.dp))
                Box(modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(8.dp))
                    .background(CyberBlue.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Column {
                        Text("🌐 Перевод", color = CyberBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(translation, color = OnSurface, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var name = "file"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
    }
    return name
}

private fun createRecorder(context: Context, file: File): MediaRecorder {
    val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
             else @Suppress("DEPRECATION") MediaRecorder()
    return r.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setOutputFile(file.absolutePath)
        prepare()
    }
}
