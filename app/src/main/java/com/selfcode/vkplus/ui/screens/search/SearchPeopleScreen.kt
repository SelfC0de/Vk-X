package com.selfcode.vkplus.ui.screens.search

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
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import com.selfcode.vkplus.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchPeopleViewModel @Inject constructor(private val repository: VKRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _results = MutableStateFlow<List<VKUser>>(emptyList())
    val results: StateFlow<List<VKUser>> = _results
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            _query.debounce(500).filter { it.length >= 2 }.collectLatest { q ->
                _loading.value = true
                when (val r = repository.searchPeople(q)) {
                    is VKResult.Success -> _results.value = r.data
                    is VKResult.Error -> {}
                }
                _loading.value = false
            }
        }
    }

    fun setQuery(q: String) { _query.value = q }
    fun addFriend(userId: Int) { viewModelScope.launch { repository.addFriend(userId) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPeopleScreen(
    onProfile: (String) -> Unit,
    viewModel: SearchPeopleViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Поиск людей...", color = OnSurfaceMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceMuted) },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberBlue, unfocusedBorderColor = Divider,
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = CyberBlue
            ),
            shape = RoundedCornerShape(10.dp), singleLine = true
        )
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberBlue)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onProfile(user.id.toString()) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = user.photo100, contentDescription = null,
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(SurfaceVariant),
                            contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.fullName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (!user.status.isNullOrBlank()) Text(user.status, color = OnSurfaceMuted, fontSize = 12.sp)
                        }
                        TextButton(onClick = { viewModel.addFriend(user.id) }) {
                            Text("Добавить", color = CyberBlue, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Divider.copy(alpha = 0.4f), modifier = Modifier.padding(start = 70.dp))
                }
            }
        }
    }
}
