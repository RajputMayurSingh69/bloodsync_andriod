package com.bloodsync

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.bloodsync.data.emergency.EmergencyAlertManager
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.data.repository.ThemeMode
import com.bloodsync.service.EmergencySirenService
import com.bloodsync.ui.theme.Bloodsync_androidTheme

class MainActivity : ComponentActivity() {

    private var repository: BloodSyncRepository? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("MainActivity", "POST_NOTIFICATIONS granted: $isGranted")
    }

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

        // Initialize Emergency Alert Manager
        EmergencyAlertManager.init(applicationContext)

        // Request POST_NOTIFICATIONS runtime permission on Android 13+
        requestNotificationPermissionIfNeeded()

        val repo = BloodSyncRepository(applicationContext)
        repository = repo

        // Handle incoming emergency intent extras if app was opened via notification/full-screen intent
        handleEmergencyIntent(intent)

        setContent {
            val themeMode by repo.themeMode
            val systemInDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDark
            }

            Bloodsync_androidTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BloodSyncApp(repository = repo)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEmergencyIntent(intent)
    }

    private fun handleEmergencyIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val requestId = intent.getStringExtra(EmergencySirenService.EXTRA_REQUEST_ID)
            ?: intent.getStringExtra("requestId")
            ?: intent.getStringExtra("extra_request_id")

        if (action == "ACTION_VIEW_EMERGENCY" || !requestId.isNullOrBlank()) {
            android.util.Log.i("MainActivity", "Handling inbound emergency deep-link intent for request: $requestId")
            EmergencyAlertManager.acknowledgeAndNavigate(applicationContext, requestId)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanly detach Firestore real-time snapshot listeners to prevent memory and quota leaks
        repository?.detachCloudListeners()
    }
}