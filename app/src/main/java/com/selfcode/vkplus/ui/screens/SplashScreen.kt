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
import androidx.compose.ui.draw.blur
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

    var iconVisible    by remember { mutableStateOf(false) }
    var titleVisible   by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var linksVisible   by remember { mutableStateOf(false) }
    var buttonVisible  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150);  iconVisible     = true
        delay(500);  titleVisible    = true
        delay(350);  subtitleVisible = true
        delay(400);  linksVisible    = true
        delay(500);  buttonVisible   = true
        // auto-proceed after ~3.2s total
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

    // Pulsing glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(
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
                .size(300.dp)
                .scale(pulseScale)
                .alpha(glowAlpha * 0.25f)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(listOf(CyberBlue, CyberAccent.copy(alpha = 0.3f), Color.Transparent)),
                    RoundedCornerShape(50)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            // Icon with secondary glow ring
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale * iconScale)
                        .alpha(glowAlpha * 0.35f)
                        .blur(28.dp)
                        .background(
                            Brush.radialGradient(listOf(CyberBlue, Color.Transparent)),
                            RoundedCornerShape(50)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(iconScale)
                        .alpha(iconAlpha)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // "Welcome to"
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700, easing = EaseOutQuart)) { it / 2 }
            ) {
                Text(
                    text = "Welcome to",
                    color = OnSurfaceMuted,
                    fontSize = 14.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(Modifier.height(4.dp))

            // "VK+" with shimmer feel
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800, easing = EaseOutQuart)) { it / 2 }
            ) {
                Text(
                    text = "VK+",
                    color = CyberBlue,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                )
            }

            // "by SelfCode"
            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(tween(600)) + expandVertically(tween(600))
            ) {
                Text(
                    text = "by SelfCode",
                    color = CyberAccent,
                    fontSize = 13.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(44.dp))

            // Links
            AnimatedVisibility(
                visible = linksVisible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 3 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinkChip("vk.com/selfcode_dev")  { uriHandler.openUri("https://vk.com/selfcode_dev") }
                    LinkChip("t.me/selfcode_dev")    { uriHandler.openUri("https://t.me/selfcode_dev") }
                }
            }

            Spacer(Modifier.height(52.dp))

            // Enter button
            AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(tween(500)) + scaleIn(tween(500, easing = EaseOutBack))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(
                            Brush.horizontalGradient(listOf(CyberBlue, CyberAccent, CyberBlue)),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onFinished() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Войти",
                        color = Color(0xFF050810),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Bottom version tag
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
            .background(
                Brush.horizontalGradient(listOf(
                    CyberBlue.copy(alpha = 0.08f),
                    CyberAccent.copy(alpha = 0.05f)
                )),
                RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = CyberBlue.copy(alpha = borderAlpha),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}
