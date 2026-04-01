package com.selfcode.vkplus.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.selfcode.vkplus.auth.VKConfig
import com.selfcode.vkplus.data.local.SavedAccount
import com.selfcode.vkplus.data.local.TokenStorage
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthScreenViewModel @Inject constructor(
    private val tokenStorage: TokenStorage
) : ViewModel() {
    val savedAccounts: StateFlow<List<SavedAccount>> = tokenStorage.savedAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId: StateFlow<Int?> = tokenStorage.userId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun switchAccount(account: SavedAccount, onDone: () -> Unit) {
        viewModelScope.launch {
            tokenStorage.switchAccount(account)
            onDone()
        }
    }

    fun removeAccount(userId: Int) {
        viewModelScope.launch { tokenStorage.removeAccount(userId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthScreen(
    onTokenReceived: (Uri) -> Unit,
    onQrAuthenticated: () -> Unit,
    onManualToken: (token: String) -> Unit,
    onAccountSwitch: (() -> Unit)? = null,
    invalidToken: Boolean = false,
    viewModel: AuthScreenViewModel = hiltViewModel()
) {
    val savedAccounts by viewModel.savedAccounts.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    var tabIndex by remember { mutableIntStateOf(if (invalidToken) 1 else 0) }
    var deleteConfirm by remember { mutableStateOf<SavedAccount?>(null) }

    // Delete confirm dialog
    deleteConfirm?.let { acc ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            containerColor = Surface,
            title = { Text("Удалить аккаунт?", color = OnSurface) },
            text = {
                Text(
                    "${acc.name.ifBlank { "id${acc.userId}" }} будет удалён из списка.",
                    color = OnSurfaceMuted, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.removeAccount(acc.userId); deleteConfirm = null }) {
                    Text("Удалить", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) { Text("Отмена", color = CyberBlue) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Background, Color(0xFF060C14))))
            .systemBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "headerGlow")
            val titleAlpha by infiniteTransition.animateFloat(
                initialValue = 0.85f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "titleAlpha"
            )
            Text("VK+", color = CyberBlue.copy(alpha = titleAlpha), fontSize = 34.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
            Spacer(Modifier.height(3.dp))
            Text("by SelfCode", color = CyberAccent.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 3.sp)
        }

        // ── Saved accounts section ──────────────────────────────────────────
        val otherAccounts = savedAccounts.filter { it.userId != currentUserId }
        if (savedAccounts.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    if (otherAccounts.isNotEmpty()) "Сохранённые аккаунты" else "Аккаунты",
                    color = OnSurfaceMuted, fontSize = 12.sp, letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                savedAccounts.forEach { account ->
                    val isCurrent = account.userId == currentUserId
                    SavedAccountRow(
                        account = account,
                        isCurrent = isCurrent,
                        onSwitch = {
                            if (!isCurrent && onAccountSwitch != null) {
                                viewModel.switchAccount(account) { onAccountSwitch() }
                            }
                        },
                        onDelete = { deleteConfirm = account }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Добавить другой аккаунт",
                    color = CyberBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }
        }

        // ── Auth tabs ──────────────────────────────────────────────────────
        AnimatedTabSelector(
            tabs = listOf("🔑  Пароль", "🪙  Токен"),
            selectedIndex = tabIndex,
            onTabSelected = { tabIndex = it }
        )

        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = tabIndex,
            transitionSpec = {
                if (targetState > initialState)
                    slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(300)) togetherWith
                    slideOutHorizontally(tween(300)) { -it / 2 } + fadeOut(tween(300))
                else
                    slideInHorizontally(tween(300)) { -it / 2 } + fadeIn(tween(300)) togetherWith
                    slideOutHorizontally(tween(300)) { it / 2 } + fadeOut(tween(300))
            },
            label = "authTab"
        ) { index ->
            when (index) {
                0 -> WebViewTab(onTokenReceived)
                1 -> TokenTab(onManualToken, invalidToken)
                else -> WebViewTab(onTokenReceived)
            }
        }
    }
}

@Composable
private fun SavedAccountRow(
    account: SavedAccount,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) CyberBlue.copy(alpha = 0.08f) else SurfaceVariant)
            .border(
                if (isCurrent) 1.dp else 0.5.dp,
                if (isCurrent) CyberBlue.copy(alpha = 0.5f) else Divider,
                RoundedCornerShape(12.dp)
            )
            .then(if (!isCurrent) Modifier.clickable(onClick = onSwitch) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(contentAlignment = Alignment.BottomEnd) {
            if (account.photo.isNotBlank()) {
                AsyncImage(
                    model = account.photo, contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .border(1.5.dp, if (isCurrent) CyberBlue else Divider, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(SurfaceVariant)
                        .border(1.5.dp, if (isCurrent) CyberBlue else Divider, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = account.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(initial, color = CyberBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (isCurrent) {
                Box(
                    modifier = Modifier.size(12.dp).offset(x = 1.dp, y = 1.dp)
                        .background(Background, CircleShape).padding(2.dp)
                        .background(CyberAccent, CircleShape)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                account.name.ifBlank { "id${account.userId}" },
                color = if (isCurrent) CyberBlue else OnSurface,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                if (isCurrent) "активный аккаунт" else "id${account.userId}",
                color = OnSurfaceMuted, fontSize = 11.sp
            )
        }

        if (!isCurrent) {
            Text("Войти", color = CyberBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp))
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Delete, null, tint = ErrorRed.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AnimatedTabSelector(tabs: List<String>, selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "tabBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "borderAlpha"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .border(0.5.dp, CyberBlue.copy(alpha = borderAlpha), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            val bgAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "tabBg$index")
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(CyberBlue.copy(alpha = bgAlpha * 0.15f))
                    .then(if (!selected) Modifier.border(0.dp, Color.Transparent, RoundedCornerShape(10.dp)) else Modifier)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (selected) CyberBlue else OnSurfaceMuted,
                    fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewTab(onTokenReceived: (Uri) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled  = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        if (uri.scheme == "vk" || uri.toString().contains("blank.html")) {
                            onTokenReceived(uri)
                            return true
                        }
                        return false
                    }
                }
                loadUrl(VKConfig.authUrl())
            }
        }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun TokenTab(onManualToken: (String) -> Unit, invalidToken: Boolean) {
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (invalidToken) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .border(0.5.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Warning, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Токен недействителен или истёк", color = ErrorRed, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Access Token", color = OnSurfaceMuted) },
            placeholder = { Text("vk1.a.xxx...", color = OnSurfaceMuted.copy(alpha = 0.5f), fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        null, tint = OnSurfaceMuted)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (token.isNotBlank())
                        Brush.horizontalGradient(listOf(CyberBlue, CyberAccent))
                    else
                        Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))
                )
                .then(if (token.isNotBlank()) Modifier.clickable { onManualToken(token.trim()) } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text("Войти", color = if (token.isNotBlank()) Color(0xFF050810) else OnSurfaceMuted,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        // How to get token
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(SurfaceVariant, RoundedCornerShape(12.dp))
                .border(0.5.dp, Divider, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text("Как получить токен?", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val steps = listOf(
                "Перейди на вкладку «Пароль» и войди",
                "Или используй Kate Mobile — Settings → Advanced → Token",
                "Токен начинается с vk1.a."
            )
            steps.forEachIndexed { i, step ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Box(modifier = Modifier.size(18.dp).background(CyberBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("${i+1}", color = CyberBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(step, color = OnSurfaceMuted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}
