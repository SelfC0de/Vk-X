package com.selfcode.vkplus.ui.screens.communities

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKCommunity
import com.selfcode.vkplus.data.model.VKPost
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import com.selfcode.vkplus.ui.screens.feed.PostCard
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityUiState(
    val community: VKCommunity? = null,
    val posts: List<VKPost> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isMember: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repository: VKRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CommunityUiState())
    val state: StateFlow<CommunityUiState> = _state

    fun load(groupId: Int) {
        viewModelScope.launch {
            _state.value = CommunityUiState(isLoading = true)
            // Load group info
            when (val r = repository.getGroupById(groupId)) {
                is VKResult.Success -> {
                    val g = r.data
                    _state.value = _state.value.copy(
                        community = g,
                        isMember = g.isMember == 1,
                        isLoading = false
                    )
                }
                is VKResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                    return@launch
                }
            }
            // Load wall
            when (val r = repository.getGroupWall(groupId)) {
                is VKResult.Success -> _state.value = _state.value.copy(posts = r.data)
                is VKResult.Error -> {}
            }
        }
    }

    fun loadMore(groupId: Int) {
        if (_state.value.isLoadingMore) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            when (val r = repository.getGroupWall(groupId, offset = _state.value.posts.size)) {
                is VKResult.Success -> _state.value = _state.value.copy(
                    posts = _state.value.posts + r.data,
                    isLoadingMore = false
                )
                is VKResult.Error -> _state.value = _state.value.copy(isLoadingMore = false)
            }
        }
    }

    fun toggleMembership(groupId: Int) {
        viewModelScope.launch {
            val isMember = _state.value.isMember
            val result = if (isMember) repository.leaveGroup(groupId) else repository.joinGroup(groupId)
            if (result is VKResult.Success) {
                _state.value = _state.value.copy(isMember = !isMember)
            }
        }
    }

    fun toggleLike(post: VKPost) {
        viewModelScope.launch { repository.toggleLike(post) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    groupId: Int,
    onBack: () -> Unit,
    onOpenMessages: ((peerId: Int, name: String, photo: String?) -> Unit)? = null,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(groupId) { viewModel.load(groupId) }

    // Pagination
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 4 && !state.isLoadingMore && state.posts.size >= 20
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore(groupId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.community?.name ?: "Сообщество", color = OnSurface, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnSurface) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = CyberBlue)
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.error!!, color = ErrorRed)
            }
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
                // Header
                item {
                    state.community?.let { g ->
                        Column(modifier = Modifier.fillMaxWidth().background(Surface).padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = g.photo200 ?: g.photo100,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(SurfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(g.name, color = OnSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        if (g.verified == 1) {
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Filled.Verified, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    if (g.activity.isNotBlank()) Text(g.activity, color = OnSurfaceMuted, fontSize = 12.sp)
                                    if (g.membersCount > 0) {
                                        val cnt = when {
                                            g.membersCount >= 1_000_000 -> "${g.membersCount / 1_000_000}M подписчиков"
                                            g.membersCount >= 1_000 -> "${g.membersCount / 1_000}K подписчиков"
                                            else -> "${g.membersCount} подписчиков"
                                        }
                                        Text(cnt, color = OnSurfaceMuted, fontSize = 12.sp)
                                    }
                                }
                            }

                            if (g.status.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(g.status, color = OnSurfaceMuted, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }

                            if (g.description.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                var expanded by remember { mutableStateOf(false) }
                                val isLong = g.description.length > 180
                                Text(
                                    if (!expanded && isLong) g.description.take(180) + "…" else g.description,
                                    color = OnSurface, fontSize = 13.sp, lineHeight = 18.sp
                                )
                                if (isLong) Text(
                                    if (expanded) "Скрыть" else "Подробнее",
                                    color = CyberBlue, fontSize = 12.sp,
                                    modifier = Modifier.clickable { expanded = !expanded }.padding(top = 2.dp)
                                )
                            }

                            if (g.site.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(g.site))) } }) {
                                    Icon(Icons.Filled.Language, null, tint = CyberBlue, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(g.site, color = CyberBlue, fontSize = 13.sp)
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Join/Leave
                                Button(
                                    onClick = { viewModel.toggleMembership(groupId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.isMember) SurfaceVariant else CyberBlue
                                    ),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text(
                                        if (state.isMember) "Выйти" else "Вступить",
                                        fontSize = 13.sp,
                                        color = if (state.isMember) ErrorRed else Background
                                    )
                                }

                                // Message button
                                if (g.canMessage == 1 && onOpenMessages != null) {
                                    OutlinedButton(
                                        onClick = { onOpenMessages(-groupId, g.name, g.photo100) },
                                        modifier = Modifier.height(38.dp),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(CyberBlue)
                                        )
                                    ) {
                                        Icon(Icons.Filled.Message, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Сообщения", fontSize = 13.sp, color = CyberBlue)
                                    }
                                }

                                // Open in VK
                                OutlinedButton(
                                    onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/${g.screenName}"))) } },
                                    modifier = Modifier.height(38.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(Divider)
                                    )
                                ) {
                                    Icon(Icons.Filled.OpenInNew, null, tint = OnSurfaceMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = Divider)
                    }
                }

                // Wall posts
                if (state.posts.isNotEmpty()) {
                    item {
                        Text("Записи сообщества", color = OnSurfaceMuted, fontSize = 12.sp, letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            authorName = state.community?.name ?: "",
                            authorPhoto = state.community?.photo100,
                            onLike = { viewModel.toggleLike(post) }
                        )
                        HorizontalDivider(color = Divider.copy(alpha = 0.4f))
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CyberBlue, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
