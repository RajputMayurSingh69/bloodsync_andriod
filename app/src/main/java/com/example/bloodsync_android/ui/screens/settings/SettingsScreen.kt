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
                subtitle = "Preferences & App Options",
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
            // 1. Theme & Appearance (Light & Dark Only)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 14.dp)
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
                                text = "App Theme",
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
                            title = "Light",
                            icon = Icons.Default.WbSunny,
                            isSelected = currentTheme == ThemeMode.LIGHT,
                            onClick = { repository.setThemeMode(ThemeMode.LIGHT) }
                        )

                        ThemeOptionTile(
                            title = "Dark",
                            icon = Icons.Default.Nightlight,
                            isSelected = currentTheme == ThemeMode.DARK,
                            onClick = { repository.setThemeMode(ThemeMode.DARK) }
                        )
                    }
                }
            }

            // ==========================================
            // 2. Notification Preferences
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
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
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Notification Preferences",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Emergency alerts & appointment updates",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Emergency SOS Broadcasts",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Real-time alerts when nearby patients need your blood group",
                                fontSize = 11.sp,
                                color = appColors.textSecondary,
                                lineHeight = 15.sp
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
                                checkedTrackColor = StatusEligibleGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = appColors.divider)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Donation Reminders & Milestones",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Appointment slot reminders and lifesaver certificate updates",
                                fontSize = 11.sp,
                                color = appColors.textSecondary,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = appointmentAlertsEnabled,
                            onCheckedChange = { appointmentAlertsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StatusEligibleGreen
                            )
                        )
                    }
                }
            }

            // ==========================================
            // 4. Clinical Safety: 3-Month Interval Enforcement
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
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
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clinical Donation Interval",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "WHO 90-Day (3 Month) safety standard",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    SystemFeatureStatusRow(
                        title = "WHO 3-Month Interval Lock",
                        description = "Enforces safe recovery window between whole blood donations to safeguard donor hemoglobin and ferritin levels.",
                        statusText = if (daysLeft > 0) "$daysLeft Days Left" else "Eligible",
                        statusColor = if (daysLeft > 0) StatusWarningAmber else StatusEligibleGreen
                    )
                }
            }

            // ==========================================
            // 5. Data Management (Clear Local Cache)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage local device storage and cached records",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.dp, StatusUrgentRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = StatusUrgentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clear Local Cache & Reset",
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
                    text = "BloodSync Android • Version 2.2.0 (Official)",
                    fontSize = 11.sp,
                    color = appColors.textMuted
                )
                Text(
                    text = "Voluntary Blood Donor Network • Real-time Cloud Sync",
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
                    text = "Clear Local Cache?",
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will clear all local cached records on this device and refresh from Firebase server.",
                    color = appColors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.clearLocalCache()
                        showResetDialog = false
                        showResetSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Confirm Clear", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = appColors.textSecondary)
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = appColors.cardBackground
        )
    }

    // Reset Success Toast / Dialog
    if (showResetSuccess) {
        AlertDialog(
            onDismissRequest = { showResetSuccess = false },
            title = {
                Text(
                    text = "Cache Cleared",
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Local device cache has been cleared and synchronized with Firebase Cloud.",
                    color = appColors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showResetSuccess = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(26.dp),
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
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = appColors.cardBackground
        )
    }
}

@Composable
private fun ThemeOptionTile(
    title: String,
    subtitle: String = "",
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
            .clip(RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50.dp)
            ),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BloodRedPrimary else appColors.textPrimary
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }
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
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = appColors.textSecondary,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(50.dp),
            color = statusColor.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
