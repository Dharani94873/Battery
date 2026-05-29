package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BatteryLogEntity
import com.example.data.ChargeSessionEntity
import com.example.data.BatterySettings
import com.example.service.ServiceBatteryState
import com.example.ui.theme.*
import com.example.viewmodel.BatteryViewModel
import com.example.viewmodel.DiagnosticState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatteryDashboardScreen(
    viewModel: BatteryViewModel,
    onToggleGuard: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val batteryState by viewModel.batteryState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isGuardRunning by viewModel.isGuardServiceRunning.collectAsStateWithLifecycle()
    val recentSessions by viewModel.chargeSessions.collectAsStateWithLifecycle()
    val diagnosticState by viewModel.diagnosticState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechDarkBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Battery Guard",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            fontFamily = FontFamily.SansSerif
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isGuardRunning) EmeraldGreen else WarningAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGuardRunning) "Lifespan Guard Active" else "Guard Suspended",
                                fontSize = 12.sp,
                                color = if (isGuardRunning) EmeraldGreen else WarningAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.refreshBatterySnap() },
                        modifier = Modifier
                            .background(TechCardBg, CircleShape)
                            .testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh telemetry",
                            tint = ElectricCyan
                        )
                    }
                }
            }
        },
        containerColor = TechDarkBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sleek Segmented Control Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TechCardBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabs = listOf("Monitor", "Configure", "Calibrate", "Session Log")
                tabs.forEachIndexed { index, title ->
                    val selected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) TechCardBorder else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 10.dp)
                            .testTag("tab_${title.lowercase().replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) TextPrimaryDark else TextSecondaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabContentAnimation",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { targetTab ->
                when (targetTab) {
                    0 -> MonitorTab(
                        batteryState = batteryState,
                        settings = settings,
                        isGuardRunning = isGuardRunning,
                        recentSessions = recentSessions,
                        onToggleGuard = onToggleGuard,
                        onRunDiagnostic = { activeTab = 2 }
                    )
                    1 -> ConfigureTab(
                        settings = settings,
                        viewModel = viewModel
                    )
                    2 -> CalibrateTab(
                        diagnosticState = diagnosticState,
                        calibratedHealthPct = settings.calibratedHealthPct,
                        onStartScan = { viewModel.runComprehensiveDiagnostic() },
                        onResetScan = { viewModel.resetDiagnostic() }
                    )
                    3 -> SessionsTab(
                        sessions = recentSessions,
                        onClearSessions = { viewModel.clearSessions() }
                    )
                }
            }
        }
    }
}

@Composable
fun MonitorTab(
    batteryState: ServiceBatteryState,
    settings: BatterySettings,
    isGuardRunning: Boolean,
    recentSessions: List<ChargeSessionEntity>,
    onToggleGuard: (Boolean) -> Unit,
    onRunDiagnostic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-fidelity Battery Gauge Element
        BatteryGauge(
            percentage = batteryState.percentage,
            isCharging = batteryState.isCharging,
            statusText = batteryState.status,
            optimalHighLimit = settings.highLimitThreshold,
            optimalLowLimit = settings.lowLimitThreshold
        )

        // Lifespan Optimization Advisory Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TechCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (batteryState.percentage in settings.lowLimitThreshold..settings.highLimitThreshold) 
                            Icons.Default.VerifiedUser else Icons.Default.Warning,
                        contentDescription = "Optimization State",
                        tint = if (batteryState.percentage in settings.lowLimitThreshold..settings.highLimitThreshold) 
                            EmeraldGreen else WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (batteryState.percentage in settings.lowLimitThreshold..settings.highLimitThreshold) 
                            "Optimal Range Active" else "Wear Zone Warning",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (batteryState.percentage > settings.highLimitThreshold) {
                        "Your battery is above ${settings.highLimitThreshold}%. Keeping li-ion cells at full voltage stresses chemistry. Unplug to maximize lifespan."
                    } else if (batteryState.percentage < settings.lowLimitThreshold) {
                        "Battery is below ${settings.lowLimitThreshold}%. Deep discharge induces cathode stress. We highly recommend connecting a charger soon."
                    } else {
                        "Battery is currently within the sweet spot (${settings.lowLimitThreshold}% - ${settings.highLimitThreshold}%). This prevents cell fatigue and maximizes duty-cycles!"
                    },
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    lineHeight = 18.sp
                )
            }
        }

        // Live Grid Diagnostics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                title = "Battery Temp",
                value = "${batteryState.temperature}°C",
                icon = Icons.Default.Thermostat,
                color = when {
                    batteryState.temperature >= settings.tempThreshold -> DangerRose
                    batteryState.temperature >= settings.tempThreshold - 5f -> WarningAmber
                    else -> EmeraldGreen
                },
                modifier = Modifier.weight(1f)
            )

            InfoCard(
                title = "Cell Health",
                value = "${settings.calibratedHealthPct}%",
                icon = Icons.Default.OfflineBolt,
                color = if (settings.calibratedHealthPct >= 90) EmeraldGreen else WarningAmber,
                modifier = Modifier.weight(1f),
                onClick = onRunDiagnostic
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                title = "Voltage",
                value = "${batteryState.voltage} mV",
                icon = Icons.Default.Bolt,
                color = ElectricCyan,
                modifier = Modifier.weight(1f)
            )

            InfoCard(
                title = "Input Terminal",
                value = batteryState.pluggedType,
                icon = Icons.Default.Power,
                color = if (batteryState.isCharging) ElectricCyan else TextSecondaryDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Active State Guard Master Switch Card (Pulsing glowing button)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TechCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lifespan Guard Protection",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Allows the system to run continuous background analysis and trigger lifespan warnings.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isGuardRunning,
                    onCheckedChange = { onToggleGuard(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TechDarkBg,
                        checkedTrackColor = EmeraldGreen,
                        uncheckedThumbColor = TextSecondaryDark,
                        uncheckedTrackColor = TechCardBorder
                    ),
                    modifier = Modifier.testTag("guard_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BatteryGauge(
    percentage: Int,
    isCharging: Boolean,
    statusText: String,
    optimalHighLimit: Int,
    optimalLowLimit: Int,
    modifier: Modifier = Modifier
) {
    // Elegant animating arcs
    val animationProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "percentageArc"
    )

    val arcColor = when {
        percentage < optimalLowLimit -> DangerRose
        percentage > optimalHighLimit -> ElectricCyan
        else -> EmeraldGreen
    }

    Box(
        modifier = modifier
            .size(230.dp)
            .shadow(16.dp, CircleShape, spotColor = arcColor, ambientColor = arcColor)
            .background(TechCardBg, CircleShape)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val sizeDim = size.minDimension - strokeWidth
            val radius = sizeDim / 2
            val centerOffset = center

            // 1. Draw back track circle
            drawCircle(
                color = TechCardBorder,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeWidth - 4.dp.toPx())
            )

            // 2. Draw colorful arc representing battery level value
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * animationProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner percentage display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$percentage",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryDark,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            
            // Glowing charging status pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(arcColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isCharging) {
                    val infiniteTransition = rememberInfiniteTransition(label = "boltPulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Charging bolt",
                        tint = arcColor.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PowerOff,
                        contentDescription = "On battery icon",
                        tint = arcColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = statusText.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = arcColor,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = TechCardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TechCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryDark
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }
    }
}

@Composable
fun ConfigureTab(
    settings: BatterySettings,
    viewModel: BatteryViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Lifespan Optimizer Rules",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )

        // Rule 1: High Charge Cap
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TechCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "High Charge Cut-off Alert",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Alerts to unplug immediately when battery charge hits target limit to safeguard cathode structure.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = settings.enableHighLimitAlert,
                        onCheckedChange = { viewModel.toggleHighLimitAlert(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen),
                        modifier = Modifier.testTag("toggle_high_alert")
                    )
                }

                if (settings.enableHighLimitAlert) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Charge Limit: ${settings.highLimitThreshold}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (settings.highLimitThreshold <= 80) "Max Protection (Recommended)" else "Standard Wear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (settings.highLimitThreshold <= 80) EmeraldGreen else WarningAmber
                        )
                    }
                    Slider(
                        value = settings.highLimitThreshold.toFloat(),
                        onValueChange = { viewModel.setHighLimitThreshold(it.roundToInt()) },
                        valueRange = 70f..95f,
                        steps = 4, // 5 intervals (70, 75, 80, 85, 90, 95)
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldGreen,
                            activeTrackColor = EmeraldGreen,
                            inactiveTrackColor = TechCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("high_limit_slider")
                    )
                }
            }
        }

        // Rule 2: Low Charge Warning
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TechCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Low Battery Safety Warning",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Notifies to connect charger before voltage drops too low, preventing extreme anode depletion stresses.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = settings.enableLowLimitAlert,
                        onCheckedChange = { viewModel.toggleLowLimitAlert(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen),
                        modifier = Modifier.testTag("toggle_low_alert")
                    )
                }

                if (settings.enableLowLimitAlert) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Threshold Limit: ${settings.lowLimitThreshold}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark
                    )
                    Slider(
                        value = settings.lowLimitThreshold.toFloat(),
                        onValueChange = { viewModel.setLowLimitThreshold(it.roundToInt()) },
                        valueRange = 15f..35f,
                        steps = 3, // (15, 20, 25, 30, 35)
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldGreen,
                            activeTrackColor = EmeraldGreen,
                            inactiveTrackColor = TechCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("low_limit_slider")
                    )
                }
            }
        }

        // Rule 3: Thermal Alert Rule
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TechCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Thermal Degradation Alert",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Overheating speeds up permanent capacity decay. Alerts you when battery climbs beyond critical degrees.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = settings.enableTempAlert,
                        onCheckedChange = { viewModel.toggleTempAlert(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen),
                        modifier = Modifier.testTag("toggle_temp_alert")
                    )
                }

                if (settings.enableTempAlert) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Temperature Limit: ${settings.tempThreshold}°C",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark
                    )
                    Slider(
                        value = settings.tempThreshold,
                        onValueChange = { viewModel.setTempThreshold(it) },
                        valueRange = 35f..48f,
                        steps = 12,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldGreen,
                            activeTrackColor = EmeraldGreen,
                            inactiveTrackColor = TechCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("temp_limit_slider")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun CalibrateTab(
    diagnosticState: DiagnosticState,
    calibratedHealthPct: Int,
    onStartScan: () -> Unit,
    onResetScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Battery Health Calibration",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Standard Android APIs do not provide exact lithium-ion degradation percentages dynamically. Run this interactive, 4-step diagnostic suite to measure physical state standard-deviations.",
            fontSize = 12.sp,
            color = TextSecondaryDark,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        when (diagnosticState) {
            is DiagnosticState.Idle -> {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(TechCardBg)
                        .border(1.dp, TechCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Diag ready",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(68.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStartScan,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_scan_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Power, contentDescription = null, tint = TechDarkBg)
                        Text(
                            text = "RUN DIAGNOSTICS SUITE",
                            fontWeight = FontWeight.Bold,
                            color = TechDarkBg,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            is DiagnosticState.Scanning -> {
                // Animated glowing loading ring
                val infiniteTransition = rememberInfiniteTransition(label = "ringSpin")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "spinAngle"
                )

                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = TechCardBorder,
                            style = Stroke(width = 6.dp.toPx())
                        )
                        drawArc(
                            color = EmeraldGreen,
                            startAngle = rotation,
                            sweepAngle = 100f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(diagnosticState.progress * 100).roundToInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Text(
                    text = diagnosticState.stepName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    textAlign = TextAlign.Center
                )

                LinearProgressIndicator(
                    progress = { diagnosticState.progress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldGreen,
                    trackColor = TechCardBorder,
                )
            }

            is DiagnosticState.Finished -> {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(TechCardBg)
                        .border(3.dp, EmeraldGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${diagnosticState.healthPct}%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "CALIBRATED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Text(
                    text = "Diagnostics Completed successfully!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text(
                    text = "Calibrated Battery health computed at ${diagnosticState.healthPct}%. The battery exhibits low chemical voltage variance and healthy thermal absorption under standard loads. Keep practicing the 20-80% rule to prolong this!",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetScan,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                        border = BorderStroke(1.dp, TechCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reset_scan_button")
                    ) {
                        Text("RE-TEST")
                    }
                }
            }
        }
    }
}

@Composable
fun SessionsTab(
    sessions: List<ChargeSessionEntity>,
    onClearSessions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lifespan Charging Log",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "${sessions.size} recorded charge sessions",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
            if (sessions.isNotEmpty()) {
                TextButton(
                    onClick = onClearSessions,
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRose),
                    modifier = Modifier.testTag("clear_sessions_button")
                ) {
                    Text("Clear Log")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TechCardBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "Empty sessions",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No charging sessions recorded yet.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Plug in your device with 'Lifespan Guard protection' active to log charging scores based on health practices.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        } else {
            // Aggregate score display
            val averageScore = sessions.map { it.lifespanScore }.average().roundToInt()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                colors = CardDefaults.cardColors(containerColor = TechCardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TechCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Average Lifespan Score",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "$averageScore%",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (averageScore >= 85) EmeraldGreen else if (averageScore >= 60) WarningAmber else DangerRose
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Lifespan Efficiency Rating",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = if (averageScore >= 85) "A+ Chem Protecting" else if (averageScore >= 70) "Good Practice" else "Needs Improvement",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (averageScore >= 85) EmeraldGreen else if (averageScore >= 70) WarningAmber else DangerRose
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionItem(session = session)
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: ChargeSessionEntity) {
    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val displayDate = remember(session.timestamp) { formatter.format(Date(session.timestamp)) }
    val durationMinutes = remember(session.durationMs) { (session.durationMs / 60000).coerceAtLeast(1) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_item_${session.id}"),
        colors = CardDefaults.cardColors(containerColor = TechCardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TechCardBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = displayDate,
                    fontSize = 13.sp,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${session.startPercentage}% ➡️ ${session.endPercentage}%",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = TechCardBorder
                    )
                    Text(
                        text = "$durationMinutes mins",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Peak Thermal: ${String.format("%.1f", session.peakTemperature)}°C",
                    fontSize = 11.sp,
                    color = if (session.peakTemperature >= 38f) WarningAmber else TextSecondaryDark
                )
            }

            // Beautiful mini circular score container
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            session.lifespanScore >= 85 -> EmeraldGreen.copy(alpha = 0.15f)
                            session.lifespanScore >= 60 -> WarningAmber.copy(alpha = 0.15f)
                            else -> DangerRose.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${session.lifespanScore}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        session.lifespanScore >= 85 -> EmeraldGreen
                        session.lifespanScore >= 60 -> WarningAmber
                        else -> DangerRose
                    }
                )
            }
        }
    }
}
