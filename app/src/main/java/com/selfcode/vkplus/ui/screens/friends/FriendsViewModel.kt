package com.selfcode.vkplus.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val friends: List<VKUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = ""
) {
    val filtered get() = if (query.isBlank()) friends else
        friends.filter { it.fullName.contains(query, ignoreCase = true) }
    val onlineCount get() = friends.count { it.isOnline }
}

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repository: VKRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState(isLoading = true))
    val uiState: StateFlow<FriendsUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val r = repository.getFriends()) {
                is VKResult.Success -> _uiState.value = _uiState.value.copy(
                    friends = r.data.items,
                    isLoading = false
                )
                is VKResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, error = r.message
                )
            }
        }
    }

    fun setQuery(q: String) {
        _uiState.value = _uiState.value.copy(query = q)
    }
}
