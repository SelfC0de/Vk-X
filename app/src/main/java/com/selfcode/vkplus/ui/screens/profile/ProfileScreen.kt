package com.selfcode.vkplus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.ui.screens.messages.ChatScreen
import com.selfcode.vkplus.ui.screens.messages.MessagesViewModel
import com.selfcode.vkplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    messagesViewModel: MessagesViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showArchive by remember { mutableStateOf(false) }

    if (showArchive && user != null) {
        ChatScreen(
            peerName = "Избранное",
            peerPhoto = user?.photo100,
            peerId = user!!.id,
            onBack = { showArchive = false },
            viewModel = messagesViewModel
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberBlue)
            }
            user == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Не удалось загрузить профиль", color = ErrorRed)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)) {
                        Text("Повторить", color = Color(0xFF050810))
                    }
                }
            }
            else -> {
                val u = user!!
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                    // Cover
                    Box(modifier = Modifier.fillMaxWidth().height(170.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFF0A1020), Color(0xFF0D1528), Background)))) {
                        Box(modifier = Modifier.size(90.dp).align(Alignment.BottomStart).offset(x = 20.dp, y = 45.dp)) {
                            AsyncImage(
                                model = u.photo200,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    .border(3.dp, Brush.linearGradient(listOf(CyberBlue, CyberAccent)), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            // Online dot
                            Box(modifier = Modifier.size(18.dp).align(Alignment.BottomEnd)
                                .background(Background, CircleShape).padding(3.dp)
                                .background(CyberAccent, CircleShape))
                        }
                    }

                    Spacer(Modifier.height(52.dp))

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(u.fullName, color = OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (!u.status.isNullOrBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(u.status, color = OnSurfaceMuted, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("vk.com/id${u.id}", color = CyberBlue, fontSize = 12.sp)

                        Spacer(Modifier.height(16.dp))

                        // Buttons row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showArchive = true },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(CyberBlue)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberBlue)
                            ) {
                                Icon(Icons.Filled.Star, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Избранное", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Divider)
                        Spacer(Modifier.height(16.dp))

                        Text("Информация", color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))

                        ProfileInfoRow(Icons.Filled.Info, "ID", "id${u.id}")
                        ProfileInfoRow(Icons.Filled.Person, "Страница", "vk.com/id${u.id}")
                        u.city?.let { ProfileInfoRow(Icons.Filled.Notifications, "Город", it.title) }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Divider)
                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(ErrorRed)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                        ) {
                            Icon(Icons.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Выйти из аккаунта")
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
