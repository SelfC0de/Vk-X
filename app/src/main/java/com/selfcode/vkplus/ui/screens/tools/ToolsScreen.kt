package com.selfcode.vkplus.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.ui.screens.settings.SettingsSection
import com.selfcode.vkplus.ui.screens.settings.SettingsDivider
import com.selfcode.vkplus.ui.theme.*

@Composable
fun ToolsScreen(viewModel: ToolsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection("🧹 Управление друзьями") {
                RemoveBannedCard(state, viewModel)
            }
        }

        item {
            SettingsSection("🔓 Bypass") {
                BypassCard()
            }
        }
    }
}

@Composable
private fun RemoveBannedCard(state: ToolsUiState, vm: ToolsViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PersonRemove, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Remove Banned Users", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Удалить удалённые/заблокированные аккаунты из друзей", color = OnSurfaceMuted, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.isScanning -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = CyberBlue, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Сканирование списка друзей...", color = OnSurfaceMuted, fontSize = 13.sp)
                }
            }
            state.isRemoving -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = ErrorRed, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Удаляем... ${state.removedCount}/${state.bannedFriends.size + state.removedCount}", color = OnSurfaceMuted, fontSize = 13.sp)
                }
            }
            state.scanDone && state.bannedFriends.isEmpty() && state.removedCount == 0 -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Список чист — «собачек» не найдено", color = CyberAccent, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text("Сканировать снова", color = CyberBlue)
                }
            }
            state.scanDone && state.removedCount > 0 && state.bannedFriends.isEmpty() -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Удалено ${state.removedCount} аккаунтов", color = CyberAccent, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text("Сканировать снова", color = CyberBlue)
                }
            }
            state.bannedFriends.isNotEmpty() -> {
                Text(
                    text = "Найдено ${state.bannedFriends.size} удалённых аккаунтов:",
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                state.bannedFriends.take(5).forEach { user ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        AsyncImage(
                            model = user.photo100,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(user.fullName, color = OnSurfaceMuted, fontSize = 13.sp)
                            Text(
                                text = if (user.deactivated == "banned") "заблокирован ВК" else "удалён",
                                color = ErrorRed,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                if (state.bannedFriends.size > 5) {
                    Text("...и ещё ${state.bannedFriends.size - 5}", color = OnSurfaceMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.removeAllBanned() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Удалить всех (${state.bannedFriends.size})", color = OnSurface)
                }
            }
            else -> {
                Button(
                    onClick = { vm.scanBannedFriends() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Сканировать", color = CyberBlue)
                }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, color = ErrorRed, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BypassCard() {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Bypass Copy + Reposts", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Копировать и пересылать из закрытых чатов", color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(18.dp))
        }
        SettingsDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.HowToVote, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Read Receipts", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Голосование в анонимных опросах без отображения имени", color = OnSurfaceMuted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = OnSurfaceMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Bypass функции применяются автоматически — ничего включать не нужно.",
                color = OnSurfaceMuted, fontSize = 11.sp, lineHeight = 15.sp
            )
        }
    }
}
