package com.selfcode.vkplus.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.local.SavedAccount
import com.selfcode.vkplus.data.local.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSwitcherViewModel @Inject constructor(
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<SavedAccount>>(emptyList())
    val accounts: StateFlow<List<SavedAccount>> = _accounts

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId

    init {
        viewModelScope.launch {
            tokenStorage.savedAccounts.collectLatest { _accounts.value = it }
        }
        viewModelScope.launch {
            tokenStorage.userId.collectLatest { _currentUserId.value = it }
        }
    }

    fun switchAccount(account: SavedAccount, onSwitched: () -> Unit) {
        viewModelScope.launch {
            tokenStorage.switchAccount(account)
            onSwitched()
        }
    }

    fun removeAccount(userId: Int) {
        viewModelScope.launch {
            tokenStorage.removeAccount(userId)
        }
    }

    fun addNewAccount(onAdd: () -> Unit) = onAdd()
}
