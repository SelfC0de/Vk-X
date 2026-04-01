package com.selfcode.vkplus.ui.screens.profile

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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: VKRepository
) : ViewModel() {
    private val _user = MutableStateFlow<VKUser?>(null)
    val user: StateFlow<VKUser?> = _user
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { loadProfile() }

    fun saveProfile(status: String) {
        viewModelScope.launch {
            repository.saveProfileInfo(status = status)
            loadProfile()
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = repository.getCurrentUser()) {
                is VKResult.Success -> _user.value = r.data
                is VKResult.Error -> {}
            }
            _isLoading.value = false
        }
    }
}
