package com.bloodsync

import android.app.Activity
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.AppNavDestination
import com.bloodsync.ui.components.BloodSyncBottomNav
import com.bloodsync.ui.components.EmergencyFloatingButton
import com.bloodsync.ui.components.ExitConfirmationDialog
import com.bloodsync.ui.screens.appointment.AppointmentScreen
import com.bloodsync.ui.screens.auth.AuthScreen
import com.bloodsync.ui.screens.certificate.CertificateScreen
import com.bloodsync.ui.screens.donors.DonorsDirectoryScreen
import com.bloodsync.ui.screens.emergency.EmergencyLiveTrackingScreen
import com.bloodsync.ui.screens.emergency.EmergencyRequestScreen
import com.bloodsync.ui.screens.health.HealthTrackerScreen
import com.bloodsync.ui.screens.history.DonationHistoryScreen
import com.bloodsync.ui.screens.home.HomeScreen
import com.bloodsync.ui.screens.notifications.NotificationCenterScreen
import com.bloodsync.ui.screens.profile.ProfileScreen
import com.bloodsync.ui.screens.settings.SettingsScreen
import com.bloodsync.ui.screens.splash.SplashScreen
import com.bloodsync.ui.theme.BloodSyncTheme
import com.bloodsync.util.EnglishTranslations
import com.bloodsync.util.LocalAppStrings
import com.bloodsync.util.LocationHelper

sealed class Screen {
    object Splash : Screen()
    object Auth : Screen()
    object Main : Screen()
    object EmergencyRequest : Screen()
    data class EmergencyLiveTracking(val requestId: String) : Screen()
    data class CertificateView(val certificateId: String?) : Screen()
    object Notifications : Screen()
    object Settings : Screen()
    object DonorsDirectory : Screen()
}

@Composable
fun BloodSyncApp(
    repository: BloodSyncRepository? = null
) {
    val context = LocalContext.current
    val repository = repository ?: remember { BloodSyncRepository(context) }
    val currentStrings = EnglishTranslations

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var currentNavDestination by remember { mutableStateOf(AppNavDestination.HOME) }
    var showExitDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalAppStrings provides currentStrings
    ) {

    // Intercept hardware/gesture Back button
    // 1. In Splash screen: prevent accidental exit while loading
    BackHandler(enabled = currentScreen is Screen.Splash) {
        // No-op during splash initialization
    }

    // 2. Sub-screens (Emergency, Live Tracking, Certificate, Notifications, Settings): navigate back to Main
    BackHandler(
        enabled = currentScreen !is Screen.Splash && currentScreen !is Screen.Auth && currentScreen !is Screen.Main
    ) {
        currentScreen = Screen.Main
    }

    // 3. Inside Main screen: If on non-HOME tab (History, Health, Appointments, Profile), navigate back to HOME tab
    BackHandler(
        enabled = currentScreen is Screen.Main && currentNavDestination != AppNavDestination.HOME
    ) {
        currentNavDestination = AppNavDestination.HOME
    }

    // 4. Root screens: On HOME tab of Main screen, or on Auth screen -> Prompt Exit Confirmation
    BackHandler(
        enabled = (currentScreen is Screen.Main && currentNavDestination == AppNavDestination.HOME) || (currentScreen is Screen.Auth)
    ) {
        showExitDialog = true
    }

    // Render Exit Confirmation Dialog when user presses back at the root screen
    if (showExitDialog) {
        val activity = context as? Activity
        ExitConfirmationDialog(
            onConfirmExit = {
                showExitDialog = false
                activity?.finishAffinity()
            },
            onDismiss = {
                showExitDialog = false
            }
        )
    }

    AnimatedContent(
        targetState = currentScreen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppScreenTransition"
    ) { screen ->
        when (screen) {
            is Screen.Splash -> {
                SplashScreen(
                    onSplashComplete = {
                        val firebaseUser = repository.firebaseService.getFirebaseAuth()?.currentUser
                        val isLoggedIn = repository.isUserLoggedIn.value || (firebaseUser != null)
                        if (isLoggedIn && !repository.isUserLoggedIn.value) {
                            repository.setLoggedIn(true)
                        }
                        currentScreen = if (isLoggedIn) Screen.Main else Screen.Auth
                    }
                )
            }

            is Screen.Auth -> {
                AuthScreen(
                    repository = repository,
                    onLoginSuccess = {
                        currentScreen = Screen.Main
                    }
                )
            }

            is Screen.Main -> {
                val locationLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                    val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                    if (fine || coarse) {
                        LocationHelper.getCurrentLocation(
                            context = context,
                            onLocation = { lat: Double, lon: Double ->
                                val current = repository.userProfile.value
                                if (current.latitude == null || current.longitude == null) {
                                    repository.updateUserProfile(current.copy(latitude = lat, longitude = lon))
                                }
                            }
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    if (!LocationHelper.hasLocationPermission(context)) {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = BloodSyncTheme.colors.background,
                    bottomBar = {
                        BloodSyncBottomNav(
                            currentDestination = currentNavDestination,
                            onNavigate = { destination ->
                                currentNavDestination = destination
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentNavDestination) {
                            AppNavDestination.HOME -> {
                                HomeScreen(
                                    repository = repository,
                                    onNavigateToEmergency = { currentScreen = Screen.EmergencyRequest },
                                    onNavigateToLiveTracking = { requestId ->
                                        currentScreen = Screen.EmergencyLiveTracking(requestId)
                                    },
                                    onNavigateToHistory = { currentNavDestination = AppNavDestination.HISTORY },
                                    onNavigateToCertificates = { currentScreen = Screen.CertificateView(null) },
                                    onNavigateToHealth = { currentNavDestination = AppNavDestination.HEALTH },
                                    onNavigateToAppointments = { currentNavDestination = AppNavDestination.APPOINTMENTS },
                                    onNavigateToNotifications = { currentScreen = Screen.Notifications },
                                    onNavigateToSettings = { currentScreen = Screen.Settings },
                                    onNavigateToDonors = { currentScreen = Screen.DonorsDirectory }
                                )
                            }

                            AppNavDestination.HISTORY -> {
                                DonationHistoryScreen(
                                    repository = repository,
                                    onBackClick = { currentNavDestination = AppNavDestination.HOME },
                                    onViewCertificate = { certId ->
                                        currentScreen = Screen.CertificateView(certId)
                                    },
                                    onNotificationClick = { currentScreen = Screen.Notifications }
                                )
                            }

                            AppNavDestination.HEALTH -> {
                                HealthTrackerScreen(
                                    repository = repository,
                                    onBackClick = { currentNavDestination = AppNavDestination.HOME },
                                    onBookAppointment = { currentNavDestination = AppNavDestination.APPOINTMENTS },
                                    onNotificationClick = { currentScreen = Screen.Notifications }
                                )
                            }

                            AppNavDestination.APPOINTMENTS -> {
                                AppointmentScreen(
                                    repository = repository,
                                    onBackClick = { currentNavDestination = AppNavDestination.HOME },
                                    onNotificationClick = { currentScreen = Screen.Notifications }
                                )
                            }

                            AppNavDestination.PROFILE -> {
                                ProfileScreen(
                                    repository = repository,
                                    onBackClick = { currentNavDestination = AppNavDestination.HOME },
                                    onLogout = { currentScreen = Screen.Auth },
                                    onNotificationClick = { currentScreen = Screen.Notifications },
                                    onNavigateToSettings = { currentScreen = Screen.Settings }
                                )
                            }
                        }
                    }
                }
            }

            is Screen.EmergencyRequest -> {
                EmergencyRequestScreen(
                    repository = repository,
                    onBackClick = { currentScreen = Screen.Main },
                    onRequestCreated = { newRequestId ->
                        currentScreen = Screen.EmergencyLiveTracking(newRequestId)
                    },
                    onNotificationClick = { currentScreen = Screen.Notifications }
                )
            }

            is Screen.EmergencyLiveTracking -> {
                EmergencyLiveTrackingScreen(
                    repository = repository,
                    requestId = screen.requestId,
                    onBackClick = { currentScreen = Screen.Main },
                    onNotificationClick = { currentScreen = Screen.Notifications }
                )
            }

            is Screen.CertificateView -> {
                CertificateScreen(
                    repository = repository,
                    initialCertificateId = screen.certificateId,
                    onBackClick = { currentScreen = Screen.Main },
                    onNotificationClick = { currentScreen = Screen.Notifications }
                )
            }

            is Screen.Notifications -> {
                NotificationCenterScreen(
                    repository = repository,
                    onBackClick = { currentScreen = Screen.Main },
                    onNavigateToTarget = { targetScreen, targetId ->
                        when (targetScreen) {
                            "emergency" -> {
                                if (targetId != null) {
                                    currentScreen = Screen.EmergencyLiveTracking(targetId)
                                } else {
                                    currentScreen = Screen.EmergencyRequest
                                }
                            }
                            "certificate" -> {
                                currentScreen = Screen.CertificateView(targetId)
                            }
                            "appointment" -> {
                                currentScreen = Screen.Main
                                currentNavDestination = AppNavDestination.APPOINTMENTS
                            }
                            "health" -> {
                                currentScreen = Screen.Main
                                currentNavDestination = AppNavDestination.HEALTH
                            }
                            "history" -> {
                                currentScreen = Screen.Main
                                currentNavDestination = AppNavDestination.HISTORY
                            }
                            else -> {
                                currentScreen = Screen.Main
                            }
                        }
                    }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    repository = repository,
                    onBackClick = { currentScreen = Screen.Main }
                )
            }

            is Screen.DonorsDirectory -> {
                DonorsDirectoryScreen(
                    repository = repository,
                    onBackClick = { currentScreen = Screen.Main },
                    onNavigateToEmergency = { currentScreen = Screen.EmergencyRequest },
                    onNotificationClick = { currentScreen = Screen.Notifications }
                )
            }
        }
    }
}
}

