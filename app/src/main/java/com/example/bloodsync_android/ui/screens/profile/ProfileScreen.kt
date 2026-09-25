package com.example.bloodsync_android.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val profile by repository.userProfile
    val unreadNotifs by repository.unreadNotificationCount

    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember(profile.name) { mutableStateOf(profile.name) }
    var phoneInput by remember(profile.phone) { mutableStateOf(profile.phone) }
    var addressInput by remember(profile.address) { mutableStateOf(profile.address) }
    var cityInput by remember(profile.city) { mutableStateOf(profile.city) }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Donor Profile",
                subtitle = if (isEditing) "Editing Your Information" else "Manage Account & Preferences",
                showBackButton = true,
                onBackClick = {
                    if (isEditing) {
                        isEditing = false
                        nameInput = profile.name
                        phoneInput = profile.phone
                        cityInput = profile.city
                        addressInput = profile.address
                    } else {
                        onBackClick()
                    }
                },
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick,
                onSettingsClick = onNavigateToSettings,
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = BloodRedPrimary
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            isEditing = false
                            nameInput = profile.name
                            phoneInput = profile.phone
                            cityInput = profile.city
                            addressInput = profile.address
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Edit",
                                tint = appColors.textMuted
                            )
                        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==============================================================
            // 1. HERO IDENTITY CARD (Blood Group, Name, Verified, Metrics)
            // ==============================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Blood Group Avatar
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(BloodRedPrimary, CircleShape)
                            .border(3.dp, appColors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.bloodGroup.ifBlank { "O+" },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = profile.name.ifBlank { "Voluntary Donor" },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Donor",
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "${profile.city.ifBlank { "Registered City" }} • Active Lifesaver",
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StatusBadge(
                        text = "🏆 Silver Lifesaver • ${profile.totalDonations} Donations",
                        textColor = CertificateGoldDark,
                        backgroundColor = appColors.yellowLight
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = appColors.divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Metric Impact Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileImpactStat(
                            value = "${profile.totalDonations}",
                            label = "Donations",
                            accentColor = BloodRedPrimary,
                            textColor = appColors.textPrimary,
                            subColor = appColors.textSecondary
                        )

                        Box(modifier = Modifier.height(28.dp).width(1.dp).background(appColors.divider))

                        ProfileImpactStat(
                            value = "${profile.totalDonations * 450}ml",
                            label = "Donated",
                            accentColor = StatusEligibleGreen,
                            textColor = appColors.textPrimary,
                            subColor = appColors.textSecondary
                        )

                        Box(modifier = Modifier.height(28.dp).width(1.dp).background(appColors.divider))

                        ProfileImpactStat(
                            value = "${profile.livesSaved}",
                            label = "Lives Saved",
                            accentColor = StatusWarningAmber,
                            textColor = appColors.textPrimary,
                            subColor = appColors.textSecondary
                        )
                    }
                }
            }

            // ==============================================================
            // 2. PERSONAL DETAILS CARD (With High-Contrast Editable Form)
            // ==============================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditing) "Edit Information" else "Personal & Contact Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = appColors.textPrimary
                        )

                        if (!isEditing) {
                            Surface(
                                color = appColors.redLight,
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.clickable { isEditing = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = BloodRedPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit",
                                        color = BloodRedPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (isEditing) {
                        // High Contrast Outlined Text Fields (Guaranteed Text Visibility in Dark & Light)
                        val textFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary,
                            focusedContainerColor = appColors.inputBackground,
                            unfocusedContainerColor = appColors.inputBackground,
                            focusedBorderColor = BloodRedPrimary,
                            unfocusedBorderColor = appColors.border,
                            focusedLabelColor = BloodRedPrimary,
                            unfocusedLabelColor = appColors.textMuted,
                            cursorColor = BloodRedPrimary
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BloodRedPrimary)
                            },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number (10 digits)") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = BloodRedPrimary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            label = { Text("City / Region") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = BloodRedPrimary)
                            },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text("Address / Landmark") },
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null, tint = BloodRedPrimary)
                            },
                            maxLines = 2,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isEditing = false
                                    nameInput = profile.name
                                    phoneInput = profile.phone
                                    cityInput = profile.city
                                    addressInput = profile.address
                                },
                                shape = RoundedCornerShape(50.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Cancel", color = appColors.textSecondary, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    if (nameInput.trim().length < 2) {
                                        Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val updated = profile.copy(
                                        name = nameInput.trim(),
                                        phone = phoneInput.trim(),
                                        city = cityInput.trim(),
                                        address = addressInput.trim()
                                    )
                                    repository.updateUserProfile(updated)
                                    isEditing = false
                                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Display Mode with Clean Rows and Icons
                        ProfileDetailRow(
                            icon = Icons.Default.Bloodtype,
                            label = "Blood Group",
                            value = profile.bloodGroup.ifBlank { "Not Specified" },
                            accentColor = BloodRedPrimary,
                            textColor = appColors.textPrimary,
                            labelColor = appColors.textSecondary
                        )

                        HorizontalDivider(color = appColors.divider)

                        ProfileDetailRow(
                            icon = Icons.Default.Phone,
                            label = "Phone Number",
                            value = profile.phone.ifBlank { "Not Added" },
                            accentColor = StatusEligibleGreen,
                            textColor = appColors.textPrimary,
                            labelColor = appColors.textSecondary
                        )

                        HorizontalDivider(color = appColors.divider)

                        ProfileDetailRow(
                            icon = Icons.Default.Email,
                            label = "Email Address",
                            value = profile.email.ifBlank { "donor@bloodsync.org" },
                            accentColor = StatusWarningAmber,
                            textColor = appColors.textPrimary,
                            labelColor = appColors.textSecondary
                        )

                        HorizontalDivider(color = appColors.divider)

                        ProfileDetailRow(
                            icon = Icons.Default.LocationCity,
                            label = "City / Region",
                            value = profile.city.ifBlank { "Nearby" },
                            accentColor = BloodRedPrimary,
                            textColor = appColors.textPrimary,
                            labelColor = appColors.textSecondary
                        )

                        HorizontalDivider(color = appColors.divider)

                        ProfileDetailRow(
                            icon = Icons.Default.Home,
                            label = "Residential Address",
                            value = profile.address.ifBlank { "Not Added" },
                            accentColor = StatusEligibleGreen,
                            textColor = appColors.textPrimary,
                            labelColor = appColors.textSecondary
                        )
                    }
                }
            }

            // ==============================================================
            // 3. AVAILABILITY & NOTIFICATION SETTINGS CARD
            // ==============================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Availability & Broadcast Alerts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = appColors.textPrimary
                    )

                    PreferenceSwitchRow(
                        title = "Available for Emergency Calls",
                        subtitle = "Notify nearby patients and hospitals that you are ready to donate",
                        isChecked = profile.isAvailableDonor,
                        onCheckedChange = {
                            repository.updateUserProfile(profile.copy(isAvailableDonor = it))
                        },
                        textColor = appColors.textPrimary,
                        subColor = appColors.textSecondary
                    )

                    HorizontalDivider(color = appColors.divider)

                    PreferenceSwitchRow(
                        title = "Push Notifications & Alerts",
                        subtitle = "Receive appointment reminders, health cooldowns & verified certificates",
                        isChecked = profile.isNotificationEnabled,
                        onCheckedChange = {
                            repository.updateUserProfile(profile.copy(isNotificationEnabled = it))
                        },
                        textColor = appColors.textPrimary,
                        subColor = appColors.textSecondary
                    )
                }
            }

            // ==============================================================
            // 4. SETTINGS SHORTCUT CARD
            // ==============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onNavigateToSettings),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(appColors.redLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BloodRedPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings & Appearance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "Toggle Dark Mode, data management & about",
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

            // ==============================================================
            // 5. LOG OUT BUTTON (PILL)
            // ==============================================================
            OutlinedButton(
                onClick = {
                    repository.setLoggedIn(false)
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusUrgentRed)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
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
private fun ProfileImpactStat(
    value: String,
    label: String,
    accentColor: Color,
    textColor: Color,
    subColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = accentColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = subColor
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    textColor: Color,
    labelColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accentColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = labelColor)
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    subColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = subColor,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StatusEligibleGreen
            )
        )
    }
}
