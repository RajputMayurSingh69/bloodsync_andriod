package com.example.bloodsync_android.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.data.repository.ThemeMode
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.theme.*

@Composable
fun SettingsScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit
) {
    val currentTheme by repository.themeMode
    val healthRecord by repository.healthRecord
    val profile by repository.userProfile
    val eligibilityResult = remember(healthRecord) { healthRecord.calculateEligibility() }
    val daysLeft = eligibilityResult.daysRemaining

    var emergencyAlertsEnabled by remember { mutableStateOf(profile.isAvailableDonor) }
    var eligibilityAlertsEnabled by remember { mutableStateOf(true) }
    var appointmentAlertsEnabled by remember { mutableStateOf(true) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showResetSuccess by remember { mutableStateOf(false) }

    val appColors = BloodSyncTheme.colors

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Settings",
                subtitle = "Appearance & System Insets",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = appColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ==========================================
            // 1. Theme & Appearance (Light, Dark, Phone Jesa)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(BloodRedPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "App Theme & Appearance",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Choose your preferred display mode",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeOptionTile(
                            title = "Phone Jaisa (System Default)",
                            subtitle = "Automatically follows your phone's dark/light setting",
                            icon = Icons.Default.PhoneAndroid,
                            isSelected = currentTheme == ThemeMode.SYSTEM,
                            onClick = { repository.setThemeMode(ThemeMode.SYSTEM) }
                        )

                        ThemeOptionTile(
                            title = "Light Mode",
                            subtitle = "Clean medical white with crimson accents",
                            icon = Icons.Default.WbSunny,
                            isSelected = currentTheme == ThemeMode.LIGHT,
                            onClick = { repository.setThemeMode(ThemeMode.LIGHT) }
                        )

                        ThemeOptionTile(
                            title = "Dark Mode",
                            subtitle = "Sleek slate-dark theme comfortable for night use",
                            icon = Icons.Default.Nightlight,
                            isSelected = currentTheme == ThemeMode.DARK,
                            onClick = { repository.setThemeMode(ThemeMode.DARK) }
                        )
                    }
                }
            }

            // ==========================================
            // 2. Navigation Bar & Status Bar Auto-Adjustment
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StatusInfoBlue.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = null,
                                tint = StatusInfoBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "System Bars & Insets Auto-Adjustment",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Adapts seamlessly to your device hardware",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    // Feature 1: Navigation Buttons Inset
                    SystemFeatureStatusRow(
                        title = "3-Button Navigation Auto-Adjustment",
                        description = "Automatically calculates your phone's 3-button navigation bar (Back, Home, Recents) height so bottom tabs and buttons are never covered or cut off.",
                        statusText = "Active",
                        statusColor = StatusEligibleGreen
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = appColors.divider)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Feature 2: Smart Status Bar
                    SystemFeatureStatusRow(
                        title = "Smart Adaptive Status Bar",
                        description = "Status bar icons (clock, battery %, Wi-Fi) automatically contrast with the app background — dark icons on light mode, light icons on dark mode — so they never wash out or turn invisible.",
                        statusText = "Optimized",
                        statusColor = StatusEligibleGreen
                    )
                }
            }

            // ==========================================
            // 3. Notification Preferences
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StatusWarningAmber.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = StatusWarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Notifications & Alerts",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Emergency broadcasts & donation timers",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    SettingToggleRow(
                        title = "24/7 Emergency Blood SOS",
                        subtitle = "High-priority alerts for critical patients needing your blood type",
                        isChecked = emergencyAlertsEnabled,
                        onCheckedChange = {
                            emergencyAlertsEnabled = it
                            repository.updateUserProfile(profile.copy(isAvailableDonor = it))
                        }
                    )

                    HorizontalDivider(color = appColors.divider, modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "3-Month Eligibility Alerts",
                        subtitle = "Notifies you as soon as 90 days have passed and you can donate again",
                        isChecked = eligibilityAlertsEnabled,
                        onCheckedChange = { eligibilityAlertsEnabled = it }
                    )

                    HorizontalDivider(color = appColors.divider, modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Appointment Schedule Reminders",
                        subtitle = "Reminders 24 hours prior to scheduled blood bank slots",
                        isChecked = appointmentAlertsEnabled,
                        onCheckedChange = { appointmentAlertsEnabled = it }
                    )
                }
            }

            // ==========================================
            // 4. Medical Rules & 3-Month Donation Gap
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(BloodRedPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Medical Safety: 3-Month Rule",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "WHO & Red Cross mandatory safety interval",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    Text(
                        text = "To safeguard donor well-being and allow ferritin (iron) stores and red blood cells to fully replenish, whole blood donations are strictly limited to once every 3 months (90 days).",
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = appColors.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Your Next Eligible Date:",
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary
                                )
                                Text(
                                    text = healthRecord.getNextEligibleDateFormatted(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BloodRedPrimary
                                )
                            }
                            Surface(
                                color = if (daysLeft <= 0) StatusEligibleGreenLight else StatusWarningAmberLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (daysLeft <= 0) "Eligible Now" else "$daysLeft Days Left",
                                    color = if (daysLeft <= 0) StatusEligibleGreen else StatusWarningAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. Data Management & Reset
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data & App Management",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reset app data back to clean demonstration defaults",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StatusUrgentRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = StatusUrgentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset Demo Data & Clear Cache",
                            color = StatusUrgentRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // ==========================================
            // 6. About App & Version
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { showTermsDialog = true }
                ) {
                    Text(
                        text = "Privacy Policy & Medical Terms",
                        fontSize = 12.sp,
                        color = BloodRedPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = BloodRedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "BloodSync Android • Version 1.3.0",
                    fontSize = 11.sp,
                    color = appColors.textMuted
                )
                Text(
                    text = "Voluntary Blood Donor Network • Real-time Sync",
                    fontSize = 11.sp,
                    color = appColors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Demo Data?",
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will reset all local appointments, donation history, and preferences back to fresh demonstration records.",
                    color = appColors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetDemoData()
                        showResetDialog = false
                        showResetSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text("Confirm Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = appColors.textSecondary)
                }
            },
            containerColor = appColors.cardBackground
        )
    }

    // Reset Success Toast / Dialog
    if (showResetSuccess) {
        AlertDialog(
            onDismissRequest = { showResetSuccess = false },
            title = {
                Text(
                    text = "Demo Data Reset",
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "All sample appointments, 3-month medical timers, and settings have been restored to default state.",
                    color = appColors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showResetSuccess = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = appColors.cardBackground
        )
    }

    // Terms & Privacy Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(
                    text = "BloodSync Medical Terms",
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Voluntary Donation: BloodSync operates strictly as a humanitarian voluntary donor matching platform.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                    Text(
                        text = "2. 3-Month Interval: In accordance with global clinical standards, donors must not donate whole blood within 90 days of a previous donation.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                    Text(
                        text = "3. Emergency Broadcasts: Emergency requests are reserved strictly for verifiable urgent medical transfusions.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = appColors.cardBackground
        )
    }
}

@Composable
private fun ThemeOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) BloodRedPrimary.copy(alpha = if (appColors.isDark) 0.2f else 0.08f) else appColors.surfaceVariant,
        animationSpec = tween(200),
        label = "tileBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) BloodRedPrimary else appColors.border,
        animationSpec = tween(200),
        label = "tileBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            ),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isSelected) BloodRedPrimary else appColors.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else appColors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BloodRedPrimary else appColors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = appColors.textSecondary
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = BloodRedPrimary,
                    unselectedColor = appColors.textMuted
                )
            )
        }
    }
}

@Composable
private fun SystemFeatureStatusRow(
    title: String,
    description: String,
    statusText: String,
    statusColor: Color
) {
    val appColors = BloodSyncTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = appColors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = appColors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BloodRedPrimary,
                checkedTrackColor = BloodRedPrimary.copy(alpha = 0.3f),
                uncheckedThumbColor = appColors.textMuted,
                uncheckedTrackColor = appColors.surfaceVariant
            )
        )
    }
}
