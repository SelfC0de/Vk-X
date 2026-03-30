package com.selfcode.vkplus.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.local.TokenStorage
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QrAuthState {
    object Loading : QrAuthState()
    data class QrReady(val url: String, val authHash: String) : QrAuthState()
    object Authenticated : QrAuthState()
    data class Error(val message: String) : QrAuthState()
}

@HiltViewModel
class QrAuthViewModel @Inject constructor(
    private val repository: VKRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _state = MutableStateFlow<QrAuthState>(QrAuthState.Loading)
    val state: StateFlow<QrAuthState> = _state

    init { start() }

    fun start() {
        viewModelScope.launch {
            _state.value = QrAuthState.Loading
            when (val r = repository.getQrCode()) {
                is VKResult.Success -> {
                    val url = r.data.url ?: "https://vk.com/login?act=authapp&hash=${r.data.authHash}"
                    _state.value = QrAuthState.QrReady(url, r.data.authHash)
                    pollStatus(r.data.authHash)
                }
                is VKResult.Error -> _state.value = QrAuthState.Error(r.message)
            }
        }
    }

    private fun pollStatus(authHash: String) {
        viewModelScope.launch {
            repeat(60) {
                delay(3000)
                val current = _state.value
                if (current !is QrAuthState.QrReady) return@launch
                when (val r = repository.checkQrCode(authHash)) {
                    is VKResult.Success -> {
                        if (r.data.status == 1 && r.data.accessToken != null && r.data.userId != null) {
                            tokenStorage.saveToken(r.data.accessToken, r.data.userId)
                            _state.value = QrAuthState.Authenticated
                            return@launch
                        }
                    }
                    is VKResult.Error -> {}
                }
            }
            _state.value = QrAuthState.Error("QR-код устарел")
        }
    }
}
