package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<BatteryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BatteryLogEntity)

    @Query("DELETE FROM battery_logs WHERE id NOT IN (SELECT id FROM battery_logs ORDER BY timestamp DESC LIMIT 500)")
    suspend fun prunOldLogs()

    @Query("SELECT * FROM charge_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChargeSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargeSessionEntity)

    @Query("DELETE FROM charge_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM charge_sessions")
    suspend fun clearAllSessions()
}
