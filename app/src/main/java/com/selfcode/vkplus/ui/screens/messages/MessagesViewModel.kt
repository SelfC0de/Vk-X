package com.selfcode.vkplus.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.api.VKConversationsResponse
import com.selfcode.vkplus.data.model.VKDialog
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val dialogs: List<VKDialog> = emptyList(),
    val profiles: Map<Int, VKUser> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repository: VKRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState(isLoading = true))
    val uiState: StateFlow<MessagesUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val r = repository.getConversations()) {
                is VKResult.Success -> _uiState.value = MessagesUiState(
                    dialogs = r.data.items,
                    profiles = r.data.profiles?.associateBy { it.id } ?: emptyMap(),
                    isLoading = false
                )
                is VKResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, error = r.message
                )
            }
        }
    }
}
