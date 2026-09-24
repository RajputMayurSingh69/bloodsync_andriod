package com.example.bloodsync_android

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
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.AppNavDestination
import com.example.bloodsync_android.ui.components.BloodSyncBottomNav
import com.example.bloodsync_android.ui.components.EmergencyFloatingButton
import com.example.bloodsync_android.ui.screens.appointment.AppointmentScreen
import com.example.bloodsync_android.ui.screens.auth.AuthScreen
import com.example.bloodsync_android.ui.screens.certificate.CertificateScreen
import com.example.bloodsync_android.ui.screens.emergency.EmergencyLiveTrackingScreen
import com.example.bloodsync_android.ui.screens.emergency.EmergencyRequestScreen
import com.example.bloodsync_android.ui.screens.health.HealthTrackerScreen
import com.example.bloodsync_android.ui.screens.history.DonationHistoryScreen
import com.example.bloodsync_android.ui.screens.home.HomeScreen
import com.example.bloodsync_android.ui.screens.notifications.NotificationCenterScreen
import com.example.bloodsync_android.ui.screens.profile.ProfileScreen
import com.example.bloodsync_android.ui.screens.settings.SettingsScreen
import com.example.bloodsync_android.ui.screens.splash.SplashScreen
import com.example.bloodsync_android.ui.theme.BloodSyncTheme

sealed class Screen {
    object Splash : Screen()
    object Auth : Screen()
    object Main : Screen()
    object EmergencyRequest : Screen()
    data class EmergencyLiveTracking(val requestId: String) : Screen()
    data class CertificateView(val certificateId: String?) : Screen()
    object Notifications : Screen()
    object Settings : Screen()
}

@Composable
fun BloodSyncApp(
    repository: BloodSyncRepository? = null
) {
    val context = LocalContext.current
    val repository = repository ?: remember { BloodSyncRepository(context) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var currentNavDestination by remember { mutableStateOf(AppNavDestination.HOME) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppScreenTransition"
    ) { screen ->
        when (screen) {
            is Screen.Splash -> {
                SplashScreen(
                    onSplashComplete = {
                        val isLoggedIn = repository.isUserLoggedIn.value
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
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = BloodSyncTheme.colors.background,
                    bottomBar = {
                        BloodSyncBottomNav(
                            currentDestination = currentNavDestination,
                            onNavigate = { destination ->
                                currentNavDestination = destination
                            }
                        )
                    },
                    floatingActionButton = {
                        // Prominent 24/7 Emergency SOS Button accessible from all main tabs
                        EmergencyFloatingButton(
                            onClick = {
                                currentScreen = Screen.EmergencyRequest
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
                                    onNavigateToHistory = { currentNavDestination = AppNavDestination.HISTORY },
                                    onNavigateToCertificates = { currentScreen = Screen.CertificateView(null) },
                                    onNavigateToHealth = { currentNavDestination = AppNavDestination.HEALTH },
                                    onNavigateToAppointments = { currentNavDestination = AppNavDestination.APPOINTMENTS },
                                    onNavigateToNotifications = { currentScreen = Screen.Notifications },
                                    onNavigateToSettings = { currentScreen = Screen.Settings }
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
        }
    }
}
