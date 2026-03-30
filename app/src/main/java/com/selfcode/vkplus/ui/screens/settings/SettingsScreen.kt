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
import com.selfcode.vkplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAntiScreenChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Приватность", "Устройство")

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = CyberBlue
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = { Text(title, color = if (selectedTab == i) CyberBlue else OnSurfaceMuted, fontSize = 13.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> PrivacyTab(state, viewModel, onAntiScreenChanged)
            1 -> DeviceTab(state, viewModel)
        }
    }
}

@Composable
private fun PrivacyTab(
    state: SettingsUiState,
    vm: SettingsViewModel,
    onAntiScreenChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSection("🛡 Режим невидимки") {
            SettingsToggle(
                icon = Icons.Default.VisibilityOff,
                title = "UnRead",
                subtitle = "Сообщения остаются непрочитанными",
                checked = state.unRead,
                onCheckedChange = vm::setUnRead
            )
            SettingsDivider()
            SettingsToggle(
                icon = Icons.Default.KeyboardHide,
                title = "UnType",
                subtitle = "Собеседник не видит «Печатает...»",
                checked = state.unType,
                onCheckedChange = vm::setUnType
            )
            SettingsDivider()
            SettingsToggle(
                icon = Icons.Default.WifiOff,
                title = "Force Offline",
                subtitle = "Каждые 5 мин отправляет setOffline",
                checked = state.forceOffline,
                onCheckedChange = vm::setForceOffline
            )
            SettingsDivider()
            SettingsToggle(
                icon = Icons.Default.Ghost,
                title = "Ghost Online",
                subtitle = "Никогда не отправлять account.setOnline",
                checked = state.ghostOnline,
                onCheckedChange = vm::setGhostOnline
            )
        }

        SettingsSection("📡 Антислежка") {
            SettingsToggle(
                icon = Icons.Default.TrackChanges,
                title = "Anti-Telemetry",
                subtitle = "Блокировать stats.vk-portal.net и трекеры",
                checked = state.antiTelemetry,
                onCheckedChange = vm::setAntiTelemetry
            )
            SettingsDivider()
            SettingsToggle(
                icon = Icons.Default.NoPhotography,
                title = "Anti Screen Rec",
                subtitle = "Чёрный экран при скриншоте и записи",
                checked = state.antiScreen,
                onCheckedChange = {
                    vm.setAntiScreen(it)
                    onAntiScreenChanged(it)
                }
            )
        }

        SettingsSection("🔔 Уведомления") {
            SettingsToggle(
                icon = Icons.Default.Edit,
                title = "Type You Push",
                subtitle = "Push когда тебе начинают печатать →👤",
                checked = state.typePush,
                onCheckedChange = vm::setTypePush
            )
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Text("VK+ by SelfCode", color = OnSurfaceMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DeviceTab(state: SettingsUiState, vm: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSection("📱 Device Spoofer") {
            Text(
                text = "Подмена подписи устройства в запросах к API",
                color = OnSurfaceMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            DeviceProfile.entries.forEach { profile ->
                val selected = state.deviceUa == profile.ua
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            profile.label,
                            color = if (selected) CyberBlue else OnSurface,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            profile.ua.take(52) + "…",
                            color = OnSurfaceMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                    }
                }
                if (profile != DeviceProfile.entries.last()) SettingsDivider()
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Divider, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Применяется автоматически ко всем запросам. Влияет на подпись постов и статус устройства.",
                    color = OnSurfaceMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        color = CyberBlue,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (checked) CyberBlue else OnSurfaceMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = OnSurfaceMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Background,
                checkedTrackColor = CyberBlue,
                uncheckedThumbColor = OnSurfaceMuted,
                uncheckedTrackColor = SurfaceVariant
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))
}
