package com.example.bloodsync_android

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.data.repository.ThemeMode
import com.example.bloodsync_android.ui.theme.Bloodsync_androidTheme

class MainActivity : ComponentActivity() {

    private var repository: BloodSyncRepository? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Strictly lock orientation to Portrait (vertical) mode to prevent rotation and restarts
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Throwable) {}

        // Allow screenshots and screen recording (FLAG_SECURE removed per user request)
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } catch (_: Throwable) {}

        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {}

        val repo = BloodSyncRepository(applicationContext)
        repository = repo

        setContent {
            val themeMode by repo.themeMode
            val isSystemDark = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            Bloodsync_androidTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BloodSyncApp(repository = repo)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanly detach Firestore real-time snapshot listeners to prevent memory and quota leaks
        repository?.detachCloudListeners()
    }
}