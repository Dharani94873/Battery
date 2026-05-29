package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_logs")
data class BatteryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val percentage: Int,
    val health: String,
    val voltage: Int, // in mV
    val temperature: Float, // in °C
    val status: String
)

@Entity(tableName = "charge_sessions")
data class ChargeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val startPercentage: Int,
    val endPercentage: Int,
    val peakTemperature: Float,
    val durationMs: Long,
    val lifespanScore: Int // 0-100% adherence score
)
