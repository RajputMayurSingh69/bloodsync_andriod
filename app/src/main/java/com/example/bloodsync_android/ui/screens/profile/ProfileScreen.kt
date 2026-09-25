package com.example.bloodsync_android.ui.screens.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.model.UserProfile
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*

@Composable
fun ProfileScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onNotificationClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val appColors = BloodSyncTheme.colors
    val profile by repository.userProfile
    val unreadNotifs by repository.unreadNotificationCount

    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(profile.name) }
    var phoneInput by remember { mutableStateOf(profile.phone) }
    var addressInput by remember { mutableStateOf(profile.address) }
    var cityInput by remember { mutableStateOf(profile.city) }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Donor Profile",
                subtitle = "Manage Account & Preferences",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick,
                onSettingsClick = onNavigateToSettings,
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = BloodRedPrimary
                        )
                    }
                }
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
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(BloodRedLight, CircleShape)
                            .border(2.dp, BloodRedPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.bloodGroup,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = BloodRedPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = profile.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedicalTextPrimary
                    )

                    Text(
                        text = "${profile.city} • Verified Voluntary Donor",
                        fontSize = 12.sp,
                        color = MedicalTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    StatusBadge(
                        text = "🏆 Silver Lifesaver (${profile.totalDonations} Donations)",
                        textColor = CertificateGoldDark,
                        backgroundColor = AlertYellowLight
                    )
                }
            }

            // Editable or Display Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Personal Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MedicalTextPrimary
                    )

                    if (isEditing) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            label = { Text("City") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val updated = profile.copy(
                                    name = nameInput,
                                    phone = phoneInput,
                                    city = cityInput,
                                    address = addressInput
                                )
                                repository.updateUserProfile(updated)
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Changes")
                        }
                    } else {
                        ProfileInfoRow("Blood Group", profile.bloodGroup)
                        ProfileInfoRow("Email", profile.email)
                        ProfileInfoRow("Phone", profile.phone)
                        ProfileInfoRow("City", profile.city)
                        ProfileInfoRow("Address", profile.address)
                    }
                }
            }

            // Preferences & Availability
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Donor Availability Preferences",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MedicalTextPrimary
                    )

                    PreferenceSwitchRow(
                        title = "Available for Emergency Calls",
                        subtitle = "Receive real-time 24/7 emergency broadcasts near you",
                        isChecked = profile.isAvailableDonor,
                        onCheckedChange = {
                            repository.updateUserProfile(profile.copy(isAvailableDonor = it))
                        }
                    )

                    PreferenceSwitchRow(
                        title = "Push Notifications",
                        subtitle = "Appointment alerts, eligibility countdown, and certificates",
                        isChecked = profile.isNotificationEnabled,
                        onCheckedChange = {
                            repository.updateUserProfile(profile.copy(isNotificationEnabled = it))
                        }
                    )
                }
            }

            // App Settings & Appearance Entry
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSettings),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(BloodRedPrimary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BloodRedPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings & Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "Light/Dark mode, 3-button insets & alerts",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = appColors.textMuted
                    )
                }
            }

            // Log Out Button
            OutlinedButton(
                onClick = {
                    repository.setLoggedIn(false)
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusUrgentRed)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = StatusUrgentRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out of BloodSync",
                    color = StatusUrgentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MedicalTextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MedicalTextPrimary)
    }
}

@Composable
fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MedicalTextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = MedicalTextSecondary)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = BloodRedPrimary, checkedTrackColor = BloodRedLight)
        )
    }
}
