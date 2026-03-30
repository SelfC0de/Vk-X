package com.selfcode.vkplus.auth

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selfcode.vkplus.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrAuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: QrAuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is QrAuthState.Authenticated) onAuthenticated()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "VK+",
                color = CyberBlue,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Вход по QR-коду",
                color = OnSurfaceMuted,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(32.dp))

            when (val s = state) {
                is QrAuthState.Loading -> CircularProgressIndicator(color = CyberBlue)
                is QrAuthState.QrReady -> {
                    val qrBitmap = remember(s.url) { generateQr(s.url) }
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Откройте приложение ВКонтакте\nи отсканируйте QR-код",
                        color = OnSurfaceMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        color = CyberBlue,
                        trackColor = SurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is QrAuthState.Error -> {
                    Text(s.message, color = ErrorRed, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.start() }) { Text("Повторить") }
                }
                else -> {}
            }
        }
    }
}

private fun generateQr(url: String): Bitmap? = runCatching {
    val writer = QRCodeWriter()
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = writer.encode(url, BarcodeFormat.QR_CODE, 512, 512, hints)
    val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
    for (x in 0 until 512) {
        for (y in 0 until 512) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    bmp
}.getOrNull()
