package com.selfcode.vkplus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlacklistViewModel @Inject constructor(private val repository: VKRepository) : ViewModel() {
    private val _users = MutableStateFlow<List<VKUser>>(emptyList())
    val users: StateFlow<List<VKUser>> = _users
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val r = repository.getBannedList()) {
                is VKResult.Success -> _users.value = r.data
                is VKResult.Error -> _error.value = r.message
            }
            _loading.value = false
        }
    }

    fun unban(userId: Int) {
        viewModelScope.launch {
            when (repository.unbanUser(userId)) {
                is VKResult.Success -> _users.value = _users.value.filter { it.id != userId }
                is VKResult.Error -> {}
            }
        }
    }
}

@Composable
fun BlacklistScreen(viewModel: BlacklistViewModel = hiltViewModel()) {
    val users by viewModel.users.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var confirmUnban by remember { mutableStateOf<VKUser?>(null) }

    // Confirm dialog
    confirmUnban?.let { user ->
        AlertDialog(
            onDismissRequest = { confirmUnban = null },
            containerColor = Surface,
            title = { Text("Разблокировать?", color = OnSurface) },
            text = { Text("${user.fullName} будет удалён из чёрного списка.", color = OnSurfaceMuted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.unban(user.id); confirmUnban = null }) {
                    Text("Разблокировать", color = CyberBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnban = null }) { Text("Отмена", color = OnSurfaceMuted) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = CyberBlue)
            }
            error != null -> Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text(error ?: "", color = ErrorRed, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)) {
                    Text("Повторить")
                }
            }
            users.isEmpty() -> Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Block, null, tint = OnSurfaceMuted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Чёрный список пуст", color = OnSurfaceMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("Заблокированные пользователи появятся здесь", color = OnSurfaceMuted, fontSize = 12.sp)
            }
            else -> {
                // Count header
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Заблокировано: ${users.size}", color = OnSurfaceMuted, fontSize = 12.sp, letterSpacing = 0.3.sp)
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users, key = { it.id }) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar with block overlay
                            Box(modifier = Modifier.size(50.dp)) {
                                AsyncImage(
                                    model = user.photo100,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        .background(SurfaceVariant)
                                        .border(2.dp, ErrorRed.copy(alpha = 0.5f), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier.size(18.dp).align(Alignment.BottomEnd)
                                        .background(Background, CircleShape).padding(2.dp)
                                        .background(ErrorRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Block, null, tint = Background, modifier = Modifier.size(11.dp))
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.fullName, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("id${user.id}", color = OnSurfaceMuted, fontSize = 11.sp)
                            }

                            // Unblock button
                            OutlinedButton(
                                onClick = { confirmUnban = user },
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(CyberBlue.copy(alpha = 0.5f))
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Filled.LockOpen, null, tint = CyberBlue, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Разблок.", color = CyberBlue, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(color = Divider.copy(alpha = 0.3f), modifier = Modifier.padding(start = 74.dp))
                    }
                }
            }
        }
    }
}
