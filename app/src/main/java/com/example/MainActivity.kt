package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.service.BatteryMonitorService
import com.example.ui.BatteryDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BatteryViewModel
import com.example.viewmodel.BatteryViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryViewModel by viewModels {
        BatteryViewModelFactory(
            applicationContext,
            (application as BatteryApplication).repository
        )
    }

    // Permission launcher for Android 13+ Notifications
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startBatteryGuardService()
        } else {
            Toast.makeText(
                this,
                "Notification permission is required for battery alerts to work.",
                Toast.LENGTH_LONG
            ).show()
            // Start anyway as background operations are unaffected, but notify user
            startBatteryGuardService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                BatteryDashboardScreen(
                    viewModel = viewModel,
                    onToggleGuard = { shouldRun ->
                        if (shouldRun) {
                            checkAndStartBatteryGuard()
                        } else {
                            stopBatteryGuardService()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun checkAndStartBatteryGuard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                startBatteryGuardService()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startBatteryGuardService()
        }
    }

    private fun startBatteryGuardService() {
        try {
            val intent = Intent(this, BatteryMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            viewModel.setGuardServiceStatus(true)
            Toast.makeText(this, "Guard Protection Activated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start Guard: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopBatteryGuardService() {
        try {
            val intent = Intent(this, BatteryMonitorService::class.java)
            stopService(intent)
            viewModel.setGuardServiceStatus(false)
            Toast.makeText(this, "Guard Protection Deactivated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop Guard", Toast.LENGTH_SHORT).show()
        }
    }
}
