package com.selfcode.vkplus.ui.screens.messages

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfcode.vkplus.data.model.VKAudioMessage
import com.selfcode.vkplus.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceMessagePlayer(audio: VKAudioMessage, isOutgoing: Boolean) {
    val url = audio.linkMp3 ?: audio.linkOgg ?: return
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(url) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Row(
        modifier = Modifier
            .widthIn(min = 180.dp, max = 260.dp)
            .background(
                if (isOutgoing) CyberBlue.copy(alpha = 0.2f) else SurfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    player?.pause()
                    isPlaying = false
                } else {
                    if (player == null) {
                        player = MediaPlayer().apply {
                            setDataSource(url)
                            prepareAsync()
                            setOnPreparedListener { start(); isPlaying = true }
                            setOnCompletionListener { isPlaying = false; progress = 0f }
                        }
                        scope.launch {
                            while (true) {
                                delay(200)
                                val p = player ?: break
                                if (!p.isPlaying) break
                                val dur = p.duration.takeIf { it > 0 } ?: break
                                progress = p.currentPosition.toFloat() / dur
                            }
                        }
                    } else {
                        player?.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier.size(36.dp).background(CyberBlue.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = CyberBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = CyberBlue,
                trackColor = Divider
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = OnSurfaceMuted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatDuration(audio.duration),
                    color = OnSurfaceMuted,
                    fontSize = 11.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Silent",
                    color = CyberAccent,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
