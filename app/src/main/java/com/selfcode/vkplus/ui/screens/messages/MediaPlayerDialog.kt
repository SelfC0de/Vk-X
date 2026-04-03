package com.selfcode.vkplus.ui.screens.messages

import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.selfcode.vkplus.ui.theme.*
import kotlinx.coroutines.delay

sealed class MediaPlayItem {
    data class Audio(val url: String, val artist: String, val title: String, val duration: Int) : MediaPlayItem()
    data class Video(val url: String, val title: String, val thumb: String?) : MediaPlayItem()
    data class Gif(val url: String) : MediaPlayItem()
}

@Composable
fun MediaPlayerDialog(item: MediaPlayItem, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.95f).clip(RoundedCornerShape(16.dp)).background(Surface),
            contentAlignment = Alignment.Center
        ) {
            when (item) {
                is MediaPlayItem.Audio -> AudioPlayerContent(item, onDismiss)
                is MediaPlayItem.Video -> VideoPlayerContent(item, onDismiss)
                is MediaPlayItem.Gif   -> GifPlayerContent(item, onDismiss)
            }
        }
    }
}

@Composable
private fun AudioPlayerContent(item: MediaPlayItem.Audio, onDismiss: () -> Unit) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var position by remember { mutableStateOf(0) }
    val player = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }

    LaunchedEffect(item.url) {
        isLoading = true
        runCatching {
            player.setDataSource(item.url)
            player.prepareAsync()
            player.setOnPreparedListener {
                isLoading = false; it.start(); isPlaying = true
            }
            player.setOnCompletionListener { isPlaying = false; progress = 0f; position = 0 }
            player.setOnErrorListener { _, _, _ -> isLoading = false; false }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(300)
            runCatching {
                val dur = player.duration.takeIf { it > 0 } ?: return@runCatching
                progress = player.currentPosition.toFloat() / dur
                position = player.currentPosition / 1000
            }
        }
    }

    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("🎵 Аудио", color = OnSurfaceMuted, fontSize = 12.sp)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.size(80.dp).background(CyberBlue.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MusicNote, null, tint = CyberBlue, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("${item.artist} — ${item.title}", color = OnSurface, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(16.dp))
        Slider(value = progress,
            onValueChange = {
                runCatching {
                    val dur = player.duration.takeIf { it > 0 } ?: return@runCatching
                    player.seekTo((it * dur).toInt())
                }
            },
            colors = SliderDefaults.colors(thumbColor = CyberBlue, activeTrackColor = CyberBlue, inactiveTrackColor = Divider)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position.toLong()), color = OnSurfaceMuted, fontSize = 11.sp)
            Text(formatTime(item.duration.toLong()), color = OnSurfaceMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { runCatching { player.seekTo(maxOf(0, player.currentPosition - 10000)) } }) {
                Icon(Icons.Filled.Replay10, null, tint = OnSurface, modifier = Modifier.size(28.dp))
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = CyberBlue)
            } else {
                Box(modifier = Modifier.size(52.dp).background(CyberBlue, CircleShape).clickable {
                    runCatching { if (player.isPlaying) { player.pause(); isPlaying = false } else { player.start(); isPlaying = true } }
                }, contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = Background, modifier = Modifier.size(28.dp))
                }
            }
            IconButton(onClick = { runCatching { val dur = player.duration; player.seekTo(minOf(dur, player.currentPosition + 10000)) } }) {
                Icon(Icons.Filled.Forward10, null, tint = OnSurface, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VideoPlayerContent(item: MediaPlayItem.Video, onDismiss: () -> Unit) {
    val player = remember { MediaPlayer() }
    var isReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(item.title.take(40), color = OnSurface, fontSize = 13.sp,
                fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            }
        }
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600)
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(h: SurfaceHolder) {
                            runCatching {
                                player.setDisplay(h)
                                if (!isReady) {
                                    player.setDataSource(item.url)
                                    player.prepareAsync()
                                    player.setOnPreparedListener { it.start(); isReady = true }
                                }
                            }
                        }
                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, he: Int) {}
                        override fun surfaceDestroyed(h: SurfaceHolder) {}
                    })
                }
            },
            modifier = Modifier.fillMaxWidth().height(240.dp)
        )
    }
}

@Composable
private fun GifPlayerContent(item: MediaPlayItem.Gif, onDismiss: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("GIF", color = CyberAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
            }
        }
        AsyncImage(model = item.url, contentDescription = "GIF",
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp))
    }
}

private fun formatTime(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)
