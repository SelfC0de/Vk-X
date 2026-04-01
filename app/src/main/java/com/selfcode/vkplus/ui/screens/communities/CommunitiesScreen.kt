package com.selfcode.vkplus.ui.screens.communities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.selfcode.vkplus.data.model.VKCommunity
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunitiesViewModel @Inject constructor(private val repository: VKRepository) : ViewModel() {
    private val _communities = MutableStateFlow<List<VKCommunity>>(emptyList())
    val communities: StateFlow<List<VKCommunity>> = _communities
    private val _searchResults = MutableStateFlow<List<VKCommunity>>(emptyList())
    val searchResults: StateFlow<List<VKCommunity>> = _searchResults
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    var selectedTab by mutableIntStateOf(0)

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            when (val r = repository.getUserCommunities()) {
                is VKResult.Success -> _communities.value = r.data
                is VKResult.Error -> {}
            }
            _loading.value = false
        }
    }

    fun search(q: String) {
        _query.value = q
        if (q.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            when (val r = repository.searchGroups(q)) {
                is VKResult.Success -> _searchResults.value = r.data
                is VKResult.Error -> {}
            }
        }
    }

    fun join(groupId: Int) { viewModelScope.launch { repository.joinGroup(groupId); load() } }
    fun leave(groupId: Int) { viewModelScope.launch { repository.leaveGroup(groupId); load() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(viewModel: CommunitiesViewModel = hiltViewModel()) {
    val communities by viewModel.communities.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val query by viewModel.query.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        TabRow(selectedTabIndex = viewModel.selectedTab, containerColor = Surface, contentColor = CyberBlue) {
            Tab(selected = viewModel.selectedTab == 0, onClick = { viewModel.selectedTab = 0 }, text = { Text("Мои", fontSize = 13.sp) })
            Tab(selected = viewModel.selectedTab == 1, onClick = { viewModel.selectedTab = 1 }, text = { Text("Поиск", fontSize = 13.sp) })
        }

        if (viewModel.selectedTab == 1) {
            OutlinedTextField(
                value = query, onValueChange = viewModel::search,
                placeholder = { Text("Поиск сообществ", color = OnSurfaceMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceMuted) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue),
                shape = RoundedCornerShape(10.dp), singleLine = true
            )
        }

        val list = if (viewModel.selectedTab == 0) communities else searchResults

        if (loading && viewModel.selectedTab == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CyberBlue) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(list, key = { it.id }) { community ->
                    CommunityRow(community, viewModel)
                }
            }
        }
    }
}

@Composable
private fun CommunityRow(community: VKCommunity, viewModel: CommunitiesViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(model = community.photo100, contentDescription = null,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceVariant),
            contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(community.name, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (community.activity.isNotBlank()) Text(community.activity, color = OnSurfaceMuted, fontSize = 12.sp)
            if (community.membersCount > 0) Text("${formatCount(community.membersCount)} участников", color = OnSurfaceMuted, fontSize = 11.sp)
        }
        if (community.isMember == 1) {
            TextButton(onClick = { viewModel.leave(community.id) }) { Text("Выйти", color = ErrorRed, fontSize = 12.sp) }
        } else {
            TextButton(onClick = { viewModel.join(community.id) }) { Text("Вступить", color = CyberBlue, fontSize = 12.sp) }
        }
    }
    HorizontalDivider(color = Divider.copy(alpha = 0.4f), modifier = Modifier.padding(start = 74.dp))
}

private fun formatCount(n: Int) = when { n >= 1_000_000 -> "${n/1_000_000}M"; n >= 1_000 -> "${n/1_000}K"; else -> n.toString() }
