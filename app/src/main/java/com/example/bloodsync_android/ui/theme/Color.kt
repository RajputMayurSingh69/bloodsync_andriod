package com.example.bloodsync_android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary BloodSync Red Palette
val BloodRedPrimary = Color(0xFFD32F2F)      // Primary Healthcare Red
val BloodRedDark = Color(0xFFB71C1C)         // Dark Crimson
val BloodRedSecondary = Color(0xFFC62828)    // Secondary Red
val BloodRedLight = Color(0xFFFFEBEE)        // Soft Crimson Tint
val BloodRedContainer = Color(0xFFFFCDD2)    // Light Red Container
val BloodCrimson = Color(0xFF8B0000)         // Deep Certificate Crimson

// Background and Surface Colors (Light Defaults)
val MedicalWhite = Color(0xFFFFFFFF)
val MedicalBackground = Color(0xFFF9FAFB)     // Clean medical gray-white
val MedicalSurface = Color(0xFFFFFFFF)
val MedicalSurfaceVariant = Color(0xFFF3F4F6)
val MedicalBorder = Color(0xFFE5E7EB)
val MedicalDivider = Color(0xFFEEEEEE)

// Typography Colors
val MedicalTextPrimary = Color(0xFF111827)   // Charcoal Black
val MedicalTextSecondary = Color(0xFF4B5563) // Balanced Neutral Gray
val MedicalTextMuted = Color(0xFF9CA3AF)     // Light Neutral Gray

// Status & Accent Colors
val StatusEligibleGreen = Color(0xFF16A34A)  // Success Green
val StatusEligibleGreenLight = Color(0xFFDCFCE7)
val StatusWarningAmber = Color(0xFFD97706)   // Amber / Countdown
val StatusWarningAmberLight = Color(0xFFFEF3C7)
val StatusUrgentRed = Color(0xFFDC2626)      // Urgent / Emergency
val StatusUrgentRedLight = Color(0xFFFEE2E2)
val StatusInfoBlue = Color(0xFF2563EB)
val StatusInfoBlueLight = Color(0xFFDBEAFE)

// Gold for Certificate
val CertificateGold = Color(0xFFD97706)
val CertificateGoldDark = Color(0xFF92400E)
val CertificateSealGold = Color(0xFFFBBF24)

/**
 * Adaptive Design Tokens for Light & Dark Mode
 */
data class BloodSyncColors(
    val isDark: Boolean,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val surfaceVariant: Color,
    val border: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val topBarBackground: Color,
    val bottomNavBackground: Color,
    val inputBackground: Color,
    val badgeBackground: Color
)

val LightBloodSyncColors = BloodSyncColors(
    isDark = false,
    primary = BloodRedPrimary,
    primaryLight = BloodRedLight,
    primaryDark = BloodRedDark,
    background = Color(0xFFF9FAFB),
    surface = Color(0xFFFFFFFF),
    cardBackground = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3F4F6),
    border = Color(0xFFE5E7EB),
    divider = Color(0xFFEEEEEE),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF9CA3AF),
    topBarBackground = Color(0xFFFFFFFF),
    bottomNavBackground = Color(0xFFFFFFFF),
    inputBackground = Color(0xFFF9FAFB),
    badgeBackground = Color(0xFFF3F4F6)
)

val DarkBloodSyncColors = BloodSyncColors(
    isDark = true,
    primary = Color(0xFFEF4444),          // High contrast bright red for dark surfaces
    primaryLight = Color(0xFF3B1215),     // Deep dark crimson tint
    primaryDark = Color(0xFFDC2626),
    background = Color(0xFF0B0F17),       // Deep slate-black canvas
    surface = Color(0xFF161E2E),          // Elevated surface
    cardBackground = Color(0xFF161E2E),   // Crisp dark card container
    surfaceVariant = Color(0xFF222F45),   // Highlighted chip/element surface
    border = Color(0xFF2B3A52),           // Visible subtle dark border
    divider = Color(0xFF1E293B),
    textPrimary = Color(0xFFF8FAFC),      // Crisp high-contrast white text
    textSecondary = Color(0xFF94A3B8),    // Muted slate text
    textMuted = Color(0xFF64748B),        // Soft slate gray
    topBarBackground = Color(0xFF0F172A), // Seamless dark top app bar
    bottomNavBackground = Color(0xFF161E2E), // Elevated bottom navigation bar
    inputBackground = Color(0xFF161E2E),
    badgeBackground = Color(0xFF222F45)
)

val LocalBloodSyncColors = staticCompositionLocalOf { LightBloodSyncColors }

object BloodSyncTheme {
    val colors: BloodSyncColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBloodSyncColors.current
}