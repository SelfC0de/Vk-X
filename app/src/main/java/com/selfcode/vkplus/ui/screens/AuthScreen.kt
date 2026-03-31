package com.selfcode.vkplus.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.selfcode.vkplus.auth.VKConfig
import com.selfcode.vkplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthScreen(
    onTokenReceived: (Uri) -> Unit,
    onQrAuthenticated: () -> Unit,
    onManualToken: (token: String) -> Unit,
    invalidToken: Boolean = false
) {
    var tabIndex by remember { mutableIntStateOf(if (invalidToken) 1 else 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF060810), Background))
                )
                .padding(top = 32.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VK+", color = CyberBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Spacer(Modifier.height(4.dp))
            Text("by SelfCode", color = CyberAccent, fontSize = 12.sp, letterSpacing = 2.sp)
        }

        // Tabs
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Surface,
            contentColor = CyberBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("🔑 Пароль", "🪙 Токен").forEachIndexed { i, title ->
                Tab(
                    selected = tabIndex == i,
                    onClick = { tabIndex = i },
                    modifier = Modifier.height(48.dp),
                    text = {
                        Text(
                            title,
                            color = if (tabIndex == i) CyberBlue else OnSurfaceMuted,
                            fontSize = 13.sp,
                            fontWeight = if (tabIndex == i) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (tabIndex) {
            0 -> WebViewTab(onTokenReceived)
            1 -> TokenTab(onManualToken, invalidToken)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewTab(onTokenReceived: (Uri) -> Unit) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url.toString()
                            return when {
                                url.startsWith("https://oauth.vk.com/blank.html") -> { onTokenReceived(request.url); true }
                                url.startsWith("vkontakte://") || url.startsWith("vk://") || !url.startsWith("http") -> true
                                else -> false
                            }
                        }
                        override fun onPageFinished(view: WebView, url: String) { isLoading = false }
                    }
                    loadUrl(VKConfig.authUrl())
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberBlue)
            }
        }
    }
}

@Composable
private fun TokenTab(onManualToken: (String) -> Unit, showInvalidError: Boolean) {
    var token by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(if (showInvalidError) "Токен недействителен. Введите новый." else null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))

        Text(
            text = "Вставьте ваш access token",
            color = OnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Токен можно получить через браузер, авторизовавшись через OAuth",
            color = OnSurfaceMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it; error = null },
            label = { Text("Access Token", color = OnSurfaceMuted) },
            placeholder = { Text("vk1.a.xxxx...", color = OnSurfaceMuted.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { tokenVisible = !tokenVisible }) {
                    Icon(
                        if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = OnSurfaceMuted
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                focusedLabelColor = CyberBlue, cursorColor = CyberBlue,
                errorBorderColor = ErrorRed
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = error != null
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = ErrorRed, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (!isLoading && token.isNotBlank())
                        Brush.horizontalGradient(listOf(CyberBlue, CyberAccent))
                    else
                        Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))
                ),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    when {
                        token.isBlank() -> error = "Введите токен"
                        else -> { isLoading = true; onManualToken(token.trim()) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Background, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        "Войти",
                        color = if (token.isNotBlank()) Color(0xFF060810) else OnSurfaceMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // OAuth hint card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariant, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text("Как получить токен:", color = OnSurfaceMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Откройте в браузере:\noauth.vk.com/authorize\n?client_id=2685278\n&scope=offline&response_type=token",
                color = OnSurfaceMuted.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 17.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}
