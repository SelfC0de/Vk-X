package com.selfcode.vkplus.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.model.VKDialog
import com.selfcode.vkplus.domain.TranslateUseCase
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
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<VKMessage> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repository: VKRepository,
    private val settingsStore: SettingsStore,
    private val translateUseCase: TranslateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState(isLoading = true))
    val uiState: StateFlow<MessagesUiState> = _uiState

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _translatedMessages = MutableStateFlow<Map<Int, String>>(emptyMap())
    val translatedMessages: StateFlow<Map<Int, String>> = _translatedMessages

    private val _lastActivity = MutableStateFlow<Map<Int, String>>(emptyMap())
    val lastActivity: StateFlow<Map<Int, String>> = _lastActivity

    private val _translateLoading = MutableStateFlow<Set<Int>>(emptySet())
    val translateLoading: StateFlow<Set<Int>> = _translateLoading

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

    fun sendMessage(peerId: Int, text: String, replyToId: Int? = null, forwardIds: List<Int>? = null) {
        viewModelScope.launch {
            _isSending.value = true
            when (repository.sendMessage(peerId, text, replyTo = replyToId, forwardMessages = forwardIds?.joinToString(","))) {
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

    fun translateMessage(messageId: Int, text: String) {
        viewModelScope.launch {
            _translateLoading.value = _translateLoading.value + messageId
            when (val r = translateUseCase.translate(text)) {
                is VKResult.Success -> {
                    _translatedMessages.value = _translatedMessages.value + (messageId to r.data)
                }
                is VKResult.Error -> {}
            }
            _translateLoading.value = _translateLoading.value - messageId
        }
    }

    fun clearTranslation(messageId: Int) {
        _translatedMessages.value = _translatedMessages.value - messageId
    }

    fun translateInput(text: String, targetLang: String = "en", onResult: (String) -> Unit) {
        viewModelScope.launch {
            when (val r = translateUseCase.translate(text, targetLang)) {
                is VKResult.Success -> onResult(r.data)
                is VKResult.Error -> {}
            }
        }
    }

    fun loadLastActivity(userId: Int) {
        viewModelScope.launch {
            when (val r = repository.getLastActivity(userId)) {
                is VKResult.Success -> {
                    val activity = r.data
                    val timeStr = if (activity.online == 1) "онлайн" else {
                        val sdf = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("ru"))
                        sdf.format(java.util.Date(activity.time * 1000))
                    }
                    _lastActivity.value = _lastActivity.value + (userId to timeStr)
                }
                is VKResult.Error -> {}
            }
        }
    }

    fun restoreMessage(peerId: Int, messageId: Int) {
        viewModelScope.launch {
            when (repository.restoreMessage(messageId)) {
                is VKResult.Success -> loadMessages(peerId)
                is VKResult.Error -> {}
            }
        }
    }

    fun loadMoreConversations() {
        val state = _uiState.value
        if (state.isLoadingMore || state.dialogs.size >= state.totalCount) return
        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            when (val r = repository.getConversationsPaged(offset = state.dialogs.size)) {
                is VKResult.Success -> {
                    val newProfiles = r.data.profiles?.associateBy { it.id } ?: emptyMap()
                    _uiState.value = _uiState.value.copy(
                        dialogs = state.dialogs + r.data.items,
                        profiles = state.profiles + newProfiles,
                        totalCount = r.data.count,
                        isLoadingMore = false
                    )
                }
                is VKResult.Error -> _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun setSearchQuery(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
        if (q.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            when (val r = repository.searchMessages(q)) {
                is VKResult.Success -> _uiState.value = _uiState.value.copy(searchResults = r.data, isSearching = false)
                is VKResult.Error -> _uiState.value = _uiState.value.copy(isSearching = false)
            }
        }
    }

    fun sendReaction(peerId: Int, messageId: Int, reactionId: Int) {
        viewModelScope.launch { repository.sendReaction(peerId, messageId, reactionId) }
    }
}
