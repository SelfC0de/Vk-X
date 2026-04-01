package com.selfcode.vkplus.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.local.TokenStorage
import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val repository: VKRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState

    init { checkToken() }

    fun checkToken() {
        viewModelScope.launch {
            val token = tokenStorage.accessToken.first()
            _authState.value = if (!token.isNullOrEmpty()) {
                AuthState.Authenticated
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    fun handleRedirectUri(uri: Uri) {
        val fragment = uri.fragment ?: return
        val params = fragment.split("&").associate {
            val kv = it.split("=")
            kv[0] to kv.getOrElse(1) { "" }
        }
        val token = params["access_token"] ?: return
        val userId = params["user_id"]?.toIntOrNull() ?: 0
        viewModelScope.launch {
            // Save token first so getCurrentUser can use it
            tokenStorage.saveToken(token, userId)
            _authState.value = AuthState.Authenticated
            // Fetch name+photo and update account record
            try {
                when (val r = repository.getCurrentUser()) {
                    is VKResult.Success -> {
                        val user = r.data
                        tokenStorage.saveToken(token, user.id, name = user.fullName, photo = user.photo100 ?: "")
                    }
                    else -> {}
                }
            } catch (e: Exception) { /* non-critical */ }
        }
    }

    fun saveManualToken(token: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Checking
            tokenStorage.saveToken(token, 0)
            when (val result = repository.getCurrentUser()) {
                is VKResult.Success -> {
                    val user = result.data
                    tokenStorage.saveToken(token, user.id, name = user.fullName, photo = user.photo100 ?: "")
                    _authState.value = AuthState.Authenticated
                }
                is VKResult.Error -> {
                    tokenStorage.clearToken()
                    _authState.value = AuthState.InvalidToken
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenStorage.clearToken()
            _authState.value = AuthState.Unauthenticated
        }
    }
}

sealed class AuthState {
    object Checking : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object InvalidToken : AuthState()
}
