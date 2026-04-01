package com.selfcode.vkplus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
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

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            when (val r = repository.getBannedList()) {
                is VKResult.Success -> _users.value = r.data
                is VKResult.Error -> {}
            }
            _loading.value = false
        }
    }

    fun unban(userId: Int) {
        viewModelScope.launch {
            repository.unbanUser(userId)
            _users.value = _users.value.filter { it.id != userId }
        }
    }
}

@Composable
fun BlacklistScreen(viewModel: BlacklistViewModel = hiltViewModel()) {
    val users by viewModel.users.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberBlue)
            }
        } else if (users.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Block, null, tint = OnSurfaceMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Чёрный список пуст", color = OnSurfaceMuted, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(users, key = { it.id }) { user ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = user.photo100, contentDescription = null,
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(SurfaceVariant),
                            contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                        Text(user.fullName, color = OnSurface, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.unban(user.id) }) {
                            Text("Разблокировать", color = CyberBlue, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Divider.copy(alpha = 0.4f), modifier = Modifier.padding(start = 70.dp))
                }
            }
        }
    }
}
