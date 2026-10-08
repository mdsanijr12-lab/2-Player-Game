package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppLanguage
import com.example.model.GraphicsQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettingsState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val musicVolume: Float = 0.65f,
    val sfxVolume: Float = 0.85f,
    val isMuted: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val fullscreenEnabled: Boolean = true,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val controlSensitivity: Float = 1.0f,
    val favoriteGameIds: Set<Int> = emptySet()
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("sani_4player_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadFromPrefs())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    private fun loadFromPrefs(): AppSettingsState {
        val langCode = prefs.getString("language", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        val lang = if (langCode == AppLanguage.BANGLA.code) AppLanguage.BANGLA else AppLanguage.ENGLISH
        val musicVol = prefs.getFloat("music_volume", 0.65f).coerceIn(0f, 1f)
        val sfxVol = prefs.getFloat("sfx_volume", 0.85f).coerceIn(0f, 1f)
        val muted = prefs.getBoolean("is_muted", false)
        val vibrate = prefs.getBoolean("vibration", true)
        val fullscreen = prefs.getBoolean("fullscreen", true)
        val qualityOrd = prefs.getInt("graphics_quality", GraphicsQuality.HIGH.ordinal)
        val quality = GraphicsQuality.entries.getOrElse(qualityOrd) { GraphicsQuality.HIGH }
        val sensitivity = prefs.getFloat("control_sensitivity", 1.0f).coerceIn(0.6f, 1.5f)
        val favStrings = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        val favIds = favStrings.mapNotNull { it.toIntOrNull() }.toSet()

        return AppSettingsState(
            language = lang,
            musicVolume = musicVol,
            sfxVolume = sfxVol,
            isMuted = muted,
            vibrationEnabled = vibrate,
            fullscreenEnabled = fullscreen,
            graphicsQuality = quality,
            controlSensitivity = sensitivity,
            favoriteGameIds = favIds
        )
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("language", lang.code).apply()
        _state.value = _state.value.copy(language = lang)
    }

    fun toggleLanguage() {
        val next = if (_state.value.language == AppLanguage.ENGLISH) AppLanguage.BANGLA else AppLanguage.ENGLISH
        setLanguage(next)
    }

    fun setMusicVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat("music_volume", v).apply()
        _state.value = _state.value.copy(musicVolume = v)
    }

    fun setSfxVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat("sfx_volume", v).apply()
        _state.value = _state.value.copy(sfxVolume = v)
    }

    fun setMuted(muted: Boolean) {
        prefs.edit().putBoolean("is_muted", muted).apply()
        _state.value = _state.value.copy(isMuted = muted)
    }

    fun setVibration(enabled: Boolean) {
        prefs.edit().putBoolean("vibration", enabled).apply()
        _state.value = _state.value.copy(vibrationEnabled = enabled)
    }

    fun setFullscreen(enabled: Boolean) {
        prefs.edit().putBoolean("fullscreen", enabled).apply()
        _state.value = _state.value.copy(fullscreenEnabled = enabled)
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        prefs.edit().putInt("graphics_quality", quality.ordinal).apply()
        _state.value = _state.value.copy(graphicsQuality = quality)
    }

    fun setControlSensitivity(sensitivity: Float) {
        val s = sensitivity.coerceIn(0.6f, 1.5f)
        prefs.edit().putFloat("control_sensitivity", s).apply()
        _state.value = _state.value.copy(controlSensitivity = s)
    }

    fun toggleFavorite(gameId: Int) {
        val current = _state.value.favoriteGameIds.toMutableSet()
        if (!current.add(gameId)) {
            current.remove(gameId)
        }
        prefs.edit().putStringSet("favorites", current.map { it.toString() }.toSet()).apply()
        _state.value = _state.value.copy(favoriteGameIds = current)
    }

    fun resetFavorites() {
        prefs.edit().remove("favorites").apply()
        _state.value = _state.value.copy(favoriteGameIds = emptySet())
    }

    fun resetSettings() {
        val favs = _state.value.favoriteGameIds
        prefs.edit()
            .putString("language", AppLanguage.ENGLISH.code)
            .putFloat("music_volume", 0.65f)
            .putFloat("sfx_volume", 0.85f)
            .putBoolean("is_muted", false)
            .putBoolean("vibration", true)
            .putBoolean("fullscreen", true)
            .putInt("graphics_quality", GraphicsQuality.HIGH.ordinal)
            .putFloat("control_sensitivity", 1.0f)
            .apply()
        _state.value = AppSettingsState(favoriteGameIds = favs)
    }
}
