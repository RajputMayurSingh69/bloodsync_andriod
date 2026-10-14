package com.bloodsync.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =======================================================================
// BLOODSYNC MEDICAL THEME (LIGHT & DARK DYNAMIC)
// Strict discipline: Obsidian charcoal & glowing crimson in Dark mode,
// soft warm white & friendly red in Light mode. Edge-to-edge system bars.
// =======================================================================

private val LightColorScheme = lightColorScheme(
    primary             = BloodRedPrimary,
    onPrimary           = Color.White,
    primaryContainer    = BloodRedLight,
    onPrimaryContainer  = BloodRedDark,
    secondary           = BloodRedSecondary,
    onSecondary         = Color.White,
    secondaryContainer  = BloodRedContainer,
    onSecondaryContainer = BloodRedDark,
    tertiary            = StatusWarningAmber,
    onTertiary          = Color.White,
    tertiaryContainer   = StatusWarningAmberLight,
    onTertiaryContainer = CertificateGoldDark,
    background          = MedicalBackground,
    onBackground        = MedicalTextPrimary,
    surface             = MedicalSurface,
    onSurface           = MedicalTextPrimary,
    surfaceVariant      = MedicalSurfaceVariant,
    onSurfaceVariant    = MedicalTextSecondary,
    outline             = MedicalBorder,
    outlineVariant      = MedicalDivider,
    error               = StatusUrgentRed,
    onError             = Color.White,
    errorContainer      = StatusUrgentRedLight,
    onErrorContainer    = BloodRedDark
)

private val DarkColorScheme = darkColorScheme(
    primary             = Color(0xFFEF5350),
    onPrimary           = Color.White,
    primaryContainer    = DarkBloodRedLight,
    onPrimaryContainer  = Color(0xFFFF8A80),
    secondary           = Color(0xFFE53935),
    onSecondary         = Color.White,
    secondaryContainer  = DarkBloodRedContainer,
    onSecondaryContainer = Color(0xFFFF8A80),
    tertiary            = Color(0xFFFF9500),
    onTertiary          = Color.Black,
    tertiaryContainer   = DarkStatusWarningAmberLight,
    onTertiaryContainer = Color(0xFFFFCC80),
    background          = DarkMedicalBackground,
    onBackground        = DarkMedicalTextPrimary,
    surface             = DarkMedicalSurface,
    onSurface           = DarkMedicalTextPrimary,
    surfaceVariant      = DarkMedicalSurfaceVariant,
    onSurfaceVariant    = DarkMedicalTextSecondary,
    outline             = DarkMedicalBorder,
    outlineVariant      = DarkMedicalDivider,
    error               = Color(0xFFEF5350),
    onError             = Color.White,
    errorContainer      = DarkBloodRedLight,
    onErrorContainer    = Color(0xFFFF8A80)
)

@Composable
fun Bloodsync_androidTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Full edge-to-edge: transparent status & nav bars
            window.statusBarColor     = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val insetsController = WindowCompat.getInsetsController(window, view)
            // If darkTheme is active, system bar icons are light (white); if lightTheme, icons are dark
            insetsController.isAppearanceLightStatusBars     = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val bloodSyncColors = if (darkTheme) DarkBloodSyncColors else LightBloodSyncColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalBloodSyncColors provides bloodSyncColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}