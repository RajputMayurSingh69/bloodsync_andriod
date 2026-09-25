package com.example.bloodsync_android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =======================================================================
// STRICT 4-COLOR PALETTE: RED, GREEN, WHITE, YELLOW ONLY
// Optimized for emergency readability, high contrast, and zero confusion.
// =======================================================================

// 1. RED (Emergency SOS, blood groups, primary actions, urgent alerts)
val BloodRedPrimary = Color(0xFFD32F2F)
val BloodRedDark = Color(0xFFB71C1C)
val BloodRedSecondary = Color(0xFFC62828)
val BloodRedLight = Color(0xFFFFEBEE)
val BloodRedContainer = Color(0xFFFFCDD2)
val BloodCrimson = Color(0xFFB71C1C)
val StatusUrgentRed = Color(0xFFD32F2F)
val StatusUrgentRedLight = Color(0xFFFFEBEE)

// 2. GREEN (Eligible to donate, available, call/WhatsApp, success, verified)
val StatusEligibleGreen = Color(0xFF2E7D32)
val StatusEligibleGreenLight = Color(0xFFE8F5E9)

// 3. WHITE (Clean, sterile, high contrast surfaces & cards)
val MedicalWhite = Color(0xFFFFFFFF)
val MedicalBackground = Color(0xFFFFFFFF)
val MedicalSurface = Color(0xFFFFFFFF)
val MedicalSurfaceVariant = Color(0xFFF8F9FA)
val MedicalBorder = Color(0xFFE0E0E0)
val MedicalDivider = Color(0xFFEEEEEE)

// 4. YELLOW (Waiting, cooldown period, warning alerts, milestones)
val StatusWarningAmber = Color(0xFFF57F17)
val StatusWarningAmberLight = Color(0xFFFFFDE7)
val AlertYellow = Color(0xFFFBC02D)
val AlertYellowLight = Color(0xFFFFFDE7)
val CertificateGold = Color(0xFFF57F17)
val CertificateGoldDark = Color(0xFFF57F17)
val CertificateSealGold = Color(0xFFFBC02D)
val AlertYellowDark = Color(0xFFF57F17)

// No Blue: Map info states to Yellow or Green for strict palette adherence
val StatusInfoBlue = Color(0xFFF57F17)
val StatusInfoBlueLight = Color(0xFFFFFDE7)

// Text Colors (High Contrast Black & Charcoal on White)
val MedicalTextPrimary = Color(0xFF1E1E1E)   // Pure high-contrast dark text
val MedicalTextSecondary = Color(0xFF424242) // Crisp secondary text
val MedicalTextMuted = Color(0xFF757575)     // Clear muted text

/**
 * Adaptive Design Tokens for Light & Dark Mode (Strict Red, Green, White, Yellow)
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
    val badgeBackground: Color,
    val greenLight: Color,
    val yellowLight: Color,
    val redLight: Color
)

val LightBloodSyncColors = BloodSyncColors(
    isDark = false,
    primary = BloodRedPrimary,
    primaryLight = BloodRedLight,
    primaryDark = BloodRedDark,
    background = Color(0xFFF9FAFB),
    surface = Color(0xFFFFFFFF),
    cardBackground = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF8F9FA),
    border = Color(0xFFE5E7EB),
    divider = Color(0xFFEEEEEE),
    textPrimary = Color(0xFF1E1E1E),
    textSecondary = Color(0xFF424242),
    textMuted = Color(0xFF757575),
    topBarBackground = Color(0xFFFFFFFF),
    bottomNavBackground = Color(0xFFFFFFFF),
    inputBackground = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFFF8F9FA),
    greenLight = StatusEligibleGreenLight,
    yellowLight = StatusWarningAmberLight,
    redLight = BloodRedLight
)

val DarkBloodSyncColors = BloodSyncColors(
    isDark = true,
    primary = Color(0xFFEF4444),
    primaryLight = Color(0xFF3B1215),
    primaryDark = Color(0xFFB71C1C),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    cardBackground = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    border = Color(0xFF334155),
    divider = Color(0xFF334155),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    topBarBackground = Color(0xFF1E293B),
    bottomNavBackground = Color(0xFF1E293B),
    inputBackground = Color(0xFF0F172A),
    badgeBackground = Color(0xFF334155),
    greenLight = Color(0xFF14301E),
    yellowLight = Color(0xFF33290D),
    redLight = Color(0xFF3B1215)
)

val LocalBloodSyncColors = staticCompositionLocalOf { LightBloodSyncColors }

object BloodSyncTheme {
    val colors: BloodSyncColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBloodSyncColors.current
}