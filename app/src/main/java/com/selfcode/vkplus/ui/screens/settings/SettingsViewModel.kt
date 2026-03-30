package com.selfcode.vkplus.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfcode.vkplus.data.api.OfflineWorker
import com.selfcode.vkplus.data.local.DeviceProfile
import com.selfcode.vkplus.data.local.SettingsStore
import com.selfcode.vkplus.data.local.TypeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val unRead: Boolean        = false,
    val unType: Boolean        = false,
    val forceOffline: Boolean  = false,
    val ghostOnline: Boolean   = false,
    val antiTelemetry: Boolean = false,
    val antiScreen: Boolean    = false,
    val typePush: Boolean      = false,
    val ghostStory: Boolean    = false,
    val typeStatus: String     = com.selfcode.vkplus.data.local.TypeStatus.NONE.value,
    val deviceUa: String       = DeviceProfile.KATE.ua
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsStore.unRead,
        settingsStore.unType,
        settingsStore.forceOffline,
        settingsStore.ghostOnline,
        settingsStore.antiTelemetry,
        settingsStore.antiScreen,
        settingsStore.typePush,
        settingsStore.ghostStory,
        settingsStore.typeStatus,
        settingsStore.deviceUa
    ) { arr ->
        SettingsUiState(
            unRead         = arr[0] as Boolean,
            unType         = arr[1] as Boolean,
            forceOffline   = arr[2] as Boolean,
            ghostOnline    = arr[3] as Boolean,
            antiTelemetry  = arr[4] as Boolean,
            antiScreen     = arr[5] as Boolean,
            typePush       = arr[6] as Boolean,
            ghostStory     = arr[7] as Boolean,
            typeStatus     = arr[8] as String,
            deviceUa       = arr[9] as String
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setUnRead(v: Boolean)        = viewModelScope.launch { settingsStore.setUnRead(v) }
    fun setUnType(v: Boolean)        = viewModelScope.launch { settingsStore.setUnType(v) }
    fun setGhostOnline(v: Boolean)   = viewModelScope.launch { settingsStore.setGhostOnline(v) }
    fun setAntiTelemetry(v: Boolean) = viewModelScope.launch { settingsStore.setAntiTelemetry(v) }
    fun setAntiScreen(v: Boolean)    = viewModelScope.launch { settingsStore.setAntiScreen(v) }
    fun setTypePush(v: Boolean)      = viewModelScope.launch { settingsStore.setTypePush(v) }
    fun setGhostStory(v: Boolean)    = viewModelScope.launch { settingsStore.setGhostStory(v) }
    fun setTypeStatus(v: String)     = viewModelScope.launch { settingsStore.setTypeStatus(v) }

    fun setForceOffline(v: Boolean) = viewModelScope.launch {
        settingsStore.setForceOffline(v)
        if (v) OfflineWorker.schedule(context) else OfflineWorker.cancel(context)
    }

    fun setDeviceProfile(profile: DeviceProfile) = viewModelScope.launch {
        settingsStore.setDeviceUa(profile.ua)
    }
}
