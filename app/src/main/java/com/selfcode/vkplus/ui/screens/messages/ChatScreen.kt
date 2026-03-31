package com.selfcode.vkplus.ui.screens.messages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var inputText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<VKMessage?>(null) }
    var selectedMessage by remember { mutableStateOf<VKMessage?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val name = getFileName(context, it)
            inputText = "[Файл: $name]"
        }
    }

    // Permission launcher for audio recording
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "vkplus_voice_${System.currentTimeMillis()}.mp4")
            recordingFile = file
            recorder = createRecorder(context, file)
            recorder?.start()
            isRecording = true
        }
    }

    LaunchedEffect(peerId) { viewModel.loadMessages(peerId) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Message action bottom sheet
    if (showActionSheet && selectedMessage != null) {
        val msg = selectedMessage!!
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false; selectedMessage = null },
            containerColor = Surface,
            contentColor = OnSurface
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                if (msg.text.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("Копировать", color = OnSurface) },
                        leadingContent = { Icon(Icons.Filled.Share, null, tint = CyberBlue) },
                        modifier = Modifier.clickable {
                            clipboard.setText(AnnotatedString(msg.text))
                            showActionSheet = false; selectedMessage = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Surface)
                    )
                    HorizontalDivider(color = Divider)
                }
                if (msg.isOutgoing) {
                    ListItem(
                        headlineContent = { Text("Редактировать", color = OnSurface) },
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = CyberBlue) },
                        modifier = Modifier.clickable {
                            editingMessage = msg
                            inputText = msg.text
                            showActionSheet = false; selectedMessage = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Surface)
                    )
                    HorizontalDivider(color = Divider)
                    ListItem(
                        headlineContent = { Text("Удалить", color = ErrorRed) },
                        leadingContent = { Icon(Icons.Filled.Delete, null, tint = ErrorRed) },
                        modifier = Modifier.clickable {
                            viewModel.deleteMessage(peerId, msg.id)
                            showActionSheet = false; selectedMessage = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Surface)
                    )
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
                    Text(peerName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = CyberBlue)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
        )

        // Edit banner
        AnimatedVisibility(visible = editingMessage != null, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(CyberBlue.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Редактирование", color = CyberBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(editingMessage?.text?.take(40) ?: "", color = OnSurfaceMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { editingMessage = null; inputText = "" }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            if (state.isChatLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            onLongPress = {
                                selectedMessage = msg
                                showActionSheet = true
                            }
                        )
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attach file button
            IconButton(onClick = { filePicker.launch("*/*") }) {
                Icon(Icons.Filled.Share, "Прикрепить файл", tint = OnSurfaceMuted, modifier = Modifier.size(22.dp))
            }

            // Text input
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(if (editingMessage != null) "Редактировать..." else "Сообщение...", color = OnSurfaceMuted) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (editingMessage != null) CyberAccent else CyberBlue,
                    unfocusedBorderColor = Divider,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4
            )

            Spacer(Modifier.width(4.dp))

            if (inputText.isBlank() && editingMessage == null) {
                // Voice record button
                val recordScale by animateFloatAsState(
                    targetValue = if (isRecording) 1.2f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow), label = "recScale"
                )
                IconButton(
                    onClick = {
                        if (isRecording) {
                            // Stop recording
                            recorder?.apply { stop(); release() }
                            recorder = null
                            isRecording = false
                            recordingFile?.let { f ->
                                inputText = "[Голосовое: ${f.name}]"
                                recordingFile = null
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.size(44.dp).scale(recordScale)
                        .background(
                            if (isRecording) ErrorRed else SurfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(Icons.Filled.Mic, "Голосовое", tint = if (isRecording) Color.White else CyberBlue, modifier = Modifier.size(20.dp))
                }
            } else {
                // Send / Save edit button
                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isBlank()) return@IconButton
                        val editing = editingMessage
                        if (editing != null) {
                            viewModel.editMessage(peerId, editing.id, text)
                            editingMessage = null
                        } else {
                            viewModel.sendMessage(peerId, text)
                        }
                        inputText = ""
                    },
                    enabled = !isSending && inputText.isNotBlank(),
                    modifier = Modifier.size(44.dp).background(
                        if (inputText.isNotBlank()) CyberBlue else SurfaceVariant,
                        CircleShape
                    )
                ) {
                    Icon(
                        if (editingMessage != null) Icons.Default.Check else Icons.Default.Send,
                        null,
                        tint = if (inputText.isNotBlank()) Color(0xFF050810) else OnSurfaceMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Recording indicator
        AnimatedVisibility(visible = isRecording, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(ErrorRed.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pulse by rememberInfiniteTransition(label = "recPulse").animateFloat(
                    0.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse"
                )
                Box(modifier = Modifier.size(8.dp).scale(pulse).background(ErrorRed, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("Запись голосового...", color = ErrorRed, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    recorder?.apply { stop(); release() }
                    recorder = null
                    isRecording = false
                    recordingFile?.delete()
                    recordingFile = null
                }) { Text("Отмена", color = OnSurfaceMuted) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: VKMessage, onLongPress: () -> Unit) {
    val isOut = message.isOutgoing
    val time = remember(message.date) {
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(message.date * 1000))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isOut) 16.dp else 4.dp,
                    bottomEnd = if (isOut) 4.dp else 16.dp
                ))
                .background(if (isOut) CyberBlue.copy(alpha = 0.85f) else SurfaceVariant)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                message.voiceMessage?.let { voice ->
                    VoiceMessagePlayer(audio = voice, isOutgoing = isOut)
                    Spacer(Modifier.height(4.dp))
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = if (isOut) Color(0xFF050810) else OnSurface,
                        fontSize = 14.sp, lineHeight = 20.sp
                    )
                }
                Text(
                    text = time,
                    color = if (isOut) Color(0xFF050810).copy(alpha = 0.6f) else OnSurfaceMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
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
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setOutputFile(file.absolutePath)
        prepare()
    }
}


