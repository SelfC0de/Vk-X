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
    onQrAuthenticated: () -> Unit = {},
    onManualToken: (String) -> Unit,
    onAccountSwitch: (() -> Unit)? = null,
    invalidToken: Boolean = false
) {
    var tabIndex by remember { mutableIntStateOf(if (invalidToken) 1 else 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Background, Color(0xFF060C14))))
            .systemBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val alpha by infiniteTransition.animateFloat(
                0.8f, 1f,
                infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "a"
            )
            Text("VK+", color = CyberBlue.copy(alpha = alpha), fontSize = 38.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
            Spacer(Modifier.height(4.dp))
            Text("by SelfCode", color = CyberAccent.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 3.sp)

            if (invalidToken) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp)
                        .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .border(0.5.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠ Токен недействителен или истёк", color = ErrorRed, fontSize = 13.sp)
                }
            }
        }

        // Tab selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("🔑  Пароль", "🪙  Токен").forEachIndexed { i, label ->
                val selected = tabIndex == i
                val bgAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "bg$i")
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBlue.copy(alpha = bgAlpha * 0.15f))
                        .clickable { tabIndex = i }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label,
                        color = if (selected) CyberBlue else OnSurfaceMuted,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Content
        AnimatedContent(
            targetState = tabIndex,
            transitionSpec = {
                if (targetState > initialState)
                    slideInHorizontally(tween(250)) { it / 2 } + fadeIn() togetherWith
                    slideOutHorizontally(tween(250)) { -it / 2 } + fadeOut()
                else
                    slideInHorizontally(tween(250)) { -it / 2 } + fadeIn() togetherWith
                    slideOutHorizontally(tween(250)) { it / 2 } + fadeOut()
            },
            label = "tab"
        ) { idx ->
            when (idx) {
                0 -> WebViewTab(onTokenReceived)
                else -> TokenTab(onManualToken)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewTab(onTokenReceived: (Uri) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
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
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TokenTab(onManualToken: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Access Token", color = OnSurfaceMuted) },
            placeholder = { Text("vk1.a.xxx...", color = OnSurfaceMuted.copy(0.5f), fontSize = 12.sp) },
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
            Text("Войти",
                color = if (token.isNotBlank()) Color(0xFF050810) else OnSurfaceMuted,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(SurfaceVariant, RoundedCornerShape(12.dp))
                .border(0.5.dp, Divider, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text("Как получить токен?", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Перейди на вкладку «Пароль» и войди",
                "Или используй Kate Mobile → Settings → Advanced → Token",
                "Токен начинается с vk1.a."
            ).forEachIndexed { i, step ->
                Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(18.dp).background(CyberBlue.copy(0.15f),
                        RoundedCornerShape(50)).padding(1.dp), contentAlignment = Alignment.Center) {
                        Text("${i+1}", color = CyberBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(step, color = OnSurfaceMuted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}
