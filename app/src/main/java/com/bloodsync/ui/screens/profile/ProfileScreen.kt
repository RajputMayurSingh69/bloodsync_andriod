package com.bloodsync.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*

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
    val strings = com.bloodsync.util.LocalAppStrings.current
    val profile by repository.userProfile
    val unreadNotifs by repository.unreadNotificationCount

    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var nameInput by remember(profile.name) { mutableStateOf(profile.name) }
    var genderInput by remember(profile.gender) { mutableStateOf(profile.gender) }
    var ageInput by remember(profile.age) { mutableStateOf(profile.age.toString()) }
    var phoneInput by remember(profile.phone) { mutableStateOf(profile.phone) }
    var addressInput by remember(profile.address) { mutableStateOf(profile.address) }
    var cityInput by remember(profile.city) { mutableStateOf(profile.city) }
    var licenseNumberInput by remember(profile.licenseNumber) { mutableStateOf(profile.licenseNumber) }
    var contactPersonInput by remember(profile.contactPerson) { mutableStateOf(profile.contactPerson) }
    var operatingHoursInput by remember(profile.operatingHours) { mutableStateOf(profile.operatingHours) }
    var websiteInput by remember(profile.website) { mutableStateOf(profile.website) }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = if (profile.role == "blood_bank") "Bank Profile" else strings.donorProfileTitle,
                subtitle = if (isEditing) strings.editProfile else strings.personalContactInfo,
                showBackButton = true,
                onBackClick = {
                    if (isEditing) {
                        isEditing = false
                        nameInput = profile.name
                        genderInput = profile.gender
                        ageInput = profile.age.toString()
                        phoneInput = profile.phone
                        cityInput = profile.city
                        addressInput = profile.address
                        licenseNumberInput = profile.licenseNumber
                        contactPersonInput = profile.contactPerson
                        operatingHoursInput = profile.operatingHours
                        websiteInput = profile.website
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
                                contentDescription = strings.editProfile,
                                tint = BloodRedPrimary
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            isEditing = false
                            nameInput = profile.name
                            genderInput = profile.gender
                            ageInput = profile.age.toString()
                            phoneInput = profile.phone
                            cityInput = profile.city
                            addressInput = profile.address
                            licenseNumberInput = profile.licenseNumber
                            contactPersonInput = profile.contactPerson
                            operatingHoursInput = profile.operatingHours
                            websiteInput = profile.website
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.cancel,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==============================================================
            // 1. HERO IDENTITY CARD
            // ==============================================================
            BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (profile.role == "blood_bank") {
                        // Blood Bank Avatar
                        Surface(
                            modifier = Modifier.size(86.dp),
                            shape = CircleShape,
                            color = BloodRedPrimary,
                            border = BorderStroke(3.dp, MedicalWhite),
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = "Bank",
                                    tint = MedicalWhite,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = profile.name.ifBlank { "Registered Blood Bank" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = appColors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Bank",
                                tint = StatusEligibleGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${profile.city.ifBlank { "City" }} • Certified Facility",
                            fontSize = 13.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        // Donor Blood Group Avatar Squircle
                        Surface(
                            modifier = Modifier.size(86.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = BloodRedPrimary,
                            shadowElevation = 6.dp,
                            border = BorderStroke(2.dp, MedicalWhite)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile.bloodGroup.ifBlank { "O+" },
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MedicalWhite,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = profile.name.ifBlank { "Voluntary Donor" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
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
                            text = "${profile.city.ifBlank { "Nearby City" }} • Active Lifesaver",
                            fontSize = 13.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (profile.totalDonations > 0) {
                            val badgeText = when {
                                profile.totalDonations >= 25 -> "Legendary"
                                profile.totalDonations >= 20 -> "Diamond"
                                profile.totalDonations >= 15 -> "Gold"
                                profile.totalDonations >= 10 -> "Silver"
                                else -> "Bronze"
                            }
                            StatusPillBadge(
                                text = "🏆 $badgeText Lifesaver • ${profile.totalDonations} Donations",
                                backgroundColor = BloodRedLight,
                                contentColor = BloodRedPrimary,
                                showBeacon = false
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 3 Metric Impact Badges with AnimatedCounter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedCounter(
                                    targetValue = profile.totalDonations,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BloodRedPrimary
                                )
                                Text(
                                    text = "Donations",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appColors.textSecondary
                                )
                            }

                            Box(modifier = Modifier.height(30.dp).width(1.dp).background(MedicalBorder))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedCounter(
                                    targetValue = profile.totalDonations * 450,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = StatusEligibleGreen,
                                    suffix = "ml"
                                )
                                Text(
                                    text = "Donated",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appColors.textSecondary
                                )
                            }

                            Box(modifier = Modifier.height(30.dp).width(1.dp).background(MedicalBorder))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedCounter(
                                    targetValue = profile.livesSaved,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = StatusWarningAmber
                                )
                                Text(
                                    text = "Lives Saved",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // ==============================================================
            // 2. PERSONAL DETAILS CARD (View / In-Place Edit Mode)
            // ==============================================================
            BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                shape = RoundedCornerShape(50.dp),
                                color = BloodRedLight,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .clickable { isEditing = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = BloodRedPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
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
                        if (profile.role == "blood_bank") {
                            BloodSyncTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = "Institution Name",
                                leadingIcon = Icons.Default.LocalHospital
                            )
                            BloodSyncTextField(
                                value = licenseNumberInput,
                                onValueChange = { licenseNumberInput = it },
                                label = "Government License Number",
                                leadingIcon = Icons.Default.Badge
                            )
                            BloodSyncTextField(
                                value = contactPersonInput,
                                onValueChange = { contactPersonInput = it },
                                label = "Contact Person",
                                leadingIcon = Icons.Default.Person
                            )
                            BloodSyncTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = "24/7 Helpline Number",
                                leadingIcon = Icons.Default.Phone,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            BloodSyncTextField(
                                value = operatingHoursInput,
                                onValueChange = { operatingHoursInput = it },
                                label = "Operating Hours",
                                leadingIcon = Icons.Default.AccessTime
                            )
                            BloodSyncTextField(
                                value = cityInput,
                                onValueChange = { cityInput = it },
                                label = "City / Region",
                                leadingIcon = Icons.Default.LocationCity
                            )
                            BloodSyncTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                label = "Full Facility Address",
                                leadingIcon = Icons.Default.Home,
                                singleLine = false
                            )
                            BloodSyncTextField(
                                value = websiteInput,
                                onValueChange = { websiteInput = it },
                                label = "Official Website",
                                leadingIcon = Icons.Default.Language
                            )
                        } else {
                            BloodSyncTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = "Full Name",
                                leadingIcon = Icons.Default.Person
                            )

                            // Gender Chips
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Gender",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appColors.textPrimary,
                                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple("Male", "Male", Icons.Default.Male),
                                        Triple("Female", "Female", Icons.Default.Female),
                                        Triple("Other", "Other", Icons.Default.Transgender)
                                    ).forEach { (gKey, gLabel, gIcon) ->
                                        val isSelected = genderInput == gKey
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(50.dp))
                                                .clickable { genderInput = gKey },
                                            shape = RoundedCornerShape(50.dp),
                                            color = if (isSelected) BloodRedPrimary else MedicalWhite,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) BloodRedPrimary else MedicalBorder
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = gIcon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MedicalWhite else BloodRedPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = gLabel,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MedicalWhite else appColors.textPrimary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            BloodSyncTextField(
                                value = ageInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 3) {
                                        ageInput = input
                                    }
                                },
                                label = "Age (Years)",
                                leadingIcon = Icons.Default.Cake,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            BloodSyncTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = "Phone Number",
                                leadingIcon = Icons.Default.Phone,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            BloodSyncTextField(
                                value = cityInput,
                                onValueChange = { cityInput = it },
                                label = "City / Region",
                                leadingIcon = Icons.Default.LocationCity
                            )

                            BloodSyncTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                label = "Residential Address",
                                leadingIcon = Icons.Default.Home,
                                singleLine = false
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BloodSyncOutlinedButton(
                                text = "Cancel",
                                onClick = {
                                    isEditing = false
                                    nameInput = profile.name
                                    genderInput = profile.gender
                                    ageInput = profile.age.toString()
                                    phoneInput = profile.phone
                                    cityInput = profile.city
                                    addressInput = profile.address
                                    licenseNumberInput = profile.licenseNumber
                                    contactPersonInput = profile.contactPerson
                                    operatingHoursInput = profile.operatingHours
                                    websiteInput = profile.website
                                },
                                modifier = Modifier.weight(1f)
                            )

                            BloodSyncButton(
                                text = "Save Changes",
                                onClick = {
                                    if (nameInput.trim().length < 2) {
                                        Toast.makeText(context, strings.invalidNameError, Toast.LENGTH_SHORT).show()
                                        return@BloodSyncButton
                                    }
                                    val parsedAge = if (profile.role == "blood_bank") 0 else ageInput.toIntOrNull()
                                    if (profile.role != "blood_bank" && (parsedAge == null || parsedAge < 15)) {
                                        Toast.makeText(context, strings.ageRestrictionError, Toast.LENGTH_LONG).show()
                                        return@BloodSyncButton
                                    }
                                    val updated = profile.copy(
                                        name = nameInput.trim(),
                                        gender = genderInput,
                                        age = parsedAge ?: 0,
                                        phone = phoneInput.trim(),
                                        city = cityInput.trim(),
                                        address = addressInput.trim(),
                                        licenseNumber = licenseNumberInput.trim(),
                                        contactPerson = contactPersonInput.trim(),
                                        operatingHours = operatingHoursInput.trim(),
                                        website = websiteInput.trim()
                                    )
                                    repository.updateUserProfile(updated)
                                    isEditing = false
                                    Toast.makeText(context, strings.profileUpdatedSuccess, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Display Mode Rows
                        if (profile.role == "blood_bank") {
                            ProfileDetailRow(Icons.Default.LocalHospital, "Institution Name", profile.name.ifBlank { "Not Specified" }, BloodRedPrimary)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Badge, "License Number", profile.licenseNumber.ifBlank { "Not Added" }, StatusEligibleGreen)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Person, "Contact Person", profile.contactPerson.ifBlank { "Not Added" }, StatusWarningAmber)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Phone, "Helpline Number", profile.phone.ifBlank { "Not Added" }, StatusEligibleGreen)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Email, "Email Address", profile.email.ifBlank { "Not Added" }, StatusWarningAmber)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.AccessTime, "Operating Hours", profile.operatingHours.ifBlank { "24/7" }, BloodRedPrimary)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.LocationCity, "City / Region", profile.city.ifBlank { "Not Added" }, BloodRedPrimary)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Home, "Facility Address", profile.address.ifBlank { "Not Added" }, StatusEligibleGreen)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Language, "Website", profile.website.ifBlank { "Not Added" }, BloodRedPrimary)
                        } else {
                            ProfileDetailRow(Icons.Default.Bloodtype, "Blood Group", profile.bloodGroup.ifBlank { "Not Specified" }, BloodRedPrimary)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(
                                when (profile.gender) {
                                    "Female" -> Icons.Default.Female
                                    "Other", "Others" -> Icons.Default.Transgender
                                    else -> Icons.Default.Male
                                },
                                "Gender",
                                profile.gender,
                                StatusEligibleGreen
                            )
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Cake, "Age", "${profile.age} Years", StatusWarningAmber)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Phone, "Phone Number", profile.phone.ifBlank { "Not Added" }, StatusEligibleGreen)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Email, "Email Address", profile.email.ifBlank { "donor@bloodsync.org" }, StatusWarningAmber)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.LocationCity, "City / Region", profile.city.ifBlank { "Nearby" }, BloodRedPrimary)
                            HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                            ProfileDetailRow(Icons.Default.Home, "Residential Address", profile.address.ifBlank { "Not Added" }, StatusEligibleGreen)
                        }
                    }
                }
            }

            // ==============================================================
            // 3. EMERGENCY CONTACT & VOLUNTEER CARD
            // ==============================================================
            BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Emergency Response & Volunteer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = appColors.textPrimary
                    )

                    if (profile.role != "blood_bank") {
                        PreferenceSwitchRow(
                            title = "Volunteer in Emergencies",
                            subtitle = "Receive immediate push notifications when urgent blood is needed within 10 km",
                            isChecked = profile.isEmergencyVolunteer,
                            onCheckedChange = {
                                repository.updateUserProfile(profile.copy(isEmergencyVolunteer = it))
                            }
                        )

                        HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)

                        PreferenceSwitchRow(
                            title = "Available for Urgent Calls",
                            subtitle = "Allow certified hospitals to view your donor readiness beacon",
                            isChecked = profile.isAvailableDonor,
                            onCheckedChange = {
                                repository.updateUserProfile(profile.copy(isAvailableDonor = it))
                            }
                        )

                        HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                    }

                    PreferenceSwitchRow(
                        title = "Push Notifications & Broadcasts",
                        subtitle = "Receive appointment updates, eligibility alerts and verified donor certificates",
                        isChecked = profile.isNotificationEnabled,
                        onCheckedChange = {
                            repository.updateUserProfile(profile.copy(isNotificationEnabled = it))
                        }
                    )
                }
            }

            // ==============================================================
            // 4. EMERGENCY HOTLINE CARD
            // ==============================================================
            BloodSyncCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BloodRedLight,
                borderColor = BloodRedPrimary.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BloodRedPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MedicalWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Emergency Blood Helpline",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BloodRedPrimary
                            )
                            Text(
                                text = "24/7 National Toll-Free: 104",
                                fontSize = 12.sp,
                                color = TextPrimaryDark
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:104")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        shape = RoundedCornerShape(50.dp),
                        color = BloodRedPrimary
                    ) {
                        Text(
                            text = "CALL 104",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MedicalWhite
                        )
                    }
                }
            }

            // ==============================================================
            // 5. SETTINGS SHORTCUT CARD
            // ==============================================================
            BloodSyncCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSettings() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(BloodRedLight, CircleShape),
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
                            text = "Settings & App Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "Notification frequencies, data sync & privacy terms",
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
            // 6. LOG OUT PILL BUTTON
            // ==============================================================
            BloodSyncOutlinedButton(
                text = "Log Out of BloodSync",
                onClick = { showLogoutDialog = true },
                icon = Icons.AutoMirrored.Filled.Logout,
                textColor = BloodRedPrimary,
                borderColor = BloodRedPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Friendly Logout Confirmation Dialog
    if (showLogoutDialog) {
        ExitConfirmationDialog(
            title = "Log Out of BloodSync?",
            message = "Are you sure you want to log out? You will need to sign in again to access emergency broadcasts and appointments.",
            confirmText = "Log Out",
            dismissText = "Cancel",
            onConfirm = {
                showLogoutDialog = false
                repository.logoutUser()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    val appColors = BloodSyncTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = accentColor.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                color = appColors.textMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
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
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = appColors.textSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MedicalWhite,
                checkedTrackColor = StatusEligibleGreen,
                uncheckedTrackColor = MedicalBorder
            )
        )
    }
}
