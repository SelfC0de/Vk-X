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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
            .background(Brush.verticalGradient(listOf(Color(0xFF050810), Background, Color(0xFF060C14))))
            .systemBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "headerGlow")
            val titleAlpha by infiniteTransition.animateFloat(
                initialValue = 0.85f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "titleAlpha"
            )
            Text(
                "VK+",
                color = CyberBlue.copy(alpha = titleAlpha),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp
            )
            Spacer(Modifier.height(3.dp))
            Text("by SelfCode", color = CyberAccent.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 3.sp)
        }

        // Animated tab selector
        AnimatedTabSelector(
            tabs = listOf("🔑  Пароль", "🪙  Токен"),
            selectedIndex = tabIndex,
            onTabSelected = { tabIndex = it }
        )

        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = tabIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(300)) togetherWith
                    slideOutHorizontally(tween(300)) { -it / 2 } + fadeOut(tween(300))
                } else {
                    slideInHorizontally(tween(300)) { -it / 2 } + fadeIn(tween(300)) togetherWith
                    slideOutHorizontally(tween(300)) { it / 2 } + fadeOut(tween(300))
                }
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
private fun AnimatedTabSelector(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tabBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(SurfaceVariant, RoundedCornerShape(14.dp))
            .border(1.dp, CyberBlue.copy(alpha = borderAlpha), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { i, label ->
            val selected = selectedIndex == i
            val bgAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(250), label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) Color(0xFF050810) else OnSurfaceMuted,
                animationSpec = tween(250), label = "tabText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CyberBlue.copy(alpha = bgAlpha),
                                CyberAccent.copy(alpha = bgAlpha * 0.8f)
                            )
                        ),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onTabSelected(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 0.3.sp
                )
            }
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
                CircularProgressIndicator(color = CyberBlue, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun TokenTab(onManualToken: (String) -> Unit, showInvalidError: Boolean) {
    var token by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(if (showInvalidError) "Токен недействителен" else null) }
    var isLoading by remember { mutableStateOf(false) }

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500, easing = EaseOutQuart)) { it / 4 }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            // Glowing badge
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(
                            CyberBlue.copy(alpha = 0.12f),
                            CyberAccent.copy(alpha = 0.08f)
                        )),
                        RoundedCornerShape(20.dp)
                    )
                    .border(0.5.dp, CyberBlue.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Access Token Auth", color = CyberBlue, fontSize = 12.sp, letterSpacing = 1.sp)
            }

            Spacer(Modifier.height(16.dp))

            Text("Вставьте токен доступа", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Авторизация через OAuth — без ввода пароля",
                color = OnSurfaceMuted, fontSize = 12.sp, textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // Token field with animated border
            val fieldBorderAlpha by rememberInfiniteTransition(label = "field").animateFloat(
                initialValue = 0.3f, targetValue = 0.8f,
                animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "fieldBorder"
            )

            OutlinedTextField(
                value = token,
                onValueChange = { token = it; error = null },
                label = { Text("access_token", color = OnSurfaceMuted, fontSize = 12.sp) },
                placeholder = { Text("vk1.a.xxxxxx...", color = OnSurfaceMuted.copy(alpha = 0.4f), fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (error != null) ErrorRed
                        else if (token.isNotBlank()) CyberBlue.copy(alpha = fieldBorderAlpha)
                        else Divider,
                        RoundedCornerShape(14.dp)
                    ),
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(
                            if (tokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null, tint = OnSurfaceMuted
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                    cursorColor = CyberBlue,
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant,
                    errorContainerColor = SurfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                isError = error != null
            )

            AnimatedVisibility(visible = error != null, enter = fadeIn() + expandVertically()) {
                Text(error ?: "", color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Submit button
            val buttonEnabled = !isLoading && token.isNotBlank()
            val buttonScale by animateFloatAsState(
                targetValue = if (buttonEnabled) 1f else 0.97f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "btnScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(buttonScale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (buttonEnabled)
                            Brush.horizontalGradient(listOf(CyberBlue, CyberAccent))
                        else
                            Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))
                    )
                    .clickable {
                        if (buttonEnabled) { isLoading = true; onManualToken(token.trim()) }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Background, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        "Войти →",
                        color = if (buttonEnabled) Color(0xFF050810) else OnSurfaceMuted,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Hint card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(SurfaceVariant, Color(0xFF0F1520))),
                        RoundedCornerShape(14.dp)
                    )
                    .border(0.5.dp, Divider, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("Как получить токен:", color = CyberBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "oauth.vk.com/authorize\n  ?client_id=2685278\n  &scope=offline\n  &response_type=token",
                    color = OnSurfaceMuted.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
