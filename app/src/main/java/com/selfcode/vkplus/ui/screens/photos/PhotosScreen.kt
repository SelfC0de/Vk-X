package com.selfcode.vkplus.ui.screens.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.selfcode.vkplus.data.model.VKAlbum
import com.selfcode.vkplus.data.model.VKPhoto
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(private val repository: VKRepository) : ViewModel() {
    private val _albums = MutableStateFlow<List<VKAlbum>>(emptyList())
    val albums: StateFlow<List<VKAlbum>> = _albums
    private val _photos = MutableStateFlow<List<VKPhoto>>(emptyList())
    val photos: StateFlow<List<VKPhoto>> = _photos
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _currentAlbum = MutableStateFlow<VKAlbum?>(null)
    val currentAlbum: StateFlow<VKAlbum?> = _currentAlbum

    fun loadAlbums(ownerId: Int? = null) {
        viewModelScope.launch {
            _loading.value = true
            when (val r = repository.getPhotoAlbums(ownerId)) {
                is VKResult.Success -> _albums.value = r.data
                is VKResult.Error -> {}
            }
            _loading.value = false
        }
    }

    fun openAlbum(album: VKAlbum) {
        _currentAlbum.value = album
        viewModelScope.launch {
            _loading.value = true
            when (val r = repository.getAlbumPhotos(album.ownerId, album.id.toString())) {
                is VKResult.Success -> _photos.value = r.data
                is VKResult.Error -> {}
            }
            _loading.value = false
        }
    }

    fun closeAlbum() { _currentAlbum.value = null; _photos.value = emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(ownerId: Int? = null, viewModel: PhotosViewModel = hiltViewModel()) {
    val albums by viewModel.albums.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val currentAlbum by viewModel.currentAlbum.collectAsState()

    LaunchedEffect(ownerId) { viewModel.loadAlbums(ownerId) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        TopAppBar(
            title = { Text(currentAlbum?.title ?: "Фотографии", color = OnSurface, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                if (currentAlbum != null) {
                    IconButton(onClick = { viewModel.closeAlbum() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = CyberBlue)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberBlue)
            }
        } else if (currentAlbum == null) {
            // Albums grid
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(albums, key = { it.id }) { album ->
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)).background(SurfaceVariant)
                        .clickable { viewModel.openAlbum(album) }) {
                        AsyncImage(model = album.thumb?.bestSize()?.url, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .background(Background.copy(alpha = 0.65f)).padding(8.dp)) {
                            Column {
                                Text(album.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("${album.size} фото", color = OnSurfaceMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Photos grid
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(photos, key = { it.id }) { photo ->
                    AsyncImage(model = photo.bestSize()?.url, contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp)).background(SurfaceVariant),
                        contentScale = ContentScale.Crop)
                }
            }
        }
    }
}
