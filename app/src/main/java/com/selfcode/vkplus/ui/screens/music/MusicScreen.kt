package com.selfcode.vkplus.ui.screens.music

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.selfcode.vkplus.data.model.VKAudio
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import com.selfcode.vkplus.ui.screens.messages.downloadFile
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicUiState(
    val tracks: List<VKAudio> = emptyList(),
    val searchResults: List<VKAudio> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val currentTab: Int = 0, // 0=Мои, 1=Поиск
    val addedIds: Set<Int> = emptySet(),
    val deletedIds: Set<Int> = emptySet()
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: VKRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(MusicUiState())
    val state: StateFlow<MusicUiState> = _state

    // Player state
    var player: ExoPlayer? = null
        private set
    private val _currentTrack = MutableStateFlow<VKAudio?>(null)
    val currentTrack: StateFlow<VKAudio?> = _currentTrack
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    private val _currentPositionSec = MutableStateFlow(0)
    val currentPositionSec: StateFlow<Int> = _currentPositionSec
    private var progressJob: Job? = null

    init {
        loadMyAudio()
        initPlayer()
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        _isPlaying.value = false
                        _progress.value = 0f
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }
            })
        }
    }

    fun loadMyAudio() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val r = repository.getMyAudio()) {
                is VKResult.Success -> _state.value = _state.value.copy(tracks = r.data, isLoading = false)
                is VKResult.Error -> _state.value = _state.value.copy(isLoading = false, error = r.message)
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) { _state.value = _state.value.copy(searchResults = emptyList()); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            when (val r = repository.searchAudio(query)) {
                is VKResult.Success -> _state.value = _state.value.copy(searchResults = r.data, isSearching = false)
                is VKResult.Error -> _state.value = _state.value.copy(isSearching = false)
            }
        }
    }

    fun addAudio(audio: VKAudio) {
        viewModelScope.launch {
            when (repository.addAudio(audio.id, audio.ownerId)) {
                is VKResult.Success -> {
                    _state.value = _state.value.copy(addedIds = _state.value.addedIds + audio.id)
                }
                is VKResult.Error -> {}
            }
        }
    }

    fun deleteAudio(audio: VKAudio) {
        viewModelScope.launch {
            when (repository.deleteAudio(audio.id, audio.ownerId)) {
                is VKResult.Success -> {
                    _state.value = _state.value.copy(
                        deletedIds = _state.value.deletedIds + audio.id,
                        tracks = _state.value.tracks.filter { it.id != audio.id }
                    )
                }
                is VKResult.Error -> {}
            }
        }
    }

    fun playTrack(audio: VKAudio) {
        val url = audio.url
        if (url.isBlank()) return
        val p = player ?: return
        _currentTrack.value = audio

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("KateMobileAndroid/56 lite (Android 11; SDK 30; arm64-v8a; Xiaomi; ru)")
            .setDefaultRequestProperties(mapOf("X-VK-Android-Client" to "new"))

        val mediaSource = if (url.contains(".m3u8")) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(url))
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(url))
        }

        p.setMediaSource(mediaSource)
        p.prepare()
        p.play()

        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(300)
                val dur = p.duration.takeIf { it > 0 } ?: continue
                _progress.value = p.currentPosition.toFloat() / dur
                _currentPositionSec.value = (p.currentPosition / 1000).toInt()
            }
        }
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekTo(fraction: Float) {
        val p = player ?: return
        val dur = p.duration.takeIf { it > 0 } ?: return
        p.seekTo((fraction * dur).toLong())
    }

    fun setTab(tab: Int) { _state.value = _state.value.copy(currentTab = tab) }

    fun downloadTrack(context: Context, audio: VKAudio) {
        if (audio.url.isBlank()) return
        val name = "${audio.artist} - ${audio.title}.mp3"
            .replace("[/\\\\:*?\"<>|]".toRegex(), "_")
        downloadFile(context, audio.url, name, "audio/mpeg")
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        player?.release()
        player = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(viewModel: MusicViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentSec by viewModel.currentPositionSec.collectAsState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Tabs
        TabRow(selectedTabIndex = state.currentTab, containerColor = Surface, contentColor = CyberBlue,
            indicator = { tabs -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabs[state.currentTab]), color = CyberBlue) }
        ) {
            Tab(selected = state.currentTab == 0, onClick = { viewModel.setTab(0) },
                text = { Text("Моя музыка", fontSize = 13.sp, color = if (state.currentTab == 0) CyberBlue else OnSurfaceMuted) })
            Tab(selected = state.currentTab == 1, onClick = { viewModel.setTab(1) },
                text = { Text("Поиск", fontSize = 13.sp, color = if (state.currentTab == 1) CyberBlue else OnSurfaceMuted) })
        }

        // Search bar (always visible for search tab)
        if (state.currentTab == 1) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Исполнитель, трек...", color = OnSurfaceMuted, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = OnSurfaceMuted) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = ""; viewModel.search("") }) {
                        Icon(Icons.Filled.Clear, null, tint = OnSurfaceMuted)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(searchQuery); keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        val tracks = if (state.currentTab == 0) state.tracks else state.searchResults
        val isLoading = if (state.currentTab == 0) state.isLoading else state.isSearching

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = CyberBlue)
                }
                state.error != null && state.currentTab == 0 -> Column(
                    Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.MusicOff, null, tint = ErrorRed, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(state.error ?: "", color = ErrorRed, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadMyAudio() }, colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)) {
                        Text("Повторить")
                    }
                }
                tracks.isEmpty() && !isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.MusicNote, null, tint = OnSurfaceMuted, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.currentTab == 1) "Введите запрос для поиска" else "Нет треков",
                            color = OnSurfaceMuted, fontSize = 14.sp
                        )
                    }
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = if (currentTrack != null) 90.dp else 8.dp)) {
                    items(tracks, key = { "${it.ownerId}_${it.id}" }) { audio ->
                        TrackRow(
                            audio = audio,
                            isPlaying = currentTrack?.id == audio.id && isPlaying,
                            isCurrent = currentTrack?.id == audio.id,
                            isMyAudio = state.currentTab == 0,
                            isAdded = state.addedIds.contains(audio.id),
                            onPlay = { viewModel.playTrack(audio) },
                            onAdd = { viewModel.addAudio(audio) },
                            onDelete = { viewModel.deleteAudio(audio) },
                            onDownload = { viewModel.downloadTrack(context, audio) }
                        )
                    }
                }
            }
        }

        // Mini player
        currentTrack?.let { track ->
            MiniPlayer(
                track = track,
                isPlaying = isPlaying,
                progress = progress,
                currentSec = currentSec,
                onToggle = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) }
            )
        }
    }
}

@Composable
private fun TrackRow(
    audio: VKAudio,
    isPlaying: Boolean,
    isCurrent: Boolean,
    isMyAudio: Boolean,
    isAdded: Boolean,
    onPlay: () -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit
) {
    val pulseAnim = rememberInfiniteTransition(label = "p")
    val alpha by pulseAnim.animateFloat(0.5f, 1f,
        infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse), "a")

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (isCurrent) CyberBlue.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play indicator / index
        Box(modifier = Modifier.size(40.dp).background(
            if (isCurrent) CyberBlue.copy(alpha = if (isPlaying) alpha * 0.2f else 0.12f) else SurfaceVariant,
            CircleShape
        ), contentAlignment = Alignment.Center) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                null, tint = if (isCurrent) CyberBlue else OnSurfaceMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(audio.title, color = if (isCurrent) CyberBlue else OnSurface,
                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(audio.artist, color = OnSurfaceMuted, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Text("%d:%02d".format(audio.duration / 60, audio.duration % 60),
            color = OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp))

        // Action buttons
        if (isMyAudio) {
            IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Download, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, null, tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        } else {
            IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Download, null, tint = OnSurfaceMuted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(36.dp), enabled = !isAdded) {
                Icon(if (isAdded) Icons.Filled.Check else Icons.Filled.Add,
                    null, tint = if (isAdded) CyberAccent else CyberBlue, modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider(color = Divider.copy(alpha = 0.3f), modifier = Modifier.padding(start = 64.dp))
}

@Composable
private fun MiniPlayer(
    track: VKAudio,
    isPlaying: Boolean,
    progress: Float,
    currentSec: Int,
    onToggle: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Surface)))
    ) {
        // Progress bar — tappable
        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 0.dp),
            colors = SliderDefaults.colors(
                thumbColor = CyberBlue, activeTrackColor = CyberBlue,
                inactiveTrackColor = Divider, thumbSize = androidx.compose.ui.unit.DpSize(10.dp, 10.dp)
            )
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = OnSurfaceMuted, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("%d:%02d / %d:%02d".format(currentSec / 60, currentSec % 60, track.duration / 60, track.duration % 60),
                color = OnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
            Box(modifier = Modifier.size(44.dp).background(CyberBlue, CircleShape).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center) {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null, tint = Background, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}
