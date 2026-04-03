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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProxyViewModel @Inject constructor(
    private val storage: ProxyStorage
) : ViewModel() {

    val config = storage.config.stateIn(viewModelScope, SharingStarted.Eagerly, ProxyConfig())

    fun save(cfg: ProxyConfig) {
        viewModelScope.launch { storage.save(cfg) }
    }

    fun toggle(enabled: Boolean) {
        viewModelScope.launch { storage.setEnabled(enabled) }
    }

    // Parse tg://proxy?server=...&port=...&secret=... OR vkplus://proxy?host=...&port=...&type=...
    fun parseLink(raw: String): ProxyConfig? {
        return try {
            val trimmed = raw.trim()
            val uri = Uri.parse(trimmed)
            when {
                // tg://proxy?server=...&port=...&secret=...
                (uri.scheme == "tg" || uri.scheme == "https") && uri.host == "t.me" || uri.scheme == "tg" -> {
                    val server = uri.getQueryParameter("server") ?: return null
                    val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 443
                    val secret = uri.getQueryParameter("secret") ?: ""
                    // Decode MTProto secret: strip leading 'ee' (FakeTLS prefix) to get real secret
                    val cleanSecret = if (secret.startsWith("ee")) secret.substring(2) else secret
                    ProxyConfig(
                        enabled = true,
                        host = server,
                        port = port,
                        type = "SOCKS5",
                        secret = cleanSecret
                    )
                }
                // vkplus://proxy?host=...&port=...&type=...&user=...&pass=...
                uri.scheme == "vkplus" && uri.host == "proxy" -> {
                    val host = uri.getQueryParameter("host") ?: return null
                    val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 1080
                    val type = uri.getQueryParameter("type")?.uppercase() ?: "SOCKS5"
                    val user = uri.getQueryParameter("user") ?: ""
                    val pass = uri.getQueryParameter("pass") ?: ""
                    val secret = uri.getQueryParameter("secret") ?: ""
                    ProxyConfig(enabled = true, host = host, port = port, type = type, user = user, pass = pass, secret = secret)
                }
                // Plain host:port
                trimmed.contains(":") && !trimmed.contains("://") -> {
                    val parts = trimmed.split(":")
                    ProxyConfig(enabled = true, host = parts[0], port = parts.getOrNull(1)?.toIntOrNull() ?: 1080)
                }
                else -> null
            }
        } catch (e: Exception) { null }
    }

    // Build vkplus:// deep link from current config
    fun buildDeepLink(cfg: ProxyConfig): String {
        val sb = StringBuilder("vkplus://proxy?host=${cfg.host}&port=${cfg.port}&type=${cfg.type}")
        if (cfg.user.isNotBlank()) sb.append("&user=${cfg.user}&pass=${cfg.pass}")
        if (cfg.secret.isNotBlank()) sb.append("&secret=${cfg.secret}")
        return sb.toString()
    }
}

@Composable
fun ProxyScreen(viewModel: ProxyViewModel = hiltViewModel()) {
    val config by viewModel.config.collectAsState()
    val context = LocalContext.current

    var host by remember(config.host) { mutableStateOf(config.host) }
    var port by remember(config.port) { mutableStateOf(config.port.toString()) }
    var type by remember(config.type) { mutableStateOf(config.type) }
    var user by remember(config.user) { mutableStateOf(config.user) }
    var pass by remember(config.pass) { mutableStateOf(config.pass) }
    var secret by remember(config.secret) { mutableStateOf(config.secret) }
    var linkInput by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var showDeepLink by remember { mutableStateOf(false) }

    val isConfigured = host.isNotBlank() && port.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize().background(Background)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status card
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (config.enabled) CyberBlue.copy(0.12f) else SurfaceVariant)
                .border(1.dp, if (config.enabled) CyberBlue.copy(0.4f) else Divider, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (config.enabled) Icons.Filled.VpnKey else Icons.Filled.VpnKeyOff,
                null, tint = if (config.enabled) CyberBlue else OnSurfaceMuted,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (config.enabled) "Прокси активен" else "Прокси отключён",
                    color = if (config.enabled) CyberBlue else OnSurface,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
                if (config.enabled && config.host.isNotBlank()) {
                    Text("${config.host}:${config.port} · ${config.type}",
                        color = OnSurfaceMuted, fontSize = 12.sp)
                }
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = { if (isConfigured) viewModel.toggle(it) },
                enabled = isConfigured,
                colors = SwitchDefaults.colors(checkedThumbColor = Background, checkedTrackColor = CyberBlue)
            )
        }

        // Import link section
        SectionCard(title = "Импорт ссылки") {
            Text("Поддерживаются: tg://proxy?..., vkplus://proxy?..., host:port",
                color = OnSurfaceMuted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = linkInput,
                onValueChange = { linkInput = it; parseError = null },
                placeholder = { Text("tg://proxy?server=...&port=...&secret=...", color = OnSurfaceMuted, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                trailingIcon = {
                    Row {
                        // Paste from clipboard
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val txt = cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                            if (txt.isNotBlank()) { linkInput = txt; parseError = null }
                        }) { Icon(Icons.Filled.ContentPaste, null, tint = CyberBlue, modifier = Modifier.size(18.dp)) }
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
                Text(parseError!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val parsed = viewModel.parseLink(linkInput)
                    if (parsed != null) {
                        host = parsed.host; port = parsed.port.toString()
                        type = parsed.type; user = parsed.user; pass = parsed.pass; secret = parsed.secret
                        viewModel.save(parsed)
                        linkInput = ""
                        parseError = null
                    } else {
                        parseError = "Не удалось распознать ссылку"
                    }
                },
                enabled = linkInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
            ) {
                Icon(Icons.Filled.Download, null, tint = Background, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Импортировать", color = Background, fontWeight = FontWeight.SemiBold)
            }
        }

        // Manual config
        SectionCard(title = "Настройки вручную") {
            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SOCKS5", "HTTP").forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberBlue,
                            selectedLabelColor = Background,
                            containerColor = SurfaceVariant,
                            labelColor = OnSurfaceMuted
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProxyField("Хост / IP", host, Modifier.weight(1f)) { host = it }
                ProxyField("Порт", port, Modifier.width(90.dp), KeyboardType.Number) { port = it }
            }
            Spacer(Modifier.height(4.dp))
            ProxyField("Пользователь (необязательно)", user) { user = it }
            Spacer(Modifier.height(4.dp))
            // Password with show/hide
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Пароль (необязательно)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                    }
                },
                colors = proxyFieldColors()
            )
            // Secret (MTProto)
            if (secret.isNotBlank() || type == "SOCKS5") {
                Spacer(Modifier.height(4.dp))
                ProxyField("Secret / MTProto (необязательно)", secret) { secret = it }
                if (secret.isNotBlank()) {
                    Text("FakeTLS домен: ${decodeMTSecret(secret)}",
                        color = OnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    viewModel.save(ProxyConfig(
                        enabled = config.enabled,
                        host = host, port = port.toIntOrNull() ?: 1080,
                        type = type, user = user, pass = pass, secret = secret
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
            ) { Text("Сохранить", color = Background, fontWeight = FontWeight.SemiBold) }
        }

        // Deep link generator
        if (isConfigured) {
            SectionCard(title = "Deep Link") {
                val deepLink = viewModel.buildDeepLink(ProxyConfig(
                    enabled = config.enabled, host = host,
                    port = port.toIntOrNull() ?: 1080,
                    type = type, user = user, pass = pass, secret = secret
                ))
                Text(deepLink, color = CyberBlue, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariant)
                        .padding(10.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("proxy", deepLink))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CyberBlue)
                    )
                ) {
                    Icon(Icons.Filled.ContentCopy, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Скопировать ссылку", color = CyberBlue)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(0.5.dp, Divider, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(title, color = OnSurfaceMuted, fontSize = 11.sp, letterSpacing = 0.5.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

@Composable
private fun ProxyField(label: String, value: String, modifier: Modifier = Modifier,
                       keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true,
        colors = proxyFieldColors()
    )
}

@Composable
private fun proxyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
    focusedLabelColor = CyberBlue, unfocusedLabelColor = OnSurfaceMuted,
    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
)

// Decode FakeTLS domain from MTProto secret
// Format: ee + <domain hex> OR plain hex → try to read ASCII at end
private fun decodeMTSecret(secret: String): String {
    return try {
        val hex = if (secret.startsWith("ee")) secret.substring(2) else secret
        // Last part of hex may encode domain name
        val bytes = hex.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
        val str = String(bytes).filter { it.code in 32..126 }
        if (str.length > 4) str else "нет"
    } catch (e: Exception) { "нет" }
}
