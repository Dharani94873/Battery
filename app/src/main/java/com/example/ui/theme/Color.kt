package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

// Light Colors (Professional Slate / Off-White / Emerald)
val SoftLightBg = Color(0xFFF8FAFC)       // Cool slate light grey
val PureWhiteSurface = Color(0xFFFFFFFF)  // Clean pristine white
val TextSlatePrimary = Color(0xFF0F172A)  // Dark elegant blue-grey
val TextSlateSecondary = Color(0xFF475569)// Medium gray
val TextSlateMuted = Color(0xFF94A3B8)    // Light slate gray
val DesignOutlineLight = Color(0xFFE2E8F0)// Soft border contour

// Dark Colors (Obsidian / Dark Teal / Cyan)
val SlateDarkBg = Color(0xFF0F172A)       // Deep slate obsidian
val SolidDarkSurface = Color(0xFF1E293B)  // Dark slate card surface
val DesignOutlineDark = Color(0xFF334155)  // Dark border contour

// Dynamic State Accents
val EmeraldGreen = Color(0xFF10B981)      // Green (Optimal / Healthy State)
val EmeraldGlow = Color(0xFF34D399)       // Soft green glow
val ElectricCyan = Color(0xFF3B82F6)      // Sapphire Blue / Cyan (Charging standard)
val WarningAmber = Color(0xFFF59E0B)      // Warm amber (Warning/Wear)
val DangerRose = Color(0xFFEF4444)        // Alert red (Critical limits)

// Compat mappings to retain any legacy references
val TechDarkBg @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.background
val TechCardBg @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.surface
val TechCardBorder @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.outline
val TextPrimaryDark @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
val TextSecondaryDark @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
