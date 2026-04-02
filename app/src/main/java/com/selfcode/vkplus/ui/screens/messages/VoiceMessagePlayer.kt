package com.selfcode.vkplus.ui.screens.messages

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfcode.vkplus.data.model.VKAudioMessage
import com.selfcode.vkplus.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun VoiceMessagePlayer(audio: VKAudioMessage, isOutgoing: Boolean) {
    val url = audio.linkMp3 ?: audio.linkOgg ?: return
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentSec by remember { mutableStateOf(0) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val scope = rememberCoroutineScope()

    // Pseudo-waveform bars derived from duration seed (stable per message)
    val bars = remember(audio.duration) {
        val rng = Random(audio.duration.toLong() * 31 + url.hashCode())
        List(28) { 0.15f + rng.nextFloat() * 0.85f }
    }

    // Pulsing animation when playing
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(600, easing = EaseInOutSine), RepeatMode.Reverse), "pa"
    )

    DisposableEffect(url) {
        onDispose { player?.release(); player = null }
    }

    val accentColor = if (isOutgoing) CyberBlue else CyberAccent
    val bgColor = if (isOutgoing) CyberBlue.copy(alpha = 0.12f) else SurfaceVariant

    Row(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 270.dp)
            .background(bgColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause button
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(accentColor.copy(alpha = if (isPlaying) pulseAlpha * 0.25f else 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentColor, strokeWidth = 2.dp)
            } else {
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            player?.pause(); isPlaying = false
                        } else {
                            if (player == null) {
                                isLoading = true
                                val mp = MediaPlayer()
                                player = mp
                                mp.setDataSource(url)
                                mp.prepareAsync()
                                mp.setOnPreparedListener {
                                    isLoading = false; it.start(); isPlaying = true
                                    scope.launch {
                                        while (true) {
                                            delay(100)
                                            val p = player ?: break
                                            try {
                                                if (!p.isPlaying) break
                                                val dur = p.duration.takeIf { it > 0 } ?: break
                                                progress = p.currentPosition.toFloat() / dur
                                                currentSec = p.currentPosition / 1000
                                            } catch (e: Exception) { break }
                                        }
                                    }
                                }
                                mp.setOnCompletionListener {
                                    isPlaying = false; progress = 1f
                                    currentSec = audio.duration
                                }
                                mp.setOnErrorListener { _, _, _ -> isLoading = false; isPlaying = false; player = null; true }
                            } else {
                                player?.start(); isPlaying = true
                                scope.launch {
                                    while (true) {
                                        delay(100)
                                        val p = player ?: break
                                        try {
                                            if (!p.isPlaying) break
                                            val dur = p.duration.takeIf { it > 0 } ?: break
                                            progress = p.currentPosition.toFloat() / dur
                                            currentSec = p.currentPosition / 1000
                                        } catch (e: Exception) { break }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = accentColor, modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Waveform + time
        Column(modifier = Modifier.weight(1f)) {
            // Waveform bars with tap-to-seek
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val frac = offset.x / size.width
                            player?.let { p ->
                                try {
                                    val dur = p.duration.takeIf { it > 0 } ?: return@detectTapGestures
                                    p.seekTo((frac * dur).toInt())
                                    progress = frac
                                    currentSec = (frac * audio.duration).toInt()
                                } catch (e: Exception) {}
                            }
                        }
                    }
            ) {
                val barW = (size.width - bars.size * 2f) / bars.size
                val maxH = size.height
                bars.forEachIndexed { i, amp ->
                    val x = i * (barW + 2f)
                    val h = amp * maxH * 0.9f + maxH * 0.1f
                    val played = (i.toFloat() / bars.size) < progress
                    val barColor = if (played) accentColor else accentColor.copy(alpha = 0.3f)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, (maxH - h) / 2f),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f)
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // Time display
            Row {
                val displaySec = if (isPlaying || progress > 0f) currentSec else audio.duration
                Text(
                    formatDuration(displaySec),
                    color = OnSurfaceMuted, fontSize = 10.sp
                )
                if (isPlaying || progress > 0f) {
                    Spacer(Modifier.width(4.dp))
                    Text("/ ${formatDuration(audio.duration)}", color = OnSurfaceMuted.copy(0.5f), fontSize = 10.sp)
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
