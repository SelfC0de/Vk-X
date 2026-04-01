package com.selfcode.vkplus.ui.screens.feed

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.api.VKGroup
import com.selfcode.vkplus.data.model.VKPost
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Pagination trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 5 && !state.isLoadingMore && state.nextFrom != null
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(700, easing = LinearEasing)), label = "rot"
    )

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            state.isLoading && state.posts.isEmpty() ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CyberBlue)

            state.error != null ->
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ошибка загрузки", color = ErrorRed, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text(state.error ?: "", color = OnSurfaceMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.loadFeed() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)) {
                        Text("Повторить", color = Background)
                    }
                }

            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)) {
                items(state.posts, key = { "${it.authorId}_${it.id}" }) { post ->
                    PostCard(
                        post = post,
                        authorName = resolveAuthorName(post, state),
                        authorPhoto = resolveAuthorPhoto(post, state),
                        onLike = { viewModel.toggleLike(post) }
                    )
                    HorizontalDivider(color = Divider.copy(alpha = 0.4f))
                }
                if (state.isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyberBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }

        // Refresh FAB
        FloatingActionButton(
            onClick = { if (!isRefreshing) viewModel.refresh() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(48.dp),
            containerColor = CyberBlue, contentColor = Background, shape = CircleShape
        ) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(22.dp)
                .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier))
        }
    }
}

private fun resolveAuthorName(post: VKPost, state: FeedUiState): String {
    val id = post.authorId
    return when {
        id > 0  -> state.profiles[id]?.fullName ?: "Пользователь"
        id < 0  -> state.groups[-id]?.name      ?: "Сообщество"
        else    -> "VK"
    }
}

private fun resolveAuthorPhoto(post: VKPost, state: FeedUiState): String? {
    val id = post.authorId
    return when {
        id > 0 -> state.profiles[id]?.photo100
        id < 0 -> state.groups[-id]?.photo100
        else   -> null
    }
}

@Composable
fun PostCard(post: VKPost, authorName: String, authorPhoto: String?, onLike: () -> Unit) {
    val context = LocalContext.current
    val date = remember(post.date) {
        SimpleDateFormat("d MMM · HH:mm", Locale("ru")).format(Date(post.date * 1000))
    }
    val isLiked = post.likes?.isLiked == true

    Column(modifier = Modifier.fillMaxWidth().background(Background).padding(horizontal = 12.dp, vertical = 10.dp)) {
        // Author header
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = authorPhoto, contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(authorName, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(date, color = OnSurfaceMuted, fontSize = 12.sp)
            }
        }

        // Post text
        if (post.text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            var expanded by remember { mutableStateOf(false) }
            val isLong = post.text.length > 300
            Text(
                text = if (!expanded && isLong) post.text.take(300) + "…" else post.text,
                color = OnSurface, fontSize = 14.sp, lineHeight = 20.sp
            )
            if (isLong) {
                Text(
                    text = if (expanded) "Скрыть" else "Показать полностью",
                    color = CyberBlue, fontSize = 13.sp,
                    modifier = Modifier.clickable { expanded = !expanded }.padding(top = 4.dp)
                )
            }
        }

        // Attachments
        post.attachments?.forEach { attach ->
            when (attach.type) {
                "photo" -> attach.photo?.let { photo ->
                    val url = photo.sizes?.maxByOrNull { it.width * it.height }?.url
                    if (url != null) {
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = url, contentDescription = null,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .heightIn(max = 320.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                "link" -> attach.link?.let { link ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(SurfaceVariant, RoundedCornerShape(10.dp))
                            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Share, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(link.title.ifBlank { link.url }, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(link.url, color = OnSurfaceMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Action bar
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Like
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onLike() }) {
                Icon(
                    if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    null, tint = if (isLiked) ErrorRed else OnSurfaceMuted,
                    modifier = Modifier.size(18.dp)
                )
                if ((post.likes?.count ?: 0) > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text("${post.likes?.count}", color = if (isLiked) ErrorRed else OnSurfaceMuted, fontSize = 13.sp)
                }
            }
            // Comments
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                if ((post.comments?.count ?: 0) > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text("${post.comments?.count}", color = OnSurfaceMuted, fontSize = 13.sp)
                }
            }
            // Reposts
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Repeat, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
                if ((post.reposts?.count ?: 0) > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text("${post.reposts?.count}", color = OnSurfaceMuted, fontSize = 13.sp)
                }
            }
            // Views
            post.views?.count?.let { views ->
                if (views > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Visibility, null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(formatViews(views), color = OnSurfaceMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun formatViews(n: Int) = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000 -> "${n / 1_000}K"
    else -> n.toString()
}
