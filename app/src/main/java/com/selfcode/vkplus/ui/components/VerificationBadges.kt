package com.selfcode.vkplus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.selfcode.vkplus.data.model.VKVerificationInfo
import com.selfcode.vkplus.ui.theme.OnSurfaceMuted

private val FAVICON_URLS = mapOf(
    "gosuslugi" to "https://www.gosuslugi.ru/favicon.ico",
    "alfa"      to "https://alfabank.ru/favicon.ico",
    "tinkoff"   to "https://cdn.tbank.ru/params/common_front/resourses/icons/favicon-32x32.png",
    "sber"      to "https://www.sberbank.ru/common_static/favicon.ico",
    "vtb"       to "https://www.vtb.ru/favicon.ico"
)

// Синяя галочка VK (verified == 1, официальный аккаунт)
@Composable
fun BlueCheckBadge() {
    Icon(
        Icons.Filled.Verified,
        contentDescription = "Верифицирован ВКонтакте",
        tint = Color(0xFF1DA1F2),
        modifier = Modifier.size(18.dp)
    )
}

// Серая галочка (verification_info — сервисная верификация через банки/госуслуги)
@Composable
private fun GrayCheckBadge() {
    Icon(
        Icons.Filled.Verified,
        contentDescription = "Подтверждён через сервис",
        tint = Color(0xFF9E9E9E),
        modifier = Modifier.size(18.dp)
    )
}

// Строка "Верификация: <значки>" или "Верификация: Отсутствует"
@Composable
fun VerificationRow(verified: Int, info: VKVerificationInfo?) {
    val serviceVerifs = info?.verifications?.filter { FAVICON_URLS.containsKey(it.type) }.orEmpty()
    val hasVkBlue     = verified == 1
    val hasServiceGray = serviceVerifs.isNotEmpty()
    val hasAny         = hasVkBlue || hasServiceGray

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Верификация: ", color = OnSurfaceMuted, fontSize = 13.sp)
        if (!hasAny) {
            Text("Отсутствует", color = OnSurfaceMuted, fontSize = 13.sp)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasVkBlue) BlueCheckBadge()
                // Серая галочка + иконки сервисов
                if (hasServiceGray) {
                    GrayCheckBadge()
                    serviceVerifs.sortedBy { it.priority }.forEach { v ->
                        val url = FAVICON_URLS[v.type] ?: return@forEach
                        SafeFaviconImage(url = url, name = v.name)
                    }
                }
            }
        }
    }
}

// Только значки рядом с именем (без текста)
@Composable
fun VerificationBadgesInline(verified: Int, info: VKVerificationInfo?) {
    val serviceVerifs = info?.verifications?.filter { FAVICON_URLS.containsKey(it.type) }.orEmpty()
    val hasVkBlue      = verified == 1
    val hasServiceGray = serviceVerifs.isNotEmpty()
    if (!hasVkBlue && !hasServiceGray) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasVkBlue) BlueCheckBadge()
        if (hasServiceGray) {
            GrayCheckBadge()
            serviceVerifs.sortedBy { it.priority }.forEach { v ->
                val url = FAVICON_URLS[v.type] ?: return@forEach
                SafeFaviconImage(url = url, name = v.name)
            }
        }
    }
}

@Composable
private fun SafeFaviconImage(url: String, name: String) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = name,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> Spacer(Modifier.size(16.dp))
            is AsyncImagePainter.State.Error   -> Icon(
                Icons.Filled.Shield, contentDescription = name,
                tint = OnSurfaceMuted, modifier = Modifier.size(14.dp)
            )
            else -> SubcomposeAsyncImageContent()
        }
    }
}
