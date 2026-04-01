package com.selfcode.vkplus.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.model.VKPhoto
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
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _profilePhotos = MutableStateFlow<List<VKPhoto>>(emptyList())
    val profilePhotos: StateFlow<List<VKPhoto>> = _profilePhotos

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val r = repository.getCurrentUser()) {
                is VKResult.Success -> {
                    _user.value = r.data
                    loadProfilePhotos(r.data.id)
                }
                is VKResult.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }

    private fun loadProfilePhotos(ownerId: Int) {
        viewModelScope.launch {
            when (val r = repository.getAlbumPhotos(ownerId, "wall")) {
                is VKResult.Success -> _profilePhotos.value = r.data.take(9)
                is VKResult.Error -> {}
            }
        }
    }

    fun saveProfile(status: String) {
        viewModelScope.launch {
            repository.saveProfileInfo(status = status)
            loadProfile()
        }
    }
}
