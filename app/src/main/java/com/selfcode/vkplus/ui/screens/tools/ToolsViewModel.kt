package com.selfcode.vkplus.ui.screens.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.api.VKGift
import com.selfcode.vkplus.data.model.VKUser
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GiftInfo(val gift: VKGift, val realSenderId: Int)

data class ToolsUiState(
    val doxxedGifts: List<GiftInfo> = emptyList(),
    val isLoadingGifts: Boolean = false,
    val giftScanDone: Boolean = false,
    val bannedFriends: List<VKUser> = emptyList(),
    val isScanning: Boolean = false,
    val isRemoving: Boolean = false,
    val removedCount: Int = 0,
    val scanDone: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val repository: VKRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolsUiState())
    val uiState: StateFlow<ToolsUiState> = _uiState

    fun scanBannedFriends() {
        viewModelScope.launch {
            _uiState.value = ToolsUiState(isScanning = true)
            when (val r = repository.getBannedFriends()) {
                is VKResult.Success -> _uiState.value = ToolsUiState(
                    bannedFriends = r.data,
                    scanDone = true
                )
                is VKResult.Error -> _uiState.value = ToolsUiState(
                    error = r.message,
                    scanDone = true
                )
            }
        }
    }

    fun removeAllBanned() {
        val list = _uiState.value.bannedFriends
        if (list.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRemoving = true, removedCount = 0)
            var removed = 0
            list.forEach { user ->
                when (repository.removeFriend(user.id)) {
                    is VKResult.Success -> removed++
                    is VKResult.Error -> {}
                }
                delay(334) // ~3 req/sec — VK rate limit
                _uiState.value = _uiState.value.copy(removedCount = removed)
            }
            _uiState.value = _uiState.value.copy(
                isRemoving = false,
                bannedFriends = emptyList(),
                scanDone = true
            )
        }
    }

    fun scanGifts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGifts = true, giftScanDone = false)
            when (val r = repository.getGifts()) {
                is VKResult.Success -> {
                    val doxxed = r.data.items
                        .filter { it.hasRealSender }
                        .map { GiftInfo(it, it.fromId!!) }
                    _uiState.value = _uiState.value.copy(
                        doxxedGifts = doxxed,
                        isLoadingGifts = false,
                        giftScanDone = true
                    )
                }
                is VKResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingGifts = false, giftScanDone = true
                )
            }
        }
    }

    fun reset() {
        _uiState.value = ToolsUiState()
    }
}
