package com.selfcode.vkplus.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.local.SettingsStore
import com.selfcode.vkplus.ui.screens.exploits.ExploitsViewModel
import com.selfcode.vkplus.ui.screens.messages.ChatScreen
import com.selfcode.vkplus.ui.screens.messages.MessagesViewModel
import com.selfcode.vkplus.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    messagesViewModel: MessagesViewModel = hiltViewModel(),
    exploitsViewModel: ExploitsViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val exploitsState by exploitsViewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showArchive by remember { mutableStateOf(false) }
    var copyToast by remember { mutableStateOf(false) }

    LaunchedEffect(copyToast) {
        if (copyToast) {
            kotlinx.coroutines.delay(2000)
            copyToast = false
        }
    }

    if (showArchive && user != null) {
        ChatScreen(
            peerName = "Избранное",
            peerPhoto = user?.photo100,
            peerId = user!!.id,
            onBack = { showArchive = false },
            viewModel = messagesViewModel
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)
            user == null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Не удалось загрузить профиль", color = ErrorRed)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.loadProfile() }, colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)) {
                    Text("Повторить", color = Color(0xFF050810))
                }
            }
            else -> {
                val u = user!!
                val profileUrl = "https://vk.com/id${u.id}"

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // Cover
                    Box(modifier = Modifier.fillMaxWidth().height(170.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFF0A1020), Color(0xFF0D1528), Background)))) {
                        Box(modifier = Modifier.size(90.dp).align(Alignment.BottomStart).offset(x = 20.dp, y = 45.dp)) {
                            AsyncImage(model = u.photo200, contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    .border(3.dp, Brush.linearGradient(listOf(CyberBlue, CyberAccent)), CircleShape),
                                contentScale = ContentScale.Crop)
                            Box(modifier = Modifier.size(18.dp).align(Alignment.BottomEnd)
                                .background(Background, CircleShape).padding(3.dp).background(CyberAccent, CircleShape))
                        }
                    }

                    Spacer(Modifier.height(52.dp))

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        // Name + fake verification badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(u.fullName, color = OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (exploitsState.fakeVerification) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.CheckCircle, null, tint = CyberAccent, modifier = Modifier.size(20.dp))
                            }
                        }

                        if (!u.status.isNullOrBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(u.status, color = OnSurfaceMuted, fontSize = 13.sp)
                        }

                        Spacer(Modifier.height(4.dp))

                        // Profile link with copy button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("vk.com/id${u.id}", color = CyberBlue, fontSize = 13.sp,
                                modifier = Modifier.clickable {
                                    clipboard.setText(AnnotatedString(profileUrl))
                                    copyToast = true
                                })
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = { clipboard.setText(AnnotatedString(profileUrl)); copyToast = true },
                                modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Filled.Share, null, tint = OnSurfaceMuted, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Buttons row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showArchive = true },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(CyberBlue)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberBlue)
                            ) {
                                Icon(Icons.Filled.Star, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Избранное", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Divider)
                        Spacer(Modifier.height(16.dp))

                        Text("Информация", color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))

                        ProfileInfoRow(Icons.Filled.Info, "ID", "id${u.id}")
                        ProfileInfoRow(Icons.Filled.Person, "Страница", "vk.com/id${u.id}")
                        u.city?.let { ProfileInfoRow(Icons.Filled.Notifications, "Город", it.title) }

                        // Last seen when offline mode enabled
                        if (u.lastSeen != null && !u.isOnline) {
                            val lastSeenStr = remember(u.lastSeen.time) {
                                val now = System.currentTimeMillis() / 1000
                                val diff = now - u.lastSeen.time
                                when {
                                    diff < 60 -> "только что"
                                    diff < 3600 -> "${diff / 60} мин. назад"
                                    diff < 86400 -> "сегодня в " + SimpleDateFormat("HH:mm", Locale("ru")).format(Date(u.lastSeen.time * 1000))
                                    diff < 172800 -> "вчера в " + SimpleDateFormat("HH:mm", Locale("ru")).format(Date(u.lastSeen.time * 1000))
                                    else -> SimpleDateFormat("d MMM в HH:mm", Locale("ru")).format(Date(u.lastSeen.time * 1000))
                                }
                            }
                            ProfileInfoRow(Icons.Filled.Visibility, "Последний визит", lastSeenStr)
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Divider)
                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(ErrorRed)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                        ) {
                            Icon(Icons.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Выйти из аккаунта")
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }

        // Copy toast
        AnimatedVisibility(
            visible = copyToast,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Box(modifier = Modifier.background(SurfaceVariant, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Ссылка скопирована", color = OnSurface, fontSize = 13.sp)
            }
        }
    }
}
