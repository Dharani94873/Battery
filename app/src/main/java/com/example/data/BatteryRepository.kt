package com.example.data

import kotlinx.coroutines.flow.Flow

class BatteryRepository(
    private val batteryDao: BatteryDao,
    val settingsManager: BatterySettingsManager
) {
    val recentLogs: Flow<List<BatteryLogEntity>> = batteryDao.getRecentLogs()
    val allSessions: Flow<List<ChargeSessionEntity>> = batteryDao.getAllSessions()
    val settings: Flow<BatterySettings> = settingsManager.settings

    suspend fun insertLog(log: BatteryLogEntity) {
        batteryDao.insertLog(log)
        batteryDao.prunOldLogs()
    }

    suspend fun insertSession(session: ChargeSessionEntity) {
        batteryDao.insertSession(session)
    }

    suspend fun deleteSession(id: Long) {
        batteryDao.deleteSessionById(id)
    }

    suspend fun clearSessions() {
        batteryDao.clearAllSessions()
    }

    fun setHighLimitEnabled(enabled: Boolean) = settingsManager.updateEnableHighLimitAlert(enabled)
    fun setLowLimitEnabled(enabled: Boolean) = settingsManager.updateEnableLowLimitAlert(enabled)
    fun setTempAlertEnabled(enabled: Boolean) = settingsManager.updateEnableTempAlert(enabled)
    fun setHighLimit(limit: Int) = settingsManager.updateHighLimitThreshold(limit)
    fun setLowLimit(limit: Int) = settingsManager.updateLowLimitThreshold(limit)
    fun setTempThreshold(temp: Float) = settingsManager.updateTempThreshold(temp)
    fun setCalibratedHealth(pct: Int) = settingsManager.updateCalibratedHealthPct(pct)
    fun setUseFahrenheit(enabled: Boolean) = settingsManager.updateUseFahrenheit(enabled)
}
