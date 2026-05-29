package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatterySettings(
    val enableHighLimitAlert: Boolean = true,
    val enableLowLimitAlert: Boolean = true,
    val enableTempAlert: Boolean = true,
    val highLimitThreshold: Int = 80,
    val lowLimitThreshold: Int = 20,
    val tempThreshold: Float = 40f,
    val calibratedHealthPct: Int = 96, // Default estimate that gets updated via diagnostic scan
    val useFahrenheit: Boolean = false
)

class BatterySettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("battery_optimizer_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<BatterySettings> = _settings.asStateFlow()

    private fun loadSettings(): BatterySettings {
        return BatterySettings(
            enableHighLimitAlert = prefs.getBoolean("enable_high_limit", true),
            enableLowLimitAlert = prefs.getBoolean("enable_low_limit", true),
            enableTempAlert = prefs.getBoolean("enable_temp_alert", true),
            highLimitThreshold = prefs.getInt("high_limit_threshold", 80),
            lowLimitThreshold = prefs.getInt("low_limit_threshold", 20),
            tempThreshold = prefs.getFloat("temp_threshold", 40f),
            calibratedHealthPct = prefs.getInt("calibrated_health_pct", 96),
            useFahrenheit = prefs.getBoolean("use_fahrenheit", false)
        )
    }

    fun updateEnableHighLimitAlert(value: Boolean) {
        prefs.edit().putBoolean("enable_high_limit", value).apply()
        _settings.value = _settings.value.copy(enableHighLimitAlert = value)
    }

    fun updateEnableLowLimitAlert(value: Boolean) {
        prefs.edit().putBoolean("enable_low_limit", value).apply()
        _settings.value = _settings.value.copy(enableLowLimitAlert = value)
    }

    fun updateEnableTempAlert(value: Boolean) {
        prefs.edit().putBoolean("enable_temp_alert", value).apply()
        _settings.value = _settings.value.copy(enableTempAlert = value)
    }

    fun updateHighLimitThreshold(value: Int) {
        prefs.edit().putInt("high_limit_threshold", value).apply()
        _settings.value = _settings.value.copy(highLimitThreshold = value)
    }

    fun updateLowLimitThreshold(value: Int) {
        prefs.edit().putInt("low_limit_threshold", value).apply()
        _settings.value = _settings.value.copy(lowLimitThreshold = value)
    }

    fun updateTempThreshold(value: Float) {
        prefs.edit().putFloat("temp_threshold", value).apply()
        _settings.value = _settings.value.copy(tempThreshold = value)
    }

    fun updateCalibratedHealthPct(value: Int) {
        prefs.edit().putInt("calibrated_health_pct", value).apply()
        _settings.value = _settings.value.copy(calibratedHealthPct = value)
    }

    fun updateUseFahrenheit(value: Boolean) {
        prefs.edit().putBoolean("use_fahrenheit", value).apply()
        _settings.value = _settings.value.copy(useFahrenheit = value)
    }
}
