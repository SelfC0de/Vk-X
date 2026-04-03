package com.selfcode.vkplus.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.ui.screens.settings.SettingsDivider
import com.selfcode.vkplus.ui.screens.settings.SettingsSection
import com.selfcode.vkplus.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

@Composable
fun ToolsScreen(viewModel: ToolsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SettingsSection("🧹 Управление друзьями") { RemoveBannedCard(state, viewModel) } }
        item { SettingsSection("🎁 Bypass Gift Anonymity") { GiftAnonymityCard(state, viewModel) } }
        item { SettingsSection("🔓 Bypass") { BypassInfoCard() } }
        item { SettingsSection("💱 Currency Exchange") { CurrencyExchangeCard(state, viewModel) } }
    }
}

@Composable
private fun RemoveBannedCard(state: ToolsUiState, vm: ToolsViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Person, null, tint = ErrorRed, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Remove Banned Users", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Удалить удалённые/заблокированные аккаунты", color = OnSurfaceMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            state.isScanning -> LoadingRow("Сканирование списка друзей...", CyberBlue)
            state.isRemoving -> LoadingRow("Удаляем... ${state.removedCount}/${state.bannedFriends.size + state.removedCount}", ErrorRed)
            state.scanDone && state.bannedFriends.isEmpty() && state.removedCount == 0 -> {
                SuccessRow("Список чист — «собачек» не найдено")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text("Сканировать снова", color = CyberBlue)
                }
            }
            state.scanDone && state.removedCount > 0 && state.bannedFriends.isEmpty() -> {
                SuccessRow("Удалено ${state.removedCount} аккаунтов")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text("Сканировать снова", color = CyberBlue)
                }
            }
            state.bannedFriends.isNotEmpty() -> {
                Text("Найдено ${state.bannedFriends.size} удалённых:", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                state.bannedFriends.take(5).forEach { user ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        AsyncImage(model = user.photo100, contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceVariant), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(user.fullName, color = OnSurfaceMuted, fontSize = 13.sp)
                            Text(if (user.deactivated == "banned") "заблокирован ВК" else "удалён", color = ErrorRed, fontSize = 11.sp)
                        }
                    }
                }
                if (state.bannedFriends.size > 5) Text("...и ещё ${state.bannedFriends.size - 5}", color = OnSurfaceMuted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.removeAllBanned() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Удалить всех (${state.bannedFriends.size})", color = OnSurface)
                }
            }
            else -> {
                Button(onClick = { vm.scanBannedFriends() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Filled.Search, null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Сканировать", color = CyberBlue)
                }
            }
        }
        if (state.error != null) { Spacer(Modifier.height(8.dp)); Text(state.error!!, color = ErrorRed, fontSize = 12.sp) }
    }
}

@Composable
private fun GiftAnonymityCard(state: ToolsUiState, vm: ToolsViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, null, tint = CyberAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Bypass Gift Anonymity", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Деанон отправителя анонимного подарка через API", color = OnSurfaceMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            state.isLoadingGifts -> LoadingRow("Запрашиваем подарки...", CyberBlue)
            state.giftScanDone && state.doxxedGifts.isEmpty() -> {
                SuccessRow("Анонимных подарков с раскрытым ID не найдено")
            }
            state.doxxedGifts.isNotEmpty() -> {
                Text("🎯 Найдено ${state.doxxedGifts.size} раскрытых отправителей:", color = CyberAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                state.doxxedGifts.forEach { info ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        AsyncImage(model = info.gift.gift?.thumb256, contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceVariant), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("ID отправителя: ${info.realSenderId}", color = CyberAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("vk.com/id${info.realSenderId}", color = OnSurfaceMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
            else -> {
                Button(onClick = { vm.scanGifts() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Filled.Search, null, tint = CyberAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Сканировать подарки", color = CyberAccent)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = OnSurfaceMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Работает только если API вернул from_id в ответе (старые подарки)", color = OnSurfaceMuted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun BypassInfoCard() {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Share, null, tint = CyberBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Bypass Copy + Reposts", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Долгое нажатие на любое сообщение копирует текст", color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Icon(Icons.Filled.CheckCircle, null, tint = CyberAccent, modifier = Modifier.size(18.dp))
        }
        SettingsDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ThumbUp, null, tint = CyberBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Read Receipts", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Голосование в анонимных опросах без отображения имени", color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Icon(Icons.Filled.CheckCircle, null, tint = CyberAccent, modifier = Modifier.size(18.dp))
        }
        SettingsDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Mic, null, tint = CyberBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Silent VM Listener", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Воспроизведение ГС без markAsListened — метка не улетает", color = OnSurfaceMuted, fontSize = 12.sp)
            }
            Icon(Icons.Filled.CheckCircle, null, tint = CyberAccent, modifier = Modifier.size(18.dp))
        }
    }
}

private val ALL_CURRENCIES = listOf(
    "USD", "EUR", "RUB", "GBP", "CNY", "JPY", "AED", "TRY", "KZT", "BYN",
    "CHF", "CAD", "AUD", "SEK", "NOK", "PLN", "CZK", "HUF", "INR", "BRL"
)

@Composable
private fun CurrencyExchangeCard(state: ToolsUiState, vm: ToolsViewModel) {
    var baseExpanded   by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CurrencyExchange, null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Currency Exchange", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Актуальный курс валют через FreeCurrencyAPI", color = OnSurfaceMuted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Base + Target selectors
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Base
            Column(Modifier.weight(1f)) {
                Text("Base Currency", color = OnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceVariant)
                            .border(1.dp, Divider, RoundedCornerShape(10.dp))
                            .clickable { baseExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.currencyBase, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = baseExpanded, onDismissRequest = { baseExpanded = false }) {
                        ALL_CURRENCIES.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur, color = if (cur == state.currencyBase) CyberBlue else OnSurface) },
                                onClick = { vm.setCurrencyBase(cur); baseExpanded = false }
                            )
                        }
                    }
                }
            }

            // Swap icon
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 22.dp)) {
                Icon(Icons.Filled.SyncAlt, null, tint = OnSurfaceMuted, modifier = Modifier.size(20.dp)
                    .clickable { vm.setCurrencyBase(state.currencyTarget); vm.setCurrencyTarget(state.currencyBase) })
            }

            // Target
            Column(Modifier.weight(1f)) {
                Text("To Currency", color = OnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceVariant)
                            .border(1.dp, Divider, RoundedCornerShape(10.dp))
                            .clickable { targetExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.currencyTarget, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = targetExpanded, onDismissRequest = { targetExpanded = false }) {
                        ALL_CURRENCIES.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur, color = if (cur == state.currencyTarget) CyberBlue else OnSurface) },
                                onClick = { vm.setCurrencyTarget(cur); targetExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Result card
        when {
            state.currencyLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = CyberBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Получаем курс...", color = OnSurfaceMuted, fontSize = 13.sp)
                }
            }
            state.currencyResult != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBlue.copy(alpha = 0.08f))
                        .border(1.dp, CyberBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text("1 ${state.currencyBase} =", color = OnSurfaceMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "%.4f".format(state.currencyResult),
                            color = CyberBlue,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(state.currencyTarget, color = OnSurfaceMuted, fontSize = 18.sp,
                            fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "100 ${state.currencyBase} = ${"%.2f".format(state.currencyResult!! * 100)} ${state.currencyTarget}",
                        color = OnSurfaceMuted, fontSize = 12.sp
                    )
                }
            }
            state.currencyError != null -> {
                Text("⚠ ${state.currencyError}", color = ErrorRed, fontSize = 12.sp)
            }
            else -> {
                Text("Выберите валюты и нажмите Получить курс", color = OnSurfaceMuted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { vm.fetchExchangeRate() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.currencyLoading,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
        ) {
            Icon(Icons.Filled.Refresh, null, tint = Background, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Получить курс", color = Background, fontWeight = FontWeight.SemiBold)
        }
    }
}


@Composable
private fun LoadingRow(text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = color, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, color = OnSurfaceMuted, fontSize = 13.sp)
    }
}

@Composable
private fun SuccessRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, tint = CyberAccent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = CyberAccent, fontSize = 13.sp)
    }
}
