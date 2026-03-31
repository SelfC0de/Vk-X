package com.selfcode.vkplus.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selfcode.vkplus.data.local.DeviceProfile
import com.selfcode.vkplus.data.local.TypeStatus
import com.selfcode.vkplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAntiScreenChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = CyberBlue) {
            listOf("Приватность", "Движок", "Устройство").forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = { Text(title, color = if (selectedTab == i) CyberBlue else OnSurfaceMuted, fontSize = 12.sp) }
                )
            }
        }
        when (selectedTab) {
            0 -> PrivacyTab(state, viewModel, onAntiScreenChanged)
            1 -> EngineTab(state, viewModel)
            2 -> DeviceTab(state, viewModel)
        }
    }
}

@Composable
private fun PrivacyTab(state: SettingsUiState, vm: SettingsViewModel, onAntiScreenChanged: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSection("🛡 Режим невидимки") {
            SettingsToggle(Icons.Default.VisibilityOff, "UnRead",
                "Сообщения остаются непрочитанными", state.unRead, vm::setUnRead)
            SettingsDivider()
            SettingsToggle(Icons.Default.Clear, "UnType",
                "Не отправлять «Печатает...» (блокировка)", state.unType, vm::setUnType)
            SettingsDivider()
            SettingsToggle(Icons.Default.WifiOff, "Force Offline",
                "Каждые 5 мин отправляет account.setOffline", state.forceOffline, vm::setForceOffline)
            SettingsDivider()
            SettingsToggle(Icons.Default.Person, "Ghost Online",
                "Никогда не отправлять account.setOnline", state.ghostOnline, vm::setGhostOnline)
            SettingsDivider()
            SettingsToggle(Icons.Default.VisibilityOff, "Ghost View Story",
                "Просмотр историй без отправки stories.markAsViewed", state.ghostStory, vm::setGhostStory)
        }

        SettingsSection("📡 Антислежка") {
            SettingsToggle(Icons.Default.Notifications, "Anti-Telemetry",
                "Блокировать stats.vk-portal.net и трекеры", state.antiTelemetry, vm::setAntiTelemetry)
            SettingsDivider()
            SettingsToggle(Icons.Default.Lock, "Anti Screen Rec",
                "Чёрный экран при скриншоте и записи", state.antiScreen) {
                vm.setAntiScreen(it); onAntiScreenChanged(it)
            }
        }

        SettingsSection("🔔 Уведомления") {
            SettingsToggle(Icons.Default.Edit, "Type You Push",
                "Push когда тебе начинают печатать →👤", state.typePush, vm::setTypePush)
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Text("VK+ by SelfCode", color = OnSurfaceMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EngineTab(state: SettingsUiState, vm: SettingsViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSection("🎙 Silent VM Listener") {
            SettingsToggle(Icons.Default.Mic, "Silent VM Listener",
                "Слушать ГС без отправки markAsListened — метка не улетает", state.silentVm, vm::setSilentVm)
        }

        SettingsSection("🔄 Anti-Ban Engine") {
            SettingsToggle(Icons.Default.Refresh, "Anti-Ban Engine",
                "Авторотация client_id при капче/rate limit", state.antiBan, vm::setAntiBan)
            SettingsDivider()
            SettingsToggle(Icons.Default.Warning, "Offline Post",
                "Публиковать/отправлять без триггера account.setOnline", state.offlinePost, vm::setOfflinePost)
        }

        SettingsSection("🕵️ Activity Bypass") {
            SettingsToggle(Icons.Default.Visibility, "Bypass Activity Status",
                "Загружать сообщения через execute — не триггерит Online", state.bypassActivity, vm::setBypassActivity)
            SettingsDivider()
            SettingsToggle(Icons.Default.ExitToApp, "Bypass Link Warning",
                "Открывать ссылки напрямую, игнорируя vk.com/away", state.bypassLinks, vm::setBypassLinks)
            SettingsDivider()
            SettingsToggle(Icons.Default.Refresh, "Bypass Link Shorter",
                "HEAD-запрос к vk.cc до клика — показывает реальный URL", state.bypassShortUrl, vm::setBypassShortUrl)
            SettingsDivider()
            SettingsToggle(Icons.Default.Wifi, "LongPoll Only Mode",
                "Получать сообщения без setOnline — вечный оффлайн при чтении", state.longPollOnly, vm::setLongPollOnly)
        }

        SettingsSection("⌨️ Type Status Changer") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Подменить статус активности в чате",
                    color = OnSurfaceMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TypeStatus.entries.forEach { status ->
                    val selected = state.typeStatus == status.value
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { vm.setTypeStatus(status.value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CyberBlue,
                                unselectedColor = OnSurfaceMuted
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${status.emoji}  ${status.label}",
                            color = if (selected) CyberBlue else OnSurface,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = OnSurfaceMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "«Отключено» = UnType (статус не отправляется совсем).\nОстальные варианты подменяют реальный статус.",
                        color = OnSurfaceMuted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Text("VK+ by SelfCode", color = OnSurfaceMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DeviceTab(state: SettingsUiState, vm: SettingsViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSection("📱 Device Spoofer") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Подмена User-Agent во всех запросах к API",
                    color = OnSurfaceMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DeviceProfile.entries.forEach { profile ->
                    val selected = state.deviceUa == profile.ua
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { vm.setDeviceProfile(profile) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CyberBlue,
                                unselectedColor = OnSurfaceMuted
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.label,
                                color = if (selected) CyberBlue else OnSurface,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(profile.ua.take(52) + "…", color = OnSurfaceMuted, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                        if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                    }
                    if (profile != DeviceProfile.entries.last()) SettingsDivider()
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = OnSurfaceMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Применяется ко всем запросам автоматически. Влияет на подпись постов и статус устройства в профиле.",
                        color = OnSurfaceMuted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Text("VK+ by SelfCode", color = OnSurfaceMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(text = title, color = CyberBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun SettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = if (checked) CyberBlue else OnSurfaceMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = OnSurfaceMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Background, checkedTrackColor = CyberBlue,
                uncheckedThumbColor = OnSurfaceMuted, uncheckedTrackColor = SurfaceVariant
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))
}
