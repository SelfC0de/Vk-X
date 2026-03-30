package com.selfcode.vkplus.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.local.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkToken()
    }

    private fun checkToken() {
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
        val userId = params["user_id"]?.toIntOrNull() ?: return

        viewModelScope.launch {
            tokenStorage.saveToken(token, userId)
            _authState.value = AuthState.Authenticated
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
}
