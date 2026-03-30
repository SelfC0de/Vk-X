package com.selfcode.vkplus.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.selfcode.vkplus.auth.VKConfig
import com.selfcode.vkplus.ui.theme.Background
import com.selfcode.vkplus.ui.theme.CyberBlue

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthScreen(onTokenReceived: (Uri) -> Unit) {
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val url = request.url.toString()
                            return when {
                                url.startsWith("https://oauth.vk.com/blank.html") -> {
                                    onTokenReceived(request.url)
                                    true
                                }
                                url.startsWith("vkontakte://") ||
                                url.startsWith("vk://") ||
                                !url.startsWith("http") -> true
                                else -> false
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            isLoading = false
                        }
                    }
                    loadUrl(VKConfig.authUrl())
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyberBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "VK+",
                        color = CyberBlue,
                        fontSize = 28.sp,
                        letterSpacing = 4.sp
                    )
                }
            }
        }
    }
}
