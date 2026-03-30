package com.selfcode.vkplus.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.selfcode.vkplus.auth.QrAuthScreen
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
    var isLoading by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Surface,
            contentColor = CyberBlue
        ) {
            listOf("Пароль", "Токен").forEachIndexed { i, title ->
                Tab(
                    selected = tabIndex == i,
                    onClick = { tabIndex = i },
                    text = { Text(title, color = if (tabIndex == i) CyberBlue else OnSurfaceMuted) }
                )
            }
        }

        when (tabIndex) {
            0 -> Box(modifier = Modifier.fillMaxSize()) {
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyberBlue)
                            Spacer(Modifier.height(16.dp))
                            Text("VK+", color = CyberBlue, fontSize = 28.sp, letterSpacing = 4.sp)
                        }
                    }
                }
            }
            1 -> TokenLoginTab(onManualToken = onManualToken, showInvalidError = invalidToken)
        }
    }
}

@Composable
private fun TokenLoginTab(onManualToken: (String) -> Unit, showInvalidError: Boolean) {
    var token by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(if (showInvalidError) "Токен недействителен. Введите новый." else null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VK+", color = CyberBlue, fontSize = 32.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(8.dp))
        Text("Вход по токену", color = OnSurfaceMuted, fontSize = 14.sp)
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it; error = null },
            label = { Text("Access Token", color = OnSurfaceMuted) },
            placeholder = { Text("vk1.a.xxxx...", color = OnSurfaceMuted) },
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
                focusedLabelColor = CyberBlue, cursorColor = CyberBlue
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            isError = error != null
        )

        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error!!, color = ErrorRed, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    token.isBlank() -> error = "Введите токен"
                    else -> { isLoading = true; onManualToken(token.trim()) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Background, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Войти", color = Background, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Получить токен:\noauth.vk.com/authorize?client_id=2685278\n&scope=offline&response_type=token",
            color = OnSurfaceMuted, fontSize = 11.sp, lineHeight = 16.sp
        )
    }
}
