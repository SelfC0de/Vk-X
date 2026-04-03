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

// Синяя галочка VK (verified == 1)
@Composable
fun BlueCheckBadge() {
    Icon(
        Icons.Filled.Verified,
        contentDescription = "Верифицирован ВКонтакте",
        tint = Color(0xFF1DA1F2),
        modifier = Modifier.size(18.dp)
    )
}

// Строка "Верификация: <иконки>" или "Верификация: Отсутствует"
@Composable
fun VerificationRow(verified: Int, info: VKVerificationInfo?) {
    val hasVerif = verified == 1 || (info != null && info.verifications.isNotEmpty())

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Верификация: ", color = OnSurfaceMuted, fontSize = 13.sp)
        if (!hasVerif) {
            Text("Отсутствует", color = OnSurfaceMuted, fontSize = 13.sp)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (verified == 1) {
                    BlueCheckBadge()
                }
                info?.verifications?.sortedBy { it.priority }?.forEach { v ->
                    val url = FAVICON_URLS[v.type] ?: return@forEach
                    SafeFaviconImage(url = url, name = v.name)
                }
            }
        }
    }
}

// Только иконки рядом с именем (без текста)
@Composable
fun VerificationBadgesInline(verified: Int, info: VKVerificationInfo?) {
    val hasVerif = verified == 1 || (info != null && info.verifications.isNotEmpty())
    if (!hasVerif) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (verified == 1) BlueCheckBadge()
        info?.verifications?.sortedBy { it.priority }?.forEach { v ->
            val url = FAVICON_URLS[v.type] ?: return@forEach
            SafeFaviconImage(url = url, name = v.name)
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
            is AsyncImagePainter.State.Loading -> {
                // Пустое место пока грузится — не краш
                Spacer(Modifier.size(16.dp))
            }
            is AsyncImagePainter.State.Error -> {
                // Если не загрузилась — серый щит вместо краша
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = name,
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}
