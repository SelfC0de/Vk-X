package com.selfcode.vkplus.ui.screens.messages

import android.Manifest
import android.content.Context
import android.content.Intent
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
import com.selfcode.vkplus.data.model.*
import com.selfcode.vkplus.ui.theme.*
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val REACTIONS = listOf("👍" to 1, "❤️" to 2, "🔥" to 3, "😂" to 4, "😢" to 5, "👎" to 6)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    peerName: String,
    peerPhoto: String?,
    peerId: Int,
    onBack: () -> Unit,
    onOpenProfile: ((userId: String) -> Unit)? = null,
    viewModel: MessagesViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val messages = state.chatMessages[peerId] ?: emptyList()
    val profiles = state.profiles
    val isGroupChat = peerId > 2_000_000_000
    val isLoadingOlder = state.isLoadingOlder
    val isSending by viewModel.isSending.collectAsState()
    val translatedMessages by viewModel.translatedMessages.collectAsState()
    val translateLoading by viewModel.translateLoading.collectAsState()
    val lastActivityMap by viewModel.lastActivity.collectAsState()
    val lastActivity: String? = lastActivityMap[peerId]
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
    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !isLoadingOlder) listState.animateScrollToItem(messages.size - 1)
    }
    // Load older when scrolled to top
    val shouldLoadOlder by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && !isLoadingOlder && messages.isNotEmpty() }
    }
    LaunchedEffect(shouldLoadOlder) {
        if (shouldLoadOlder) viewModel.loadOlderMessages(peerId)
    }

    // Reaction picker
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
            containerColor = Surface) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                listOf(
                    "↩ Ответить" to { replyToMessage = msg; showActionSheet = false; selectedMessage = null },
                    "➡ Переслать" to { forwardMessage = msg; showActionSheet = false; selectedMessage = null },
                    "🌐 Перевести" to { viewModel.translateMessage(msg.id, msg.text); showActionSheet = false; selectedMessage = null },
                    "📋 Копировать" to { clipboard.setText(AnnotatedString(msg.text)); showActionSheet = false; selectedMessage = null },
                    "😀 Реакция" to { showReactions = true; showActionSheet = false },
                    "✏ Изменить" to { editingMessage = msg; inputText = msg.text; showActionSheet = false; selectedMessage = null },
                    "🗑 Удалить" to { viewModel.deleteMessage(peerId, msg.id); showActionSheet = false; selectedMessage = null }
                ).forEach { (label, action) ->
                    TextButton(onClick = action, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                        Text(label, color = OnSurface, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.then(
                            if (peerId > 0 && onOpenProfile != null)
                                Modifier.clickable { onOpenProfile.invoke(peerId.toString()) }
                            else Modifier
                        )
                    ) {
                        if (peerPhoto != null) {
                            AsyncImage(model = peerPhoto, contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceVariant),
                                contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            Text(peerName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (lastActivity != null) {
                                Text(lastActivity, color = if (lastActivity == "онлайн сейчас") CyberAccent else OnSurfaceMuted, fontSize = 11.sp)
                            }
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnSurface) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Messages list
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // Load older indicator at top
                    if (isLoadingOlder) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CyberBlue, strokeWidth = 2.dp)
                            }
                        }
                    }
                    items(messages, key = { it.id }) { msg ->
                        val translated = translatedMessages[msg.id]
                        val isTranslating = translateLoading.contains(msg.id)
                        MessageBubble(
                            message = msg,
                            translatedText = translated,
                            isTranslating = isTranslating,
                            onLongClick = { selectedMessage = msg; showActionSheet = true },
                            onAuthorClick = if (isGroupChat && msg.out == 0) {
                                { onOpenProfile?.invoke(msg.fromId.toString()) }
                            } else null,
                            authorProfile = profiles[msg.fromId],
                            showAuthor = isGroupChat && msg.out == 0,
                            context = context
                        )
                    }
                }
            }

            // Reply / Forward preview
            replyToMessage?.let { msg ->
                Row(modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).height(36.dp).background(CyberBlue, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ответ", color = CyberBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(msg.text.take(60), color = OnSurfaceMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { replyToMessage = null }) { Icon(Icons.Filled.Close, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp)) }
                }
            }
            forwardMessage?.let { msg ->
                Row(modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).height(36.dp).background(CyberAccent, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Переслать", color = CyberAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(msg.text.take(60), color = OnSurfaceMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { forwardMessage = null }) { Icon(Icons.Filled.Close, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp)) }
                }
            }
            editingMessage?.let {
                Row(modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).height(36.dp).background(Color(0xFFFF9800), RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Редактировать", color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(it.text.take(60), color = OnSurfaceMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { editingMessage = null; inputText = "" }) { Icon(Icons.Filled.Close, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp)) }
                }
            }

            // Input bar
            Row(modifier = Modifier.fillMaxWidth().background(Surface).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = { filePicker.launch("*/*") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.AttachFile, null, tint = OnSurfaceMuted, modifier = Modifier.size(20.dp))
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение...", color = OnSurfaceMuted, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 5,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
                Spacer(Modifier.width(6.dp))
                if (inputText.isNotBlank()) {
                    IconButton(onClick = {
                        val text = inputText.trim()
                        if (editingMessage != null) {
                            viewModel.editMessage(peerId, editingMessage!!.id, text)
                            editingMessage = null
                        } else {
                            viewModel.sendMessage(peerId, text,
                                replyToId = replyToMessage?.id,
                                forwardIds = forwardMessage?.let { listOf(it.id) })
                            replyToMessage = null; forwardMessage = null
                        }
                        inputText = ""
                    }, enabled = !isSending,
                        modifier = Modifier.size(44.dp).background(CyberBlue, CircleShape)) {
                        Icon(Icons.Filled.Send, null, tint = Background, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(onClick = {
                        if (isRecording) {
                            recorder?.stop(); recorder?.release(); recorder = null; isRecording = false
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }, modifier = Modifier.size(44.dp).background(if (isRecording) ErrorRed else SurfaceVariant, CircleShape)) {
                        Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, null,
                            tint = if (isRecording) Color.White else OnSurfaceMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun MessageBubble(
    message: VKMessage,
    translatedText: String?,
    isTranslating: Boolean,
    onLongClick: () -> Unit,
    onAuthorClick: (() -> Unit)? = null,
    authorProfile: com.selfcode.vkplus.data.model.VKUser? = null,
    showAuthor: Boolean = false,
    context: Context
) {
    val isOut = message.out == 1
    val time = remember(message.date) { SimpleDateFormat("HH:mm", Locale("ru")).format(Date(message.date * 1000)) }
    val bubbleColor = if (isOut) CyberBlue.copy(alpha = 0.15f) else SurfaceVariant
    val bubbleShape = if (isOut) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp) else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isOut) Alignment.End else Alignment.Start
    ) {
        // Author row in group chats
        if (showAuthor && authorProfile != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .then(if (onAuthorClick != null) Modifier.clickable(onClick = onAuthorClick) else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = authorProfile.photo100, contentDescription = null,
                    modifier = Modifier.size(18.dp).clip(CircleShape).background(SurfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(5.dp))
                Text(authorProfile.fullName, color = CyberBlue, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                // Reply
                message.replyMessage?.let { reply ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(CyberBlue, RoundedCornerShape(1.dp)))
                        Spacer(Modifier.width(6.dp))
                        Text(reply.text.take(80), color = CyberBlue.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Attachments
                message.attachments?.forEach { attach ->
                    AttachmentView(attach, context)
                    Spacer(Modifier.height(4.dp))
                }

                // Voice
                message.voiceMessage?.let { voice ->
                    VoiceMessagePlayer(voice, isOutgoing = message.out == 1)
                }

                // Text
                val displayText = translatedText ?: message.text
                if (displayText.isNotBlank()) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp).padding(top = 2.dp), strokeWidth = 1.5.dp, color = CyberBlue)
                    } else {
                        Text(displayText, color = OnSurface, fontSize = 14.sp, lineHeight = 19.sp)
                    }
                    if (translatedText != null) Text("🌐 переведено", color = OnSurfaceMuted, fontSize = 10.sp)
                }

                // Time
                Text(time, color = OnSurfaceMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AttachmentView(attach: VKAttachment, context: Context) {
    var mediaItem by remember { mutableStateOf<MediaPlayItem?>(null) }
    var fullscreenPhoto by remember { mutableStateOf<String?>(null) }
    mediaItem?.let { MediaPlayerDialog(item = it, onDismiss = { mediaItem = null }) }

    // Fullscreen photo viewer
    fullscreenPhoto?.let { photoUrl ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { fullscreenPhoto = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
                    .clickable { fullscreenPhoto = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photoUrl, contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                // Close hint
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    .background(Color.Black.copy(0.5f), CircleShape).padding(8.dp)) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    when (attach.type) {
        "photo" -> attach.photo?.bestSize()?.url?.let { url ->
            AsyncImage(
                model = url, contentDescription = null,
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { fullscreenPhoto = url },
                        onLongClick = {
                            val name = "photo_${System.currentTimeMillis()}.jpg"
                            downloadFile(context, url, name, "image/jpeg")
                        }
                    ),
                contentScale = ContentScale.FillWidth
            )
        }

        "video" -> attach.video?.let { video ->
            val thumb = video.image?.maxByOrNull { it.width * it.height }?.url
            val playUrl = video.files?.bestUrl() ?: video.player
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .combinedClickable(
                    onClick = {
                        if (playUrl != null) mediaItem = MediaPlayItem.Video(playUrl, video.title, thumb)
                        else openUrl(context, "https://vk.com/video${video.ownerId}_${video.id}")
                    },
                    onLongClick = {
                        val dlUrl = video.files?.bestUrl()
                        if (dlUrl != null) downloadFile(context, dlUrl, "${video.title}.mp4", "video/mp4")
                        else Toast.makeText(context, "Прямая ссылка недоступна", Toast.LENGTH_SHORT).show()
                    }
                )) {
                if (thumb != null) AsyncImage(model = thumb, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.size(52.dp).align(Alignment.Center))
                if (video.duration > 0) {
                    val dur = "%d:%02d".format(video.duration / 60, video.duration % 60)
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(dur, color = Color.White, fontSize = 11.sp)
                    }
                }
                if (video.title.isNotBlank()) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(video.title, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        "audio" -> attach.audio?.let { audio ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface)
                .combinedClickable(
                    onClick = { if (audio.url.isNotBlank()) mediaItem = MediaPlayItem.Audio(audio.url, audio.artist, audio.title, audio.duration) },
                    onLongClick = { if (audio.url.isNotBlank()) downloadFile(context, audio.url, "${audio.artist} - ${audio.title}.mp3", "audio/mpeg") }
                ).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MusicNote, null, tint = CyberBlue, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("${audio.artist} — ${audio.title}", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("%d:%02d".format(audio.duration / 60, audio.duration % 60), color = OnSurfaceMuted, fontSize = 11.sp)
                }
                Icon(Icons.Filled.PlayArrow, null, tint = CyberBlue, modifier = Modifier.size(24.dp))
            }
        }

        "doc" -> attach.doc?.let { doc ->
            val isGif = doc.ext.lowercase() == "gif"
            val iconColor = when (doc.ext.lowercase()) {
                "pdf" -> Color(0xFFE53935); "doc","docx" -> Color(0xFF1565C0)
                "xls","xlsx" -> Color(0xFF2E7D32); "zip","rar","7z" -> Color(0xFFFF6F00)
                "mp4","avi","mov","gif" -> Color(0xFF6A1B9A); else -> OnSurfaceMuted
            }
            if (isGif && doc.url.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { mediaItem = MediaPlayItem.Gif(doc.url) },
                        onLongClick = { downloadFile(context, doc.url, doc.title.ifBlank { "anim.gif" }, "image/gif") }
                    )) {
                    AsyncImage(model = doc.url, contentDescription = "GIF", modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp))
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp).background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text("GIF", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface)
                    .combinedClickable(
                        onClick = { openUrl(context, doc.url) },
                        onLongClick = { downloadFile(context, doc.url, doc.title.ifBlank { "file.${doc.ext}" }, mimeFromExt(doc.ext)) }
                    ).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.InsertDriveFile, null, tint = iconColor, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(doc.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val sz = when { doc.size > 1_048_576 -> "%.1f МБ".format(doc.size/1_048_576f); doc.size > 1024 -> "%.0f КБ".format(doc.size/1024f); else -> "${doc.size} Б" }
                        Text("${doc.ext.uppercase()} · $sz", color = OnSurfaceMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Filled.Download, null, tint = CyberBlue, modifier = Modifier.size(22.dp))
                }
            }
        }

        "sticker" -> attach.sticker?.let { sticker ->
            sticker.bestImage()?.let { img ->
                AsyncImage(model = img.url, contentDescription = "Стикер", modifier = Modifier.size(120.dp), contentScale = ContentScale.Fit)
            }
        }

        "link" -> attach.link?.let { link ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface)
                .clickable { openUrl(context, link.realUrl) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Link, null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    if (!link.title.isNullOrBlank()) Text(link.title, color = OnSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(link.realUrl, color = CyberBlue, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        "graffiti" -> attach.graffiti?.let { g ->
            AsyncImage(model = g.url, contentDescription = "Граффити",
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = {}, onLongClick = { downloadFile(context, g.url, "graffiti.png", "image/png") }))
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun getFileName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst(); if (idx >= 0) it.getString(idx) else "file"
    } ?: "file"
}

private fun createRecorder(context: Context, file: File): MediaRecorder =
    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()).apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setOutputFile(file.absolutePath)
        prepare()
    }
