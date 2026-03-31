package com.selfcode.vkplus.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vkplus_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_UNREAD        = booleanPreferencesKey("ghost_mode")
        val KEY_UNTYPE        = booleanPreferencesKey("anti_typing")
        val KEY_FORCE_OFFLINE = booleanPreferencesKey("force_offline")
        val KEY_GHOST_ONLINE  = booleanPreferencesKey("ghost_online")
        val KEY_ANTI_TELEM    = booleanPreferencesKey("anti_telemetry")
        val KEY_ANTI_SCREEN   = booleanPreferencesKey("anti_screen")
        val KEY_TYPE_PUSH     = booleanPreferencesKey("type_push")
        val KEY_DEVICE_UA     = stringPreferencesKey("device_ua")
        val KEY_BYPASS_COPY      = booleanPreferencesKey("bypass_copy")
        val KEY_GHOST_STORY      = booleanPreferencesKey("ghost_story")
        val KEY_TYPE_STATUS      = stringPreferencesKey("type_status")
        val KEY_SILENT_VM        = booleanPreferencesKey("silent_vm")
        val KEY_OFFLINE_POST     = booleanPreferencesKey("offline_post")
        val KEY_ANTI_BAN           = booleanPreferencesKey("anti_ban")
        val KEY_BYPASS_ACTIVITY    = booleanPreferencesKey("bypass_activity")
        val KEY_BYPASS_LINKS       = booleanPreferencesKey("bypass_links")
    }

    val unRead:       Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_UNREAD]        ?: false }
    val unType:       Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_UNTYPE]        ?: false }
    val forceOffline: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_FORCE_OFFLINE] ?: false }
    val ghostOnline:  Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_GHOST_ONLINE]  ?: false }
    val antiTelemetry:Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_ANTI_TELEM]    ?: false }
    val antiScreen:   Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_ANTI_SCREEN]   ?: false }
    val typePush:     Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_TYPE_PUSH]     ?: false }
    val deviceUa:     Flow<String>  = context.settingsDataStore.data.map { it[KEY_DEVICE_UA]     ?: DeviceProfile.KATE.ua }
    val bypassCopy:   Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_BYPASS_COPY]   ?: true }
    val ghostStory:   Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_GHOST_STORY]   ?: false }
    val typeStatus:   Flow<String>  = context.settingsDataStore.data.map { it[KEY_TYPE_STATUS]   ?: TypeStatus.NONE.value }
    val silentVm:     Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_SILENT_VM]     ?: false }
    val offlinePost:  Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_OFFLINE_POST]  ?: false }
    val antiBan:         Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_ANTI_BAN]         ?: false }
    val bypassActivity:  Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_BYPASS_ACTIVITY]  ?: false }
    val bypassLinks:     Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_BYPASS_LINKS]     ?: true }

    suspend fun setUnRead(v: Boolean)        = context.settingsDataStore.edit { it[KEY_UNREAD]        = v }
    suspend fun setUnType(v: Boolean)        = context.settingsDataStore.edit { it[KEY_UNTYPE]        = v }
    suspend fun setForceOffline(v: Boolean)  = context.settingsDataStore.edit { it[KEY_FORCE_OFFLINE] = v }
    suspend fun setGhostOnline(v: Boolean)   = context.settingsDataStore.edit { it[KEY_GHOST_ONLINE]  = v }
    suspend fun setAntiTelemetry(v: Boolean) = context.settingsDataStore.edit { it[KEY_ANTI_TELEM]    = v }
    suspend fun setAntiScreen(v: Boolean)    = context.settingsDataStore.edit { it[KEY_ANTI_SCREEN]   = v }
    suspend fun setTypePush(v: Boolean)      = context.settingsDataStore.edit { it[KEY_TYPE_PUSH]     = v }
    suspend fun setDeviceUa(ua: String)      = context.settingsDataStore.edit { it[KEY_DEVICE_UA]     = ua }
    suspend fun setBypassCopy(v: Boolean)    = context.settingsDataStore.edit { it[KEY_BYPASS_COPY]   = v }
    suspend fun setGhostStory(v: Boolean)    = context.settingsDataStore.edit { it[KEY_GHOST_STORY]   = v }
    suspend fun setTypeStatus(v: String)     = context.settingsDataStore.edit { it[KEY_TYPE_STATUS]   = v }
    suspend fun setSilentVm(v: Boolean)      = context.settingsDataStore.edit { it[KEY_SILENT_VM]     = v }
    suspend fun setOfflinePost(v: Boolean)   = context.settingsDataStore.edit { it[KEY_OFFLINE_POST]  = v }
    suspend fun setAntiBan(v: Boolean)          = context.settingsDataStore.edit { it[KEY_ANTI_BAN]         = v }
    suspend fun setBypassActivity(v: Boolean)   = context.settingsDataStore.edit { it[KEY_BYPASS_ACTIVITY]  = v }
    suspend fun setBypassLinks(v: Boolean)      = context.settingsDataStore.edit { it[KEY_BYPASS_LINKS]     = v }
}

enum class DeviceProfile(val label: String, val ua: String) {
    KATE(
        "Kate Mobile",
        "KateMobileAndroid/56 lite-460 (Android 4.4.2; SDK 19; x86; unknown Android SDK built for x86; en)"
    ),
    ANDROID(
        "VK Android",
        "VKAndroidApp/7.43-15079 (Android 12; SDK 31; arm64-v8a; Samsung Galaxy S21; ru; 2960x1440)"
    ),
    IPHONE(
        "VK iPhone",
        "com.vk.vkclient/2316 CFNetwork/1240.0.4 Darwin/20.6.0"
    ),
    WINDOWS(
        "VK Windows",
        "VKDesktopApp/6.8.0 (Windows; 10; 19042)"
    )
}

enum class TypeStatus(val value: String, val label: String, val emoji: String) {
    NONE("none", "Отключено", "❌"),
    TYPING("typing", "Печатает...", "⌨️"),
    AUDIO("audiomessage", "Записывает голосовое", "🎤"),
    VIDEO("videomessage", "Записывает видео", "📹"),
    PHOTO("photo", "Загружает фото", "📸"),
    FILE("file", "Отправляет файл", "📎")
}
