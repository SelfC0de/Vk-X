package com.selfcode.vkplus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.ui.theme.*

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(color = CyberBlue)
            user == null -> Text("Не удалось загрузить профиль", color = ErrorRed)
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    AsyncImage(
                        model = user?.photo200,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = user?.fullName ?: "",
                        color = OnSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    user?.status?.takeIf { it.isNotBlank() }?.let { status ->
                        Spacer(Modifier.height(6.dp))
                        Text(text = status, color = OnSurfaceMuted, fontSize = 14.sp)
                    }
                    user?.city?.let { city ->
                        Spacer(Modifier.height(4.dp))
                        Text(text = city.title, color = OnSurfaceMuted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(32.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = ButtonDefaults.outlinedButtonBorder().copy(
                            brush = SolidColor(ErrorRed)
                        )
                    ) {
                        Text("Выйти")
                    }
                }
            }
        }
    }
}
