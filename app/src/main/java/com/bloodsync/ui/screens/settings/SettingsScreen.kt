package com.bloodsync.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.data.repository.ThemeMode
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*
import com.bloodsync.util.LocalAppStrings

@Composable
fun SettingsScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentThemeMode by repository.themeMode
    val healthRecord by repository.healthRecord
    val profile by repository.userProfile
    val eligibilityResult = remember(healthRecord) { healthRecord.calculateEligibility() }
    val daysLeft = eligibilityResult.daysRemaining

    var emergencyAlertsEnabled by remember { mutableStateOf(profile.isAvailableDonor) }
    var cooldownAlertsEnabled by remember { mutableStateOf(true) }
    var appointmentAlertsEnabled by remember { mutableStateOf(true) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showResetSuccess by remember { mutableStateOf(false) }

    val appColors = BloodSyncTheme.colors

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = strings.settingsTitle,
                subtitle = "App Preferences & Safety",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = appColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. App Theme Preference
            // ==========================================
            BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(appColors.primaryLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = appColors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "App Theme",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                    }

                    // Interactive Theme Selection Tiles
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionTile(
                            title = "Light Mode",
                            icon = Icons.Default.LightMode,
                            isSelected = currentThemeMode == ThemeMode.LIGHT,
                            onClick = { repository.setThemeMode(ThemeMode.LIGHT) }
                        )

                        ThemeOptionTile(
                            title = "Dark Mode",
                            icon = Icons.Default.DarkMode,
                            isSelected = currentThemeMode == ThemeMode.DARK,
                            onClick = { repository.setThemeMode(ThemeMode.DARK) }
                        )
                    }
                }
            }

            // ==========================================
            // 2. Notification Preferences
            // ==========================================
            BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(appColors.primaryLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = appColors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Notification Switches",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Emergency alarms & clinical interval updates",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Switch 1: Emergency SOS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Emergency SOS Broadcasts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Real-time alerts when nearby patients need your blood group",
                                fontSize = 12.sp,
                                color = appColors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = emergencyAlertsEnabled,
                            onCheckedChange = {
                                emergencyAlertsEnabled = it
                                repository.updateUserProfile(profile.copy(isAvailableDonor = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = appColors.successGreen,
                                uncheckedTrackColor = appColors.border,
                                uncheckedThumbColor = appColors.textMuted
                            )
                        )
                    }

                    HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)

                    // Switch 2: WHO 90-Day Cooldown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "WHO 90-Day Cooldown Alerts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Notify immediately when 90-day recovery completes and you become eligible again",
                                fontSize = 12.sp,
                                color = appColors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = cooldownAlertsEnabled,
                            onCheckedChange = { cooldownAlertsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = appColors.successGreen,
                                uncheckedTrackColor = appColors.border,
                                uncheckedThumbColor = appColors.textMuted
                            )
                        )
                    }

                    HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)

                    // Switch 3: Appointment Reminders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Appointment Slot Reminders",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Slot reminders 2 hours prior to scheduled blood bank visits",
                                fontSize = 12.sp,
                                color = appColors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = appointmentAlertsEnabled,
                            onCheckedChange = { appointmentAlertsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = appColors.successGreen,
                                uncheckedTrackColor = appColors.border,
                                uncheckedThumbColor = appColors.textMuted
                            )
                        )
                    }
                }
            }

            // ==========================================
            // 3. Clinical Safety: 3-Month Interval Enforcement
            // ==========================================
            BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(appColors.primaryLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = appColors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clinical Donation Interval",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "WHO 90-Day (3 Month) Safety Standard",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recovery Window Status:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.textPrimary
                        )

                        StatusPillBadge(
                            text = if (daysLeft > 0) "$daysLeft Days Left" else "Eligible Now",
                            backgroundColor = if (daysLeft > 0) appColors.yellowLight else appColors.greenLight,
                            contentColor = if (daysLeft > 0) appColors.warningAmber else appColors.successGreen,
                            showBeacon = daysLeft == 0
                        )
                    }

                    Text(
                        text = "Enforces safe recovery window between whole blood donations to safeguard donor hemoglobin and ferritin levels.",
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // ==========================================
            // 4. Danger Zone: Reset Local Cache & Credentials
            // ==========================================
            BloodSyncCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (appColors.isDark) Color(0xFF261416) else BloodRedLight.copy(alpha = 0.25f),
                borderColor = appColors.primary.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Data Management & Cache",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.primary
                    )
                    Text(
                        text = "Clearing local cache removes offline stores and re-synchronizes real-time snapshots with Firebase Cloud.",
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    BloodSyncOutlinedButton(
                        text = "Reset Local Cache & Credentials",
                        onClick = { showResetDialog = true },
                        icon = Icons.Default.DeleteOutline,
                        textColor = appColors.primary,
                        borderColor = appColors.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ==========================================
            // 5. About & Terms
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
                        fontSize = 13.sp,
                        color = appColors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = appColors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        ExitConfirmationDialog(
            title = "Clear Local Cache & Reset?",
            message = "This will clear all local cached records on this device and re-fetch real-time snapshot data directly from Firebase Cloud Firestore.",
            confirmText = "Confirm Reset",
            dismissText = "Cancel",
            onConfirm = {
                repository.clearLocalCache()
                showResetDialog = false
                showResetSuccess = true
            },
            onDismiss = { showResetDialog = false }
        )
    }

    // Reset Success Dialog
    if (showResetSuccess) {
        ExitConfirmationDialog(
            title = "Cache Cleared Successfully",
            message = "Local device cache has been cleared and synchronized with Firebase Cloud.",
            confirmText = "OK",
            dismissText = "",
            onConfirm = { showResetSuccess = false },
            onDismiss = { showResetSuccess = false }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Voluntary Donation: BloodSync operates strictly as a humanitarian voluntary donor matching platform.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "2. 3-Month Interval: In accordance with global clinical standards, donors must not donate whole blood within 90 days of a previous donation.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "3. Emergency Broadcasts: Emergency requests are reserved strictly for verifiable urgent medical transfusions.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = appColors.surface
        )
    }
}

/**
 * Interactive selectable theme tile for Light and Dark mode.
 */
@Composable
private fun ThemeOptionTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (isSelected) appColors.primaryLight.copy(alpha = 0.45f) else appColors.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) appColors.primary else appColors.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isSelected) appColors.primary else appColors.primaryLight,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else appColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = appColors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = appColors.primary,
                    unselectedColor = appColors.textMuted
                )
            )
        }
    }
}
