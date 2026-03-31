package com.selfcode.vkplus.ui.screens.about

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfcode.vkplus.ui.theme.*

@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val glowAlpha by infiniteTransition.animateFloat(0.15f, 0.3f,
        infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse), label = "glow")
    val pulseScale by infiniteTransition.animateFloat(0.97f, 1.03f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse")

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF060A14), Background)))) {
        Box(modifier = Modifier.size(320.dp).align(Alignment.TopCenter).offset(y = 40.dp)
            .scale(pulseScale).alpha(glowAlpha).blur(100.dp)
            .background(Brush.radialGradient(listOf(CyberBlue, Color.Transparent)), CircleShape))

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Avatar
            AnimatedVisibility(visible, enter = fadeIn(tween(600)) + scaleIn(tween(600, easing = EaseOutBack))) {
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(110.dp).scale(pulseScale).alpha(0.25f).blur(24.dp)
                        .background(Brush.radialGradient(listOf(CyberBlue, Color.Transparent)), CircleShape))
                    Box(
                        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0D1520))
                            .border(1.5.dp, Brush.linearGradient(listOf(CyberBlue, CyberAccent)), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SC", color = CyberBlue, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(visible, enter = fadeIn(tween(700, 200)) + slideInVertically(tween(700, 200)) { it / 2 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SelfCode", color = OnSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("selfcode_dev", color = OnSurfaceMuted, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(visible, enter = fadeIn(tween(600, 400)) + slideInVertically(tween(600, 400)) { it / 3 }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Ссылки и контакты")
                    LinkCard(Icons.Filled.Person, "ВКонтакте", "vk.com/selfcode_dev", Color(0xFF4C75A3)) {
                        uriHandler.openUri("https://vk.com/selfcode_dev")
                    }
                    LinkCard(Icons.Filled.Send, "Telegram", "t.me/selfcode_dev", Color(0xFF2AABEE)) {
                        uriHandler.openUri("https://t.me/selfcode_dev")
                    }
                    LinkCard(Icons.Filled.Star, "GitHub", "github.com/SelfC0de", Color(0xFFE8E8E8)) {
                        uriHandler.openUri("https://github.com/SelfC0de")
                    }
                    LinkCard(Icons.Filled.Edit, "GitHub Gist", "gist.github.com/SelfC0de", Color(0xFF8B949E)) {
                        uriHandler.openUri("https://gist.github.com/SelfC0de")
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            AnimatedVisibility(visible, enter = fadeIn(tween(500, 800))) {
                Text("Made with ❤️ by SelfCode · 2026",
                    color = OnSurfaceMuted.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 0.5.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) = Text(
    text.uppercase(), color = OnSurfaceMuted, fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
    modifier = Modifier.fillMaxWidth()
)

@Composable
private fun LinkCard(icon: ImageVector, label: String, value: String, color: Color, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "card")
    val borderAlpha by infiniteTransition.animateFloat(0.2f, 0.5f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "b")
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.08f), SurfaceVariant)))
            .border(0.5.dp, color.copy(alpha = borderAlpha), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = OnSurfaceMuted, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ExitToApp, null, tint = OnSurfaceMuted.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}
