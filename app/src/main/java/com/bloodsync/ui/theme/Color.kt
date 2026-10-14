package com.bloodsync.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =======================================================================
// BLOODSYNC SOFT MEDICAL RED & WHITE PALETTE (LIGHT ONLY)
// Clean, calm, trustworthy, hospital-grade but friendly & modern.
// Strict discipline: Red, Green, Amber, White. No dark/black backgrounds.
// =======================================================================

// ── PRIMARY RED (CTA, SOS, Blood Groups, Hero Accents) ──────────────────
val BloodRedPrimary         = Color(0xFFE53935)   // Vibrant soft medical red
val BloodRedDark            = Color(0xFFC62828)   // Deep crimson (titles, borders)
val BloodRedSecondary       = Color(0xFFD32F2F)   // Secondary CTA gradient end
val BloodRedLight           = Color(0xFFFFEBEE)   // Soft red container
val BloodRedContainer       = Color(0xFFFFCDD2)   // Slightly deeper tint
val BloodRedSoftTint        = Color(0xFFFFF1F2)   // Ultra soft pink tint
val BloodRedUltraLight      = Color(0xFFFFE4E6)   // Rose tint for chips/cards
val BloodCrimson            = Color(0xFFC62828)
val StatusUrgentRed         = Color(0xFFE53935)
val StatusUrgentRedLight    = Color(0xFFFFEBEE)

// Primary Red Gradient for CTAs, FAB, and Hero Banners
val BloodRedGradientStart   = Color(0xFFEF5350)
val BloodRedGradientEnd     = Color(0xFFD32F2F)

val BloodRedGradientBrush   = Brush.horizontalGradient(
    colors = listOf(BloodRedGradientStart, BloodRedGradientEnd)
)

val BloodRedVerticalGradient = Brush.verticalGradient(
    colors = listOf(BloodRedGradientStart, BloodRedGradientEnd)
)

// ── GREEN (Eligible, Available, Verified, Success) ───────────────────────
val StatusEligibleGreen      = Color(0xFF2E9E5B)   // Calm medical success green
val StatusEligibleGreenLight = Color(0xFFE8F7EE)   // Soft green container
val SuccessGreen             = StatusEligibleGreen
val SuccessGreenLight        = StatusEligibleGreenLight

// ── YELLOW / AMBER (Waiting, Cooldown, Warnings, Milestones) ─────────────
val StatusWarningAmber      = Color(0xFFE65100)   // Deep readable amber
val StatusWarningAmberLight = Color(0xFFFFF3E0)   // Soft warm amber tint
val AlertYellow             = Color(0xFFE65100)
val AlertYellowLight        = Color(0xFFFFF3E0)
val AlertYellowDark         = Color(0xFFBF360C)
val CertificateGold         = Color(0xFFF57F17)
val CertificateGoldDark     = Color(0xFFE65100)
val CertificateSealGold     = Color(0xFFFBC02D)

// Strict 4-color palette: Blue info states mapped to amber/red
val StatusInfoBlue          = Color(0xFFE65100)
val StatusInfoBlueLight     = Color(0xFFFFF3E0)

// ── SURFACES & BACKGROUNDS (Soft warm white, no pure black) ──────────────
val MedicalWhite            = Color(0xFFFFFFFF)
val MedicalBackground       = Color(0xFFFFF8F8)   // Soft warm white
val MedicalBackgroundEnd    = Color(0xFFFFF1F2)   // Very soft pink gradient end
val MedicalSurface          = Color(0xFFFFFFFF)
val MedicalSurfaceVariant   = Color(0xFFFFF5F5)   // Light pinkish neutral
val MedicalBorder           = Color(0xFFF3D9DB)   // Soft pink border stroke (0.5 - 1dp)
val MedicalDivider          = Color(0xFFF7E6E7)   // Ultra soft divider

// Soft Red-Tinted Shadow (spotColor at ~12% alpha)
val SoftRedCardShadow       = Color(0x1FE53935)

val MedicalTextPrimary      = Color(0xFF1F2937)   // Soft charcoal, ultra readable
val MedicalTextSecondary    = Color(0xFF5B6472)   // Friendly slate grey
val MedicalTextMuted        = Color(0xFF9AA1AE)   // Soft muted grey

// Convenient aliases for design system
val SoftBorderPink          = MedicalBorder
val WarmLightSurface        = MedicalSurfaceVariant
val TextSecondaryGrey       = MedicalTextSecondary
val TextPrimaryDark         = MedicalTextPrimary
val TextMutedGrey           = MedicalTextMuted

// ── DARK THEME COLOR TOKENS (Premium Obsidian Charcoal & Glowing Crimson) ──
val DarkMedicalBackground        = Color(0xFF121214)   // Obsidian warm dark
val DarkMedicalBackgroundEnd     = Color(0xFF18181C)
val DarkMedicalSurface           = Color(0xFF1C1C21)   // Elevated card surface
val DarkMedicalSurfaceVariant    = Color(0xFF26262D)   // Secondary dark surface
val DarkMedicalBorder            = Color(0xFF332729)   // Dark rose-tinted border (0.5 - 1dp)
val DarkMedicalDivider           = Color(0xFF28282F)
val DarkSoftRedCardShadow        = Color(0x40000000)

val DarkMedicalTextPrimary       = Color(0xFFF9FAFB)   // Crisp soft white
val DarkMedicalTextSecondary     = Color(0xFFA1A1AA)   // Neutral slate grey
val DarkMedicalTextMuted         = Color(0xFF71717A)   // Muted grey

val DarkBloodRedLight            = Color(0xFF351719)   // Dark red container
val DarkBloodRedContainer        = Color(0xFF45191C)
val DarkBloodRedSoftTint         = Color(0xFF2B1416)
val DarkStatusEligibleGreenLight = Color(0xFF132E1F)   // Dark green container
val DarkStatusWarningAmberLight  = Color(0xFF352008)   // Dark amber container

object BrushGradients {
    val PrimaryPill = BloodRedGradientBrush
    val SuccessPill = Brush.horizontalGradient(listOf(Color(0xFF34C759), Color(0xFF2E9E5B)))
    val WarningPill = Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFE65100)))
}

// =======================================================================
// DESIGN TOKENS — BloodSyncColors
// =======================================================================
data class BloodSyncColors(
    val isDark: Boolean = false,
    val primary: Color = BloodRedPrimary,
    val primaryLight: Color = BloodRedLight,
    val primaryDark: Color = BloodRedDark,
    val background: Color = MedicalBackground,
    val surface: Color = MedicalSurface,
    val cardBackground: Color = MedicalSurface,
    val surfaceVariant: Color = MedicalSurfaceVariant,
    val border: Color = MedicalBorder,
    val divider: Color = MedicalDivider,
    val textPrimary: Color = MedicalTextPrimary,
    val textSecondary: Color = MedicalTextSecondary,
    val textMuted: Color = MedicalTextMuted,
    val topBarBackground: Color = MedicalSurface,
    val bottomNavBackground: Color = MedicalSurface,
    val inputBackground: Color = Color(0xFFFFFAFA),
    val badgeBackground: Color = BloodRedLight,
    val greenLight: Color = StatusEligibleGreenLight,
    val yellowLight: Color = StatusWarningAmberLight,
    val redLight: Color = BloodRedLight,
    val cardShadow: Color = SoftRedCardShadow,
    val shimmerBase: Color = Color(0xFFFDE8E9),
    val shimmerHighlight: Color = Color(0xFFFFFFFF),
    val primaryGradientStart: Color = BloodRedGradientStart,
    val primaryGradientEnd: Color = BloodRedGradientEnd,
    val backgroundGradientEnd: Color = MedicalBackgroundEnd,
    val successGreen: Color = StatusEligibleGreen,
    val successGreenLight: Color = StatusEligibleGreenLight,
    val warningAmber: Color = StatusWarningAmber,
    val warningAmberLight: Color = StatusWarningAmberLight,
    val primarySoftTint: Color = BloodRedSoftTint
)

// Light theme colors
val LightBloodSyncColors = BloodSyncColors()

// Dark theme colors (Obsidian charcoal, glowing crimson, and soft white text)
val DarkBloodSyncColors = BloodSyncColors(
    isDark = true,
    primary = Color(0xFFEF5350),
    primaryLight = DarkBloodRedLight,
    primaryDark = Color(0xFFFF8A80),
    background = DarkMedicalBackground,
    surface = DarkMedicalSurface,
    cardBackground = DarkMedicalSurface,
    surfaceVariant = DarkMedicalSurfaceVariant,
    border = DarkMedicalBorder,
    divider = DarkMedicalDivider,
    textPrimary = DarkMedicalTextPrimary,
    textSecondary = DarkMedicalTextSecondary,
    textMuted = DarkMedicalTextMuted,
    topBarBackground = DarkMedicalSurface,
    bottomNavBackground = DarkMedicalSurface,
    inputBackground = DarkMedicalSurfaceVariant,
    badgeBackground = DarkBloodRedLight,
    greenLight = DarkStatusEligibleGreenLight,
    yellowLight = DarkStatusWarningAmberLight,
    redLight = DarkBloodRedLight,
    cardShadow = DarkSoftRedCardShadow,
    shimmerBase = Color(0xFF242429),
    shimmerHighlight = Color(0xFF383842),
    primaryGradientStart = Color(0xFFEF5350),
    primaryGradientEnd = Color(0xFFE53935),
    backgroundGradientEnd = DarkMedicalBackgroundEnd,
    successGreen = Color(0xFF34C759),
    successGreenLight = DarkStatusEligibleGreenLight,
    warningAmber = Color(0xFFFF9500),
    warningAmberLight = DarkStatusWarningAmberLight,
    primarySoftTint = DarkBloodRedSoftTint
)

val LocalBloodSyncColors = staticCompositionLocalOf { LightBloodSyncColors }

object BloodSyncTheme {
    val colors: BloodSyncColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBloodSyncColors.current
}