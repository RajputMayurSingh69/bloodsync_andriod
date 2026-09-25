package com.example.bloodsync_android.ui.screens.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.model.UrgencyLevel
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodGroupSelector
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.theme.*
import com.example.bloodsync_android.util.ValidationHelper

/**
 * Super Simple, Fast Emergency Request Screen.
 * Strict Palette: RED, GREEN, WHITE, YELLOW ONLY.
 * Designed for immediate SOS submission without confusion during emergencies.
 */
@Composable
fun EmergencyRequestScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onRequestCreated: (requestId: String) -> Unit,
    onNotificationClick: () -> Unit
) {
    val unreadNotifs by repository.unreadNotificationCount

    var selectedGroup by remember { mutableStateOf("O+") }
    var unitsRequired by remember { mutableIntStateOf(2) }
    var urgencyLevel by remember { mutableStateOf(UrgencyLevel.IMMEDIATE) }

    var patientName by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var hospitalAddress by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Emergency Request",
                subtitle = "Fast SOS Broadcast",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = MedicalWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Urgent Red Alert Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = BloodRedPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MedicalWhite,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🚨 LIVE EMERGENCY BROADCAST",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MedicalWhite
                        )
                        Text(
                            text = "This form alerts all verified donors within 10km immediately.",
                            fontSize = 12.sp,
                            color = MedicalWhite.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Step 1: Blood Group & Units (Most Critical Information)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BloodGroupSelector(
                        selectedGroup = selectedGroup,
                        onGroupSelected = { selectedGroup = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MedicalDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Units Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MedicalTextPrimary
                            )
                            Text(
                                text = "Whole blood bags needed",
                                fontSize = 11.sp,
                                color = MedicalTextSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { if (unitsRequired > 1) unitsRequired-- },
                                enabled = unitsRequired > 1,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = BloodRedLight,
                                    contentColor = BloodRedPrimary
                                )
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }

                            Text(
                                text = "$unitsRequired",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = BloodRedPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )

                            FilledTonalIconButton(
                                onClick = { if (unitsRequired < 10) unitsRequired++ },
                                enabled = unitsRequired < 10,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = BloodRedLight,
                                    contentColor = BloodRedPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }

            // Step 2: Urgency Selection (Strict Red / Yellow / Green)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Urgency Level",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MedicalTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UrgencyLevel.entries.forEach { level ->
                            val isSelected = urgencyLevel == level
                            val activeColor = when (level) {
                                UrgencyLevel.IMMEDIATE -> BloodRedPrimary
                                UrgencyLevel.URGENT -> StatusWarningAmber
                                UrgencyLevel.WITHIN_24_HOURS -> StatusEligibleGreen
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50.dp))
                                    .clickable { urgencyLevel = level }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) activeColor else MedicalBorder,
                                        shape = RoundedCornerShape(50.dp)
                                    ),
                                color = if (isSelected) activeColor else MedicalWhite
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = when (level) {
                                            UrgencyLevel.IMMEDIATE -> "Immediate"
                                            UrgencyLevel.URGENT -> "Urgent"
                                            UrgencyLevel.WITHIN_24_HOURS -> "24 Hours"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MedicalWhite else MedicalTextPrimary
                                    )
                                    Text(
                                        text = when (level) {
                                            UrgencyLevel.IMMEDIATE -> "< 1 hr"
                                            UrgencyLevel.URGENT -> "< 3 hrs"
                                            UrgencyLevel.WITHIN_24_HOURS -> "< 24 hrs"
                                        },
                                        fontSize = 10.sp,
                                        color = if (isSelected) MedicalWhite.copy(alpha = 0.9f) else MedicalTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 3: Hospital & Contact Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Hospital & Contact Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MedicalTextPrimary
                    )

                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        label = { Text("Hospital Name *") },
                        placeholder = { Text("e.g. City General Hospital", color = MedicalTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = BloodRedPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Emergency Contact Phone *") },
                        placeholder = { Text("e.g. 9876543210", color = MedicalTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = StatusEligibleGreen)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hospitalAddress,
                        onValueChange = { hospitalAddress = it },
                        label = { Text("Hospital Address / Ward (Optional)") },
                        placeholder = { Text("e.g. Ward 4, Ring Road", color = MedicalTextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name (Optional)") },
                        placeholder = { Text("e.g. Amit Kumar", color = MedicalTextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Validation Error Alert (Red)
            if (validationError != null) {
                Surface(
                    color = BloodRedLight,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: "",
                        color = BloodRedPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Big Bold Broadcast Button (Red with White text)
            Button(
                onClick = {
                    val hospitalValidation = ValidationHelper.validateHospitalName(hospitalName)
                    if (!hospitalValidation.isValid) {
                        validationError = hospitalValidation.errorMessage
                        return@Button
                    }
                    val phoneValidation = ValidationHelper.validatePhone(contactPhone)
                    if (!phoneValidation.isValid) {
                        validationError = phoneValidation.errorMessage
                        return@Button
                    }
                    val unitsValidation = ValidationHelper.validateUnits(unitsRequired)
                    if (!unitsValidation.isValid) {
                        validationError = unitsValidation.errorMessage
                        return@Button
                    }

                    validationError = null
                    isSubmitting = true

                    val cleanPatientName = if (patientName.isNotBlank()) ValidationHelper.sanitizeText(patientName, 60) else "Emergency Patient"
                    val cleanHospitalName = ValidationHelper.sanitizeText(hospitalName, 100)
                    val cleanAddress = ValidationHelper.sanitizeText(hospitalAddress, 150)
                    val cleanPhone = contactPhone.filter { it.isDigit() || it == '+' }.take(15)
                    val cleanNotes = ValidationHelper.sanitizeText(additionalNotes, 300)

                    val newRequest = repository.createEmergencyRequest(
                        patientName = cleanPatientName,
                        bloodGroup = selectedGroup,
                        units = unitsRequired,
                        hospitalName = cleanHospitalName,
                        hospitalAddress = cleanAddress,
                        contactPhone = cleanPhone,
                        urgencyLevel = urgencyLevel,
                        notes = cleanNotes
                    )

                    isSubmitting = false
                    onRequestCreated(newRequest.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                shape = RoundedCornerShape(50.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MedicalWhite, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp), tint = MedicalWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🚨 BROADCAST EMERGENCY SOS NOW",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MedicalWhite
                    )
                }
            }
        }
    }
}
