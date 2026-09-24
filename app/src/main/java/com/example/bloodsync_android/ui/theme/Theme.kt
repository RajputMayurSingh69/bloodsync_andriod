package com.example.bloodsync_android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BloodRedPrimary,
    onPrimary = Color.White,
    primaryContainer = BloodRedLight,
    onPrimaryContainer = BloodRedDark,
    secondary = BloodRedSecondary,
    onSecondary = Color.White,
    secondaryContainer = BloodRedContainer,
    onSecondaryContainer = BloodRedDark,
    tertiary = StatusInfoBlue,
    onTertiary = Color.White,
    background = Color(0xFFF9FAFB),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFE5E7EB),
    error = StatusUrgentRed,
    onError = Color.White,
    errorContainer = StatusUrgentRedLight,
    onErrorContainer = BloodRedDark
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEF4444),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1215),
    onPrimaryContainer = Color(0xFFFECACA),
    secondary = Color(0xFFF87171),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF450A0A),
    onSecondaryContainer = Color(0xFFFECACA),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color.White,
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF161E2E),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF222F45),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2B3A52),
    error = Color(0xFFF87171),
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun Bloodsync_androidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkBloodSyncColors else LightBloodSyncColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Transparent status & navigation bars for seamless edge-to-edge drawing
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val insetsController = WindowCompat.getInsetsController(window, view)
            // In light mode: dark icons (battery, clock, wifi, nav buttons)
            // In dark mode: white icons
            // This prevents status bar icons from becoming white/invisible on white backgrounds!
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalBloodSyncColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}