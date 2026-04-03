package com.selfcode.vkplus.ui.screens.proxy

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.local.ProxyConfig
import com.selfcode.vkplus.data.local.ProxyStorage
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

@HiltViewModel
class ProxyViewModel @Inject constructor(
    private val storage: ProxyStorage
) : ViewModel() {

    val list     = storage.list.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeId = storage.activeId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun add(cfg: ProxyConfig) = viewModelScope.launch { storage.addProxy(cfg) }
    fun remove(id: String)    = viewModelScope.launch { storage.removeProxy(id) }
    fun setActive(id: String) = viewModelScope.launch {
        storage.setActive(if (activeId.value == id) "" else id)
    }

    fun ping(cfg: ProxyConfig) = viewModelScope.launch {
        if (cfg.id.isBlank() || cfg.host.isBlank()) return@launch
        val ms = withContext(Dispatchers.IO) { tcpPing(cfg.host, cfg.port) }
        storage.updatePing(cfg.id, ms)
    }

    fun pingLatest() = viewModelScope.launch {
        kotlinx.coroutines.delay(500)
        val last = list.value.lastOrNull() ?: return@launch
        if (last.host.isBlank() || last.id.isBlank()) return@launch
        val ms = withContext(Dispatchers.IO) { tcpPing(last.host, last.port) }
        storage.updatePing(last.id, ms)
    }

    fun pingAll() = viewModelScope.launch {
        list.value.filter { it.host.isNotBlank() && it.id.isNotBlank() }.forEach { cfg ->
            launch {
                val ms = withContext(Dispatchers.IO) { tcpPing(cfg.host, cfg.port) }
                storage.updatePing(cfg.id, ms)
            }
        }
    }

    private fun tcpPing(host: String, port: Int): Long {
        return try {
            val sock = Socket()
            val t = System.currentTimeMillis()
            sock.connect(InetSocketAddress(host, port), 3000)
            val elapsed = System.currentTimeMillis() - t
            try { sock.close() } catch (_: Exception) {}
            elapsed
        } catch (e: Exception) { -1L }
    }

    fun parseLink(raw: String): ProxyConfig? {
        return try {
            val trimmed = raw.trim()
            val uri = Uri.parse(trimmed)
            when {
                // tg://proxy?server=... OR https://t.me/proxy?server=...
                (uri.scheme == "tg" && uri.host == "proxy") ||
                (uri.scheme == "https" && uri.host == "t.me" && uri.pathSegments.firstOrNull() == "proxy") -> {
                    val server = uri.getQueryParameter("server") ?: return null
                    val port   = uri.getQueryParameter("port")?.toIntOrNull() ?: 443
                    val secret = uri.getQueryParameter("secret") ?: ""
                    val clean  = if (secret.startsWith("ee")) secret.substring(2) else secret
                    ProxyConfig(host = server, port = port, type = "SOCKS5", secret = clean)
                }
                uri.scheme == "vkplus" && uri.host == "proxy" -> {
                    val host   = uri.getQueryParameter("host") ?: return null
                    val port   = uri.getQueryParameter("port")?.toIntOrNull() ?: 1080
                    val type   = uri.getQueryParameter("type")?.uppercase() ?: "SOCKS5"
                    val user   = uri.getQueryParameter("user") ?: ""
                    val pass   = uri.getQueryParameter("pass") ?: ""
                    val secret = uri.getQueryParameter("secret") ?: ""
                    ProxyConfig(host = host, port = port, type = type, user = user, pass = pass, secret = secret)
                }
                trimmed.contains(":") && !trimmed.contains("://") -> {
                    val parts = trimmed.split(":")
                    ProxyConfig(host = parts[0], port = parts.getOrNull(1)?.toIntOrNull() ?: 1080)
                }
                else -> null
            }
        } catch (e: Exception) { null }
    }

    fun buildDeepLink(cfg: ProxyConfig): String {
        val sb = StringBuilder("vkplus://proxy?host=${cfg.host}&port=${cfg.port}&type=${cfg.type}")
        if (cfg.user.isNotBlank()) sb.append("&user=${cfg.user}&pass=${cfg.pass}")
        if (cfg.secret.isNotBlank()) sb.append("&secret=${cfg.secret}")
        return sb.toString()
    }
}

@Composable
fun ProxyScreen(viewModel: ProxyViewModel = hiltViewModel()) {
    val list     by viewModel.list.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    val context  = LocalContext.current

    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {

        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Прокси", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            if (list.isNotEmpty()) {
                IconButton(onClick = { viewModel.pingAll() }) {
                    Icon(Icons.Filled.NetworkCheck, null, tint = CyberBlue, modifier = Modifier.size(22.dp))
                }
            }
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, null, tint = CyberBlue, modifier = Modifier.size(24.dp))
            }
        }

        if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.VpnKeyOff, null, tint = OnSurfaceMuted, modifier = Modifier.size(48.dp))
                    Text("Нет прокси", color = OnSurfaceMuted, fontSize = 14.sp)
                    Text("Нажмите + чтобы добавить", color = OnSurfaceMuted, fontSize = 12.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                list.forEach { cfg ->
                    ProxyItem(
                        cfg = cfg,
                        isActive = cfg.id == activeId,
                        onActivate = { viewModel.setActive(cfg.id) },
                        onPing = { viewModel.ping(cfg) },
                        onDelete = { viewModel.remove(cfg.id) },
                        onCopyLink = {
                            val link = viewModel.buildDeepLink(cfg)
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("proxy", link))
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showAdd) {
        AddProxyDialog(
            onDismiss = { showAdd = false },
            onAdd = { cfg ->
                viewModel.add(cfg)
                showAdd = false
                viewModel.pingLatest()
            },
            parseLink = viewModel::parseLink
        )
    }
}

@Composable
private fun ProxyItem(
    cfg: ProxyConfig,
    isActive: Boolean,
    onActivate: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit,
    onCopyLink: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) CyberBlue.copy(0.10f) else Surface)
            .border(
                1.dp,
                if (isActive) CyberBlue.copy(0.5f) else Divider,
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator dot
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape)
                    .background(
                        when {
                            isActive && cfg.pingMs > 0 -> pingColor(cfg.pingMs)
                            isActive -> CyberBlue
                            else -> OnSurfaceMuted.copy(0.3f)
                        }
                    )
            )
            Spacer(Modifier.width(10.dp))

            // Info
            Column(Modifier.weight(1f)) {
                Text("${cfg.host}:${cfg.port}", color = OnSurface, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(cfg.type)
                    if (cfg.pingMs > 0) {
                        Text("${cfg.pingMs} ms", color = pingColor(cfg.pingMs), fontSize = 12.sp,
                            fontWeight = FontWeight.Medium)
                    } else if (cfg.pingMs == -1L && cfg.host.isNotBlank()) {
                        Text("—", color = OnSurfaceMuted, fontSize = 12.sp)
                    }
                }
            }

            // Ping button
            IconButton(onClick = onPing, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Speed, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            }

            // Expand
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded actions
        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(SurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Activate/Deactivate
                OutlinedButton(
                    onClick = onActivate,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isActive) OnSurfaceMuted else CyberBlue
                        )
                    )
                ) {
                    Icon(
                        if (isActive) Icons.Filled.VpnKeyOff else Icons.Filled.VpnKey,
                        null,
                        tint = if (isActive) OnSurfaceMuted else CyberBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isActive) "Отключить" else "Активировать",
                        color = if (isActive) OnSurfaceMuted else CyberBlue,
                        fontSize = 12.sp
                    )
                }

                // Copy link
                IconButton(
                    onClick = onCopyLink,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariant)
                        .border(1.dp, Divider, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Filled.ContentCopy, null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(ErrorRed.copy(0.08f))
                        .border(1.dp, ErrorRed.copy(0.3f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Filled.Delete, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: String) {
    Text(
        type,
        color = CyberAccent,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyberAccent.copy(0.12f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

private fun pingColor(ms: Long): Color = when {
    ms < 150  -> Color(0xFF4CAF50)
    ms < 400  -> Color(0xFFFFAB40)
    else      -> Color(0xFFF44336)
}

@Composable
private fun AddProxyDialog(
    onDismiss: () -> Unit,
    onAdd: (ProxyConfig) -> Unit,
    parseLink: (String) -> ProxyConfig?
) {
    var linkInput  by remember { mutableStateOf("") }
    var host       by remember { mutableStateOf("") }
    var port       by remember { mutableStateOf("") }
    var type       by remember { mutableStateOf("SOCKS5") }
    var user       by remember { mutableStateOf("") }
    var pass       by remember { mutableStateOf("") }
    var secret     by remember { mutableStateOf("") }
    var showPass   by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var tab        by remember { mutableStateOf(0) } // 0=ссылка, 1=ручной
    val context    = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text("Добавить прокси", color = OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Tab switcher
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("По ссылке", "Вручную").forEachIndexed { i, label ->
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (tab == i) CyberBlue else Color.Transparent)
                                .clickable { tab = i }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label,
                                color = if (tab == i) Background else OnSurfaceMuted,
                                fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (tab == 0) {
                    // Import via link
                    Text("tg://proxy?... · vkplus://proxy?... · host:port",
                        color = OnSurfaceMuted, fontSize = 11.sp)
                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it; parseError = null },
                        placeholder = { Text("Вставьте ссылку", color = OnSurfaceMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val txt = cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                                    if (txt.isNotBlank()) { linkInput = txt; parseError = null }
                                }) {
                                    Icon(Icons.Filled.ContentPaste, null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                                }
                                if (linkInput.isNotBlank()) {
                                    IconButton(onClick = { linkInput = ""; parseError = null }) {
                                        Icon(Icons.Filled.Clear, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        },
                        colors = proxyFieldColors()
                    )
                    if (parseError != null) {
                        Text(parseError!!, color = ErrorRed, fontSize = 12.sp)
                    }
                    // Preview parsed
                    if (linkInput.isNotBlank()) {
                        val preview = parseLink(linkInput)
                        if (preview != null) {
                            Text("✓ ${preview.host}:${preview.port} · ${preview.type}",
                                color = Color(0xFF4CAF50), fontSize = 12.sp)
                        }
                    }
                } else {
                    // Manual
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SOCKS5", "HTTP").forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberBlue,
                                    selectedLabelColor = Background,
                                    containerColor = SurfaceVariant,
                                    labelColor = OnSurfaceMuted
                                )
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProxyField("Хост / IP", host, Modifier.weight(1f)) { host = it }
                        ProxyField("Порт", port, Modifier.width(80.dp), KeyboardType.Number) { port = it }
                    }
                    ProxyField("Логин", user) { user = it }
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Пароль", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                            }
                        },
                        colors = proxyFieldColors()
                    )
                    ProxyField("Secret / MTProto", secret) { secret = it }
                    if (secret.isNotBlank()) {
                        Text("FakeTLS: ${decodeMTSecret(secret)}", color = OnSurfaceMuted, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tab == 0) {
                        val parsed = parseLink(linkInput)
                        if (parsed != null) onAdd(parsed)
                        else parseError = "Не удалось распознать ссылку"
                    } else {
                        if (host.isBlank() || port.isBlank()) return@Button
                        onAdd(ProxyConfig(
                            host = host, port = port.toIntOrNull() ?: 1080,
                            type = type, user = user, pass = pass, secret = secret
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
            ) { Text("Добавить", color = Background) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceMuted) }
        }
    )
}

@Composable
private fun ProxyField(
    label: String, value: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = proxyFieldColors()
    )
}

@Composable
private fun proxyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
    focusedLabelColor = CyberBlue, unfocusedLabelColor = OnSurfaceMuted,
    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
)

private fun decodeMTSecret(secret: String): String {
    return try {
        val hex = if (secret.startsWith("ee")) secret.substring(2) else secret
        val bytes = hex.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
        val str = String(bytes).filter { it.code in 32..126 }
        if (str.length > 4) str else "нет"
    } catch (e: Exception) { "нет" }
}
