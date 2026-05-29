package com.example.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.BatteryApplication
import com.example.MainActivity
import com.example.data.BatteryLogEntity
import com.example.data.BatterySettings
import com.example.data.ChargeSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BatteryMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var highLimitAlertShown = false
    private var lowLimitAlertShown = false
    private var tempAlertShown = false

    // Charging session tracking
    private var isTrackingSession = false
    private var sessionStartTime = 0L
    private var sessionStartPct = 0
    private var sessionPeakTemp = 0f

    private val repository by lazy {
        (application as BatteryApplication).repository
    }

    private var currentSettings = BatterySettings()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                processBatteryUpdate(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification(0, "Initializing...", "Good"))

        // Register dynamic receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Observe settings changes
        scope.launch {
            repository.settings.collect { settings ->
                currentSettings = settings
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Run persistent
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        job.cancel()
    }

    private fun processBatteryUpdate(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val healthStr = getHealthString(healthCode)

        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val temperature = tempRaw / 10.0f // standard conversion to °C

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) // in mV
        val statusCode = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val statusStr = getStatusString(statusCode)

        val pluggedCode = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val isCharging = statusCode == BatteryManager.BATTERY_STATUS_CHARGING || 
                           statusCode == BatteryManager.BATTERY_STATUS_FULL

        // Post status update to UI dynamically via public state flow if requested, but let's write to database
        // so that the UI can observe database logs or write to a global StateFlow that exists on this Service.
        _serviceState.value = ServiceBatteryState(
            percentage = percentage,
            health = healthStr,
            temperature = temperature,
            voltage = voltage,
            status = statusStr,
            isCharging = isCharging,
            pluggedType = getPluggedString(pluggedCode)
        )

        // Update foreground notification display
        updateForegroundNotification(percentage, statusStr, healthStr)

        // Manage active charging session logging
        manageChargeSession(percentage, isCharging, temperature)

        // Evaluate alerts based on settings
        evaluateAlerts(percentage, isCharging, temperature)

        // Log battery logs periodically or on level changes
        logPeriodicEvent(percentage, healthStr, voltage, temperature, statusStr)
    }

    private var lastLoggedPct = -1
    private var lastLoggedTime = 0L

    private fun logPeriodicEvent(percentage: Int, health: String, voltage: Int, temperature: Float, status: String) {
        val now = System.currentTimeMillis()
        // Save to database only on change or at least 5 minutes apart to avoid clogging memory
        if (percentage != lastLoggedPct || now - lastLoggedTime > 5 * 60 * 1000) {
            lastLoggedPct = percentage
            lastLoggedTime = now
            scope.launch {
                repository.insertLog(
                    BatteryLogEntity(
                        timestamp = now,
                        percentage = percentage,
                        health = health,
                        voltage = voltage,
                        temperature = temperature,
                        status = status
                    )
                )
            }
        }
    }

    private fun manageChargeSession(percentage: Int, isCharging: Boolean, temperature: Float) {
        val now = System.currentTimeMillis()
        if (isCharging) {
            if (!isTrackingSession) {
                // Charging started!
                isTrackingSession = true
                sessionStartTime = now
                sessionStartPct = percentage
                sessionPeakTemp = temperature
                // Reset triggered flags for this charging cycle
                highLimitAlertShown = false
            } else {
                // Charging ongoing, track peak temperature
                sessionPeakTemp = maxOf(sessionPeakTemp, temperature)
            }
        } else {
            if (isTrackingSession) {
                // Charging stopped!
                isTrackingSession = false
                val durationMs = now - sessionStartTime
                // Ensure the session was long enough to count (e.g. 5 seconds for testing/utility, normally 30s)
                if (durationMs >= 5000) {
                    val gain = percentage - sessionStartPct
                    if (gain > 0) {
                        // Calculate Lifespan Adherence Score
                        // 100 point baseline
                        var score = 100
                        val targetHigh = currentSettings.highLimitThreshold
                        val targetLow = currentSettings.lowLimitThreshold

                        // Penalty for charging above the optimal battery health threshold (usually 80%)
                        if (percentage > targetHigh) {
                            val overchargedPct = percentage - targetHigh
                            // Penalty scales; worst if they charge right up to 100%
                            score -= (overchargedPct * 2.5f).toInt()
                        }

                        // Penalty for plugging in too late (e.g. starting when battery dropped below 20%)
                        if (sessionStartPct < targetLow) {
                            val deepDischargePct = targetLow - sessionStartPct
                            score -= (deepDischargePct * 1.5f).toInt()
                        }

                        // Penalty for high battery heat during session (temperature over 38°C accelerates wear)
                        if (sessionPeakTemp > 38f) {
                            val heatExcess = sessionPeakTemp - 38f
                            score -= (heatExcess * 3f).toInt()
                        }

                        // Clamp score 10-100
                        val finalScore = score.coerceIn(10, 100)

                        scope.launch {
                            repository.insertSession(
                                ChargeSessionEntity(
                                    timestamp = now,
                                    startPercentage = sessionStartPct,
                                    endPercentage = percentage,
                                    peakTemperature = sessionPeakTemp,
                                    durationMs = durationMs,
                                    lifespanScore = finalScore
                                )
                            )
                        }
                    }
                }
                // Reset low alert when un-plugging
                lowLimitAlertShown = false
            }
        }
    }

    private fun evaluateAlerts(percentage: Int, isCharging: Boolean, temperature: Float) {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. High Limit Optimized Alert (Charging completed to target lifespan cap)
        if (isCharging && currentSettings.enableHighLimitAlert && percentage >= currentSettings.highLimitThreshold) {
            if (!highLimitAlertShown) {
                highLimitAlertShown = true
                val alertNotification = NotificationCompat.Builder(this, CHANNEL_ALERTS_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("🔋 Lifespan Optimized Charge!")
                    .setContentText("Battery reached ${percentage}%. Unplug now to prevent heat and chemical stress!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(getAppPendingIntent())
                    .setAutoCancel(true)
                    .build()
                mNotificationManager.notify(ALERT_HIGH_LIMIT_ID, alertNotification)
            }
        } else if (!isCharging || percentage < currentSettings.highLimitThreshold - 2) {
            // Cool-down hysteresis to reset high limit trigger
            highLimitAlertShown = false
        }

        // 2. Low Limit Stress Warning (Discharging too low)
        if (!isCharging && currentSettings.enableLowLimitAlert && percentage <= currentSettings.lowLimitThreshold) {
            if (!lowLimitAlertShown) {
                lowLimitAlertShown = true
                val alertNotification = NotificationCompat.Builder(this, CHANNEL_ALERTS_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("⚠️ Deep Discharge Warning")
                    .setContentText("Battery level at ${percentage}%. Plug in now to avoid deep chemical wear!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(getAppPendingIntent())
                    .setAutoCancel(true)
                    .build()
                mNotificationManager.notify(ALERT_LOW_LIMIT_ID, alertNotification)
            }
        } else if (isCharging || percentage > currentSettings.lowLimitThreshold + 2) {
            lowLimitAlertShown = false
        }

        // 3. Thermal Degradation Alert (Overheating while in use or charging)
        if (currentSettings.enableTempAlert && temperature >= currentSettings.tempThreshold) {
            if (!tempAlertShown) {
                tempAlertShown = true
                val alertNotification = NotificationCompat.Builder(this, CHANNEL_ALERTS_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle("🔥 High Battery Temperature!")
                    .setContentText("Battery is running hot at ${String.format("%.1f", temperature)}°C! Avoid intense tasks.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(getAppPendingIntent())
                    .setAutoCancel(true)
                    .build()
                mNotificationManager.notify(ALERT_TEMP_ID, alertNotification)
            }
        } else if (temperature < currentSettings.tempThreshold - 2f) {
            tempAlertShown = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Battery Guard Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors real-time battery stats for safety."
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Lifespan Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent warnings for charging lifespan optimization."
                enableVibration(true)
                importance = NotificationManager.IMPORTANCE_HIGH
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    private fun buildForegroundNotification(percentage: Int, status: String, health: String): Notification {
        val message = "Status: $status | Health: $health"
        val title = "Battery Guard Active: $percentage%"

        return NotificationCompat.Builder(this, CHANNEL_SERVICE_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // standard icon
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(getAppPendingIntent())
            .build()
    }

    private fun updateForegroundNotification(percentage: Int, status: String, health: String) {
        val notification = buildForegroundNotification(percentage, status, health)
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getHealthString(healthCode: Int): String {
        return when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Unknown"
        }
    }

    private fun getStatusString(statusCode: Int): String {
        return when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full (Unplug!)"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
    }

    private fun getPluggedString(pluggedCode: Int): String {
        return when (pluggedCode) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Fast Charger"
            else -> "On Battery"
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "BatteryMonitorServiceChannel"
        const val CHANNEL_ALERTS_ID = "BatteryAlertsChannel"
        const val NOTIFICATION_ID = 2026

        const val ALERT_HIGH_LIMIT_ID = 3001
        const val ALERT_LOW_LIMIT_ID = 3002
        const val ALERT_TEMP_ID = 3003

        // Global live status flow for reactive Compose binding when service is active
        private val _serviceState = MutableStateFlow<ServiceBatteryState?>(null)
        val serviceState: StateFlow<ServiceBatteryState?> = _serviceState.asStateFlow()
    }
}

data class ServiceBatteryState(
    val percentage: Int,
    val health: String,
    val temperature: Float,
    val voltage: Int,
    val status: String,
    val isCharging: Boolean,
    val pluggedType: String
)
