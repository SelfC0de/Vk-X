package com.selfcode.vkplus.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Alignment
import com.selfcode.vkplus.data.api.ShortUrlResolver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selfcode.vkplus.data.model.VKPost
import com.selfcode.vkplus.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 5 && !state.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && state.nextFrom != null) {
            viewModel.loadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CyberBlue
                )
            }
            state.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error ?: "", color = ErrorRed, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadFeed() }) {
                        Text("Повторить")
                    }
                }
            }
            else -> {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(state.posts, key = { "${it.ownerId}_${it.id}" }) { post ->
                        PostCard(
                            post = post,
                            authorName = resolveAuthorName(post, state),
                            authorPhoto = resolveAuthorPhoto(post, state),
                            onLike = { viewModel.toggleLike(post) }
                        )
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CyberBlue, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveAuthorName(post: VKPost, state: FeedUiState): String {
    return if (post.fromId > 0) {
        state.profiles[post.fromId]?.fullName ?: "Пользователь"
    } else {
        state.groups[-post.fromId]?.name ?: "Сообщество"
    }
}

private fun resolveAuthorPhoto(post: VKPost, state: FeedUiState): String? {
    return if (post.fromId > 0) {
        state.profiles[post.fromId]?.photo100
    } else {
        state.groups[-post.fromId]?.photo100
    }
}

@Composable
fun PostCard(
    post: VKPost,
    authorName: String,
    authorPhoto: String?,
    onLike: () -> Unit
) {
    val date = remember(post.date) {
        SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(post.date * 1000))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(Surface, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = authorPhoto,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = authorName,
                    color = OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = date, color = OnSurfaceMuted, fontSize = 12.sp)
            }
        }

        if (post.text.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = post.text,
                color = OnSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
        }

        post.attachments?.forEach { attachment ->
            when (attachment.type) {
                "photo" -> {
                    val photo = attachment.photo?.bestSize()
                    if (photo != null) {
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                "link" -> {
                    val link = attachment.link
                    if (link != null) {
                        val context = LocalContext.current
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceVariant, RoundedCornerShape(8.dp))
                                .clickable {
                                    val url = link.realUrl
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = CyberBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                if (!link.title.isNullOrBlank()) {
                                    Text(link.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(link.realUrl, color = CyberBlue, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Divider, thickness = 0.5.dp)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val liked = post.likes?.isLiked == true
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onLike() }
            ) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (liked) ErrorRed else OnSurfaceMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${post.likes?.count ?: 0}",
                    color = if (liked) ErrorRed else OnSurfaceMuted,
                    fontSize = 13.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = "${post.comments?.count ?: 0}", color = OnSurfaceMuted, fontSize = 13.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = null,
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = "${post.reposts?.count ?: 0}", color = OnSurfaceMuted, fontSize = 13.sp)
            }

            post.views?.let {
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = OnSurfaceMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = formatCount(it.count), color = OnSurfaceMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}K"
    else -> count.toString()
}
