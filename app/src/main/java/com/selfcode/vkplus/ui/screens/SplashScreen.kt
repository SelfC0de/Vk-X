package com.selfcode.vkplus.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfcode.vkplus.R
import com.selfcode.vkplus.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    var iconVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var linksVisible by remember { mutableStateOf(false) }
    var exitVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200); iconVisible = true
        delay(400); titleVisible = true
        delay(300); subtitleVisible = true
        delay(300); linksVisible = true
        delay(400); exitVisible = true
    }

    val iconScale by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "iconAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF060810), Background, Color(0xFF0A0F1A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glow behind icon
        Box(
            modifier = Modifier
                .size(220.dp)
                .alpha(iconAlpha * 0.3f)
                .background(
                    Brush.radialGradient(listOf(CyberBlue.copy(alpha = 0.4f), Color.Transparent)),
                    RoundedCornerShape(50)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(iconScale)
                    .alpha(iconAlpha)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(28.dp))

            // Welcome text
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Welcome to",
                        color = OnSurfaceMuted,
                        fontSize = 15.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "VK+",
                        color = CyberBlue,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
            ) {
                Text(
                    text = "by SelfCode",
                    color = CyberAccent,
                    fontSize = 14.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(48.dp))

            // Links
            AnimatedVisibility(
                visible = linksVisible,
                enter = fadeIn(tween(600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinkChip(text = "vk.com/selfcode_dev", url = "https://vk.com/selfcode_dev", uriHandler = { uriHandler.openUri(it) })
                    LinkChip(text = "t.me/selfcode_dev", url = "https://t.me/selfcode_dev", uriHandler = { uriHandler.openUri(it) })
                }
            }

            Spacer(Modifier.height(56.dp))

            // Enter button
            AnimatedVisibility(
                visible = exitVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            Brush.horizontalGradient(listOf(CyberBlue, CyberAccent)),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onFinished() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Войти",
                        color = Color(0xFF060810),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Version tag bottom
        AnimatedVisibility(
            visible = linksVisible,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            enter = fadeIn(tween(800))
        ) {
            Text("VK+ v1.0 · SelfCode", color = OnSurfaceMuted.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun LinkChip(text: String, url: String, uriHandler: (String) -> Unit) {
    Box(
        modifier = Modifier
            .background(SurfaceVariant, RoundedCornerShape(20.dp))
            .clickable { uriHandler(url) }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = CyberBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
