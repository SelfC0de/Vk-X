package com.selfcode.vkplus.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.model.VKDialog
import com.selfcode.vkplus.data.model.VKMessage
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.data.local.SettingsStore
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val dialogs: List<VKDialog> = emptyList(),
    val profiles: Map<Int, VKUser> = emptyMap(),
    val chatMessages: Map<Int, List<VKMessage>> = emptyMap(),
    val isLoading: Boolean = false,
    val isChatLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repository: VKRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState(isLoading = true))
    val uiState: StateFlow<MessagesUiState> = _uiState

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    init { load() }

    fun loadConversations() = load()

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

    fun loadMessages(peerId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChatLoading = true)
            val useExecute = settingsStore.bypassActivity.first()
            val r = if (useExecute) repository.getMessagesExecute(peerId) else repository.getMessages(peerId)
            when (r) {
                is VKResult.Success -> {
                    val current = _uiState.value.chatMessages.toMutableMap()
                    current[peerId] = r.data.reversed()
                    _uiState.value = _uiState.value.copy(
                        chatMessages = current,
                        isChatLoading = false
                    )
                }
                is VKResult.Error -> {
                    _uiState.value = _uiState.value.copy(isChatLoading = false)
                }
            }
        }
    }

    fun sendMessage(peerId: Int, text: String) {
        viewModelScope.launch {
            _isSending.value = true
            when (repository.sendMessage(peerId, text)) {
                is VKResult.Success -> loadMessages(peerId)
                is VKResult.Error -> {}
            }
            _isSending.value = false
        }
    }

    fun deleteMessage(peerId: Int, messageId: Int) {
        viewModelScope.launch {
            repository.deleteMessage(peerId, messageId)
            val current = _uiState.value.chatMessages.toMutableMap()
            current[peerId] = current[peerId]?.filter { it.id != messageId } ?: emptyList()
            _uiState.value = _uiState.value.copy(chatMessages = current)
        }
    }

    fun editMessage(peerId: Int, messageId: Int, newText: String) {
        viewModelScope.launch {
            _isSending.value = true
            when (repository.editMessage(peerId, messageId, newText)) {
                is VKResult.Success -> loadMessages(peerId)
                is VKResult.Error -> {}
            }
            _isSending.value = false
        }
    }
}
