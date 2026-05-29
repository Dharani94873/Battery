package com.example

import android.app.Application
import com.example.data.BatteryDatabase
import com.example.data.BatteryRepository
import com.example.data.BatterySettingsManager

class BatteryApplication : Application() {
    val database by lazy { BatteryDatabase.getDatabase(this) }
    val repository by lazy {
        BatteryRepository(
            database.batteryDao,
            BatterySettingsManager(this)
        )
    }
}
