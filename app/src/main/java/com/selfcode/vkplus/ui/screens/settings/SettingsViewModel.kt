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
    val silentVm: Boolean      = false,
    val offlinePost: Boolean   = false,
    val antiBan: Boolean          = false,
    val bypassActivity: Boolean   = false,
    val bypassLinks: Boolean      = true,
    val bypassShortUrl: Boolean   = false,
    val longPollOnly: Boolean     = false,
    val hardwareSpoof: Boolean    = false,
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
        settingsStore.silentVm,
        settingsStore.offlinePost,
        settingsStore.antiBan,
        settingsStore.bypassActivity,
        settingsStore.bypassLinks,
        settingsStore.bypassShortUrl,
        settingsStore.longPollOnly,
        settingsStore.hardwareSpoof,
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
            silentVm       = arr[9] as Boolean,
            offlinePost    = arr[10] as Boolean,
            antiBan        = arr[11] as Boolean,
            bypassActivity = arr[12] as Boolean,
            bypassLinks    = arr[13] as Boolean,
            bypassShortUrl = arr[14] as Boolean,
            longPollOnly   = arr[15] as Boolean,
            hardwareSpoof  = arr[16] as Boolean,
            deviceUa       = arr[17] as String
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
    fun setSilentVm(v: Boolean)      = viewModelScope.launch { settingsStore.setSilentVm(v) }
    fun setOfflinePost(v: Boolean)   = viewModelScope.launch { settingsStore.setOfflinePost(v) }
    fun setAntiBan(v: Boolean)          = viewModelScope.launch { settingsStore.setAntiBan(v) }
    fun setBypassActivity(v: Boolean)   = viewModelScope.launch { settingsStore.setBypassActivity(v) }
    fun setBypassLinks(v: Boolean)        = viewModelScope.launch { settingsStore.setBypassLinks(v) }
    fun setBypassShortUrl(v: Boolean)     = viewModelScope.launch { settingsStore.setBypassShortUrl(v) }
    fun setLongPollOnly(v: Boolean)       = viewModelScope.launch { settingsStore.setLongPollOnly(v) }
    fun setHardwareSpoof(v: Boolean)      = viewModelScope.launch { settingsStore.setHardwareSpoof(v) }
    fun setSniSpoof(v: Boolean)           = viewModelScope.launch { settingsStore.setSniSpoof(v) }
    fun setUaSwitcher(v: Boolean)         = viewModelScope.launch { settingsStore.setUaSwitcher(v) }

    fun setForceOffline(v: Boolean) = viewModelScope.launch {
        settingsStore.setForceOffline(v)
        if (v) OfflineWorker.schedule(context) else OfflineWorker.cancel(context)
    }

    fun setDeviceProfile(profile: DeviceProfile) = viewModelScope.launch {
        settingsStore.setDeviceUa(profile.ua)
    }
}
