package com.selfcode.vkplus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.api.VKUserExtended
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(private val repository: VKRepository) : ViewModel() {
    private val _user = MutableStateFlow<VKUserExtended?>(null)
    val user: StateFlow<VKUserExtended?> = _user
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _lastActivity = MutableStateFlow<String?>(null)
    val lastActivity: StateFlow<String?> = _lastActivity
    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked

    fun load(userId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val r = repository.getUserExtended(userId)) {
                is VKResult.Success -> {
                    _user.value = r.data
                    loadActivity(r.data.id)
                }
                is VKResult.Error -> _error.value = r.message
            }
            _loading.value = false
        }
    }

    fun toggleBlock(userId: Int) {
        viewModelScope.launch {
            val currently = _isBlocked.value
            val result = if (currently) repository.unbanUser(userId) else repository.banUser(userId)
            if (result is VKResult.Success) _isBlocked.value = !currently
        }
    }

    private suspend fun loadActivity(userId: Int) {
        when (val r = repository.getLastActivity(userId)) {
            is VKResult.Success -> {
                val activity = r.data
                _lastActivity.value = when {
                    activity.online == 1 -> "онлайн сейчас"
                    activity.time <= 0L -> null // no data — don't show wrong date
                    else -> {
                        val now = System.currentTimeMillis() / 1000
                        val diff = now - activity.time
                        when {
                            diff < 60 -> "был(а) только что"
                            diff < 3600 -> "был(а) ${diff / 60} мин. назад"
                            diff < 86400 -> "был(а) сегодня в " + java.text.SimpleDateFormat("HH:mm", java.util.Locale("ru")).format(java.util.Date(activity.time * 1000))
                            diff < 172800 -> "был(а) вчера в " + java.text.SimpleDateFormat("HH:mm", java.util.Locale("ru")).format(java.util.Date(activity.time * 1000))
                            else -> "был(а) " + java.text.SimpleDateFormat("d MMM в HH:mm", java.util.Locale("ru")).format(java.util.Date(activity.time * 1000))
                        }
                    }
                }
            }
            is VKResult.Error -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onWriteMessage: (Int) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val lastActivity by viewModel.lastActivity.collectAsState()
    val isBlocked by viewModel.isBlocked.collectAsState()

    LaunchedEffect(userId) { viewModel.load(userId) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        TopAppBar(
            title = { Text(user?.fullName ?: "Профиль", color = OnSurface, fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = CyberBlue) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberBlue)
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error ?: "", color = ErrorRed)
            }
            user != null -> {
                val u = user!!
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                    // Cover + avatar
                    Box(modifier = Modifier.fillMaxWidth().height(170.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFF0D1520), Color(0xFF090D16), Background)))) {
                        Box(modifier = Modifier.size(88.dp).align(Alignment.BottomStart).offset(x = 20.dp, y = 44.dp)) {
                            AsyncImage(
                                model = u.photo200 ?: u.photo100, contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    .border(3.dp, Brush.linearGradient(listOf(CyberBlue, CyberAccent)), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (u.isOnline) {
                                Box(modifier = Modifier.size(18.dp).align(Alignment.BottomEnd)
                                    .background(Background, CircleShape).padding(3.dp)
                                    .background(CyberAccent, CircleShape))
                            }
                        }
                    }

                    Spacer(Modifier.height(50.dp))

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(u.fullName, color = OnSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            if (u.isOnline) {
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.background(CyberBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("онлайн", color = CyberBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        // Last activity (only when offline)
                        if (!u.isOnline) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                lastActivity ?: "не в сети",
                                color = OnSurfaceMuted, fontSize = 12.sp
                            )
                        }
                        if (!u.status.isNullOrBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(u.status, color = OnSurfaceMuted, fontSize = 13.sp, lineHeight = 17.sp)
                        }

                        Spacer(Modifier.height(14.dp))

                        // Counters row
                        if (u.followersCount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                CounterChip(formatCount(u.followersCount), "подписчиков")
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        // Write message button
                        Button(
                            onClick = { onWriteMessage(u.id) },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp), tint = Color(0xFF050810))
                            Spacer(Modifier.width(8.dp))
                            Text("Написать сообщение", color = Color(0xFF050810), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Divider)
                        Spacer(Modifier.height(16.dp))

                        // Info section
                        Text("Информация", color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))

                        if (!u.bdate.isNullOrBlank()) ProfileInfoRow(Icons.Filled.Star, "День рождения", u.bdate)
                        if (u.city != null) ProfileInfoRow(Icons.Filled.Notifications, "Город", u.city.title)
                        if (u.occupation != null) {
                            val label = if (u.occupation.type == "work") "Работа" else "Учёба"
                            ProfileInfoRow(Icons.Filled.Settings, label, u.occupation.name)
                        }
                        // Numeric ID with copy
                        val clipboard = LocalClipboardManager.current
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 7.dp)) {
                            Icon(Icons.Filled.Info, null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ID страницы", color = OnSurfaceMuted, fontSize = 11.sp)
                                Text("${u.id}", color = OnSurface, fontSize = 14.sp)
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString("${u.id}")) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Share, null, tint = OnSurfaceMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                        if (u.followersCount > 0) ProfileInfoRow(Icons.Filled.People, "Подписчики", formatCount(u.followersCount))

                        Spacer(Modifier.height(16.dp))

                        // Block/Unblock button
                        var confirmBlock by remember { mutableStateOf(false) }
                        if (confirmBlock) {
                            AlertDialog(
                                onDismissRequest = { confirmBlock = false },
                                containerColor = Surface,
                                title = { Text(if (isBlocked) "Разблокировать?" else "Заблокировать?", color = OnSurface) },
                                text = { Text(
                                    if (isBlocked) "${u.fullName} будет удалён из чёрного списка."
                                    else "${u.fullName} будет добавлен в чёрный список. Он не сможет видеть вашу страницу.",
                                    color = OnSurfaceMuted, fontSize = 13.sp
                                ) },
                                confirmButton = {
                                    TextButton(onClick = { viewModel.toggleBlock(userId.toIntOrNull() ?: 0); confirmBlock = false }) {
                                        Text(if (isBlocked) "Разблокировать" else "Заблокировать",
                                            color = if (isBlocked) CyberBlue else ErrorRed)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmBlock = false }) { Text("Отмена", color = OnSurfaceMuted) }
                                }
                            )
                        }
                        OutlinedButton(
                            onClick = { confirmBlock = true },
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isBlocked) CyberBlue.copy(0.5f) else ErrorRed.copy(0.5f)
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (isBlocked) Icons.Filled.LockOpen else Icons.Filled.Block,
                                null,
                                tint = if (isBlocked) CyberBlue else ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isBlocked) "Разблокировать" else "Заблокировать",
                                color = if (isBlocked) CyberBlue else ErrorRed,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = OnSurfaceMuted, fontSize = 11.sp)
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = CyberBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = OnSurfaceMuted, fontSize = 11.sp)
            Text(value, color = OnSurface, fontSize = 14.sp)
        }
    }
}

fun formatCount(count: Int) = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}K"
    else -> count.toString()
}
