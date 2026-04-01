package com.selfcode.vkplus.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.api.VKGroup
import com.selfcode.vkplus.data.model.VKPost
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<VKPost> = emptyList(),
    val profiles: Map<Int, VKUser> = emptyMap(),
    val groups: Map<Int, VKGroup> = emptyMap(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextFrom: String? = null
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: VKRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState(isLoading = true))
    private val _isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRefreshing: kotlinx.coroutines.flow.StateFlow<Boolean> = _isRefreshing
    val uiState: StateFlow<FeedUiState> = _uiState

    init {
        loadFeed()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadFeedInternal()
            _isRefreshing.value = false
        }
    }

    fun loadFeed() {
        viewModelScope.launch { loadFeedInternal() }
    }

    private suspend fun loadFeedInternal() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        when (val result = repository.getNewsfeed()) {
            is VKResult.Success -> {
                val profilesMap = result.data.profiles.associateBy { it.id }
                val groupsMap = result.data.groups.associateBy { it.id } // stored as positive, accessed as -sourceId
                _uiState.value = FeedUiState(
                    posts = result.data.items,
                    profiles = profilesMap,
                    groups = groupsMap,
                    nextFrom = result.data.nextFrom,
                    isLoading = false
                )
            }
            is VKResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.nextFrom == null) return
        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            when (val result = repository.getNewsfeed(startFrom = state.nextFrom)) {
                is VKResult.Success -> {
                    val newProfiles = state.profiles + result.data.profiles.associateBy { it.id }
                    val newGroups = state.groups + result.data.groups.associateBy { it.id }
                    _uiState.value = state.copy(
                        posts = state.posts + result.data.items,
                        profiles = newProfiles,
                        groups = newGroups,
                        nextFrom = result.data.nextFrom,
                        isLoadingMore = false
                    )
                }
                is VKResult.Error -> {
                    _uiState.value = state.copy(isLoadingMore = false)
                }
            }
        }
    }

    fun toggleLike(post: VKPost) {
        viewModelScope.launch {
            repository.toggleLike(post)
            val updated = _uiState.value.posts.map { p ->
                if (p.id == post.id && p.ownerId == post.ownerId) {
                    val wasLiked = p.likes?.isLiked == true
                    val currentCount = p.likes?.count ?: 0
                    p.copy(
                        likes = p.likes?.copy(
                            userLikes = if (wasLiked) 0 else 1,
                            count = if (wasLiked) currentCount - 1 else currentCount + 1
                        )
                    )
                } else p
            }
            _uiState.value = _uiState.value.copy(posts = updated)
        }
    }
}
