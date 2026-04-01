package com.selfcode.vkplus.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.local.SavedAccount
import com.selfcode.vkplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherScreen(
    onSwitched: () -> Unit,
    onAddAccount: () -> Unit,
    viewModel: AccountSwitcherViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    var deleteConfirm by remember { mutableStateOf<SavedAccount?>(null) }

    // Delete confirm dialog
    deleteConfirm?.let { acc ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            containerColor = Surface,
            title = { Text("Удалить аккаунт?", color = OnSurface) },
            text = { Text("${acc.name.ifBlank { "id${acc.userId}" }} будет удалён из списка. Для повторного входа потребуется токен.", color = OnSurfaceMuted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.removeAccount(acc.userId); deleteConfirm = null }) {
                    Text("Удалить", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) { Text("Отмена", color = CyberBlue) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1020), Background)))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text("VK+", color = CyberBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("Смена аккаунта", color = OnSurfaceMuted, fontSize = 13.sp)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Spacer(Modifier.height(4.dp)) }

            items(accounts, key = { it.userId }) { account ->
                val isCurrent = account.userId == currentUserId
                AccountRow(
                    account = account,
                    isCurrent = isCurrent,
                    onClick = {
                        if (!isCurrent) {
                            viewModel.switchAccount(account) { onSwitched() }
                        }
                    },
                    onDelete = { deleteConfirm = account }
                )
            }

            if (accounts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text("Нет сохранённых аккаунтов", color = OnSurfaceMuted, fontSize = 14.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // Add account button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceVariant)
                        .border(0.5.dp, CyberBlue.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onAddAccount() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).background(CyberBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, null, tint = CyberBlue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Добавить аккаунт", color = CyberBlue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Войти через пароль или токен", color = OnSurfaceMuted, fontSize = 12.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AccountRow(
    account: SavedAccount,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val borderAlpha by infiniteTransition.animateFloat(
        if (isCurrent) 0.6f else 0.2f,
        if (isCurrent) 1.0f else 0.4f,
        infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "border"
    )

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) CyberBlue.copy(alpha = 0.08f) else SurfaceVariant)
            .border(
                if (isCurrent) 1.dp else 0.5.dp,
                if (isCurrent) CyberBlue.copy(alpha = borderAlpha) else Divider,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(contentAlignment = Alignment.BottomEnd) {
            if (account.photo.isNotBlank()) {
                AsyncImage(
                    model = account.photo, contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .border(2.dp, if (isCurrent) CyberBlue else Divider, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(SurfaceVariant)
                        .border(2.dp, if (isCurrent) CyberBlue else Divider, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        account.name.firstOrNull()?.toString() ?: "?",
                        color = CyberBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            if (isCurrent) {
                Box(
                    modifier = Modifier.size(14.dp).offset(x = 2.dp, y = 2.dp)
                        .background(Background, CircleShape).padding(2.dp)
                        .background(CyberAccent, CircleShape)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    account.name.ifBlank { "id${account.userId}" },
                    color = if (isCurrent) CyberBlue else OnSurface,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(CyberBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("активный", color = CyberBlue, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Text("id${account.userId}", color = OnSurfaceMuted, fontSize = 12.sp)
        }

        // Actions
        if (!isCurrent) {
            IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = OnSurfaceMuted.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Delete, null, tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}
