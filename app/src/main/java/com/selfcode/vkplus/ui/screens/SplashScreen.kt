package com.selfcode.vkplus.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfcode.vkplus.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    var iconVisible     by remember { mutableStateOf(false) }
    var titleVisible    by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var linksVisible    by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150);  iconVisible     = true
        delay(500);  titleVisible    = true
        delay(350);  subtitleVisible = true
        delay(400);  linksVisible    = true
        delay(1200); onFinished()
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0f,
        animationSpec = tween(1200, easing = EaseInOutSine), label = "glow"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0.2f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "iconScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0f,
        animationSpec = tween(600), label = "iconAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF080D18), Color(0xFF050810), Color(0xFF030508)),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background glow blob
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .alpha(glowAlpha * 0.2f)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(listOf(CyberBlue, Color.Transparent)),
                    RoundedCornerShape(50)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            // Icon — drawn with Compose, no resource needed
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(iconScale)
                    .alpha(iconAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .alpha(0.3f)
                        .blur(20.dp)
                        .background(
                            Brush.radialGradient(listOf(CyberBlue, Color.Transparent)),
                            RoundedCornerShape(24.dp)
                        )
                )
                // Icon box
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0D1520))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(CyberBlue, CyberAccent)),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VK",
                        color = CyberBlue,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    // Plus badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color(0xFF050810), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700, easing = EaseOutQuart)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome to", color = OnSurfaceMuted, fontSize = 14.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.height(4.dp))
                    Text("VK+", color = CyberBlue, fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
                }
            }

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(tween(600)) + expandVertically(tween(600))
            ) {
                Text("by SelfCode", color = CyberAccent, fontSize = 13.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(44.dp))

            AnimatedVisibility(
                visible = linksVisible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinkChip("vk.com/selfcode_dev")  { uriHandler.openUri("https://vk.com/selfcode_dev") }
                    LinkChip("t.me/selfcode_dev")    { uriHandler.openUri("https://t.me/selfcode_dev") }
                }
            }

            Spacer(Modifier.height(52.dp))
        }

        AnimatedVisibility(
            visible = linksVisible,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            enter = fadeIn(tween(1000))
        ) {
            Text("VK+ v1.0  ·  SelfCode", color = OnSurfaceMuted.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun LinkChip(text: String, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "chipBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "borderAlpha"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CyberBlue.copy(alpha = 0.08f))
            .border(0.5.dp, CyberBlue.copy(alpha = borderAlpha), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = CyberBlue.copy(alpha = borderAlpha), fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
    }
}
