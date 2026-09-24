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

@Composable
fun EmergencyRequestScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onRequestCreated: (requestId: String) -> Unit,
    onNotificationClick: () -> Unit
) {
    val unreadNotifs by repository.unreadNotificationCount

    var selectedGroup by remember { mutableStateOf("O-") }
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
                title = "24/7 Emergency Request",
                subtitle = "Urgent Donor Broadcast",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = BloodSyncTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Urgent Header Warning
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StatusUrgentRedLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusUrgentRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Urgent Real-Time Broadcast",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BloodRedDark
                        )
                        Text(
                            text = "Submitting this form immediately pushes high-priority notifications to all verified matching donors within 10km.",
                            fontSize = 11.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }

            // Step 1: Urgency Level Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Urgency Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MedicalTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    UrgencyLevel.entries.forEach { level ->
                        val isSelected = urgencyLevel == level
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { urgencyLevel = level }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(level.badgeColorHex) else MedicalBorder,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            color = if (isSelected) Color(level.badgeColorHex).copy(alpha = 0.08f) else MedicalWhite
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { urgencyLevel = level },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(level.badgeColorHex)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = level.label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MedicalTextPrimary
                                    )
                                }

                                if (level == UrgencyLevel.IMMEDIATE) {
                                    Surface(
                                        color = StatusUrgentRed,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "CRITICAL",
                                            color = MedicalWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Step 2: Blood Group & Units Stepper
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BloodGroupSelector(
                        selectedGroup = selectedGroup,
                        onGroupSelected = { selectedGroup = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
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
                                text = "Standard blood bags (450ml each)",
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
                                enabled = unitsRequired > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }

                            Text(
                                text = "$unitsRequired",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            FilledTonalIconButton(
                                onClick = { if (unitsRequired < 10) unitsRequired++ },
                                enabled = unitsRequired < 10
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }

            // Step 3: Hospital & Contact Information
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
                        text = "Hospital & Patient Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MedicalTextPrimary
                    )

                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name / Ward (Optional)") },
                        placeholder = { Text("e.g. Rahul Sharma / ICU Bed 4", color = MedicalTextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        label = { Text("Hospital Name *") },
                        placeholder = { Text("e.g. AIIMS Trauma Center", color = MedicalTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = BloodRedPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hospitalAddress,
                        onValueChange = { hospitalAddress = it },
                        label = { Text("Hospital Address / Wing") },
                        placeholder = { Text("e.g. Block B, Ring Road", color = MedicalTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MedicalTextMuted)
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
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MedicalTextMuted)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = { Text("Medical Reason / Notes") },
                        placeholder = { Text("e.g. Scheduled emergency surgery, urgently needed", color = MedicalTextMuted) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (validationError != null) {
                Surface(
                    color = StatusUrgentRedLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: "",
                        color = StatusUrgentRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Broadcast Trigger Button
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
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MedicalWhite, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Broadcast Emergency Request Now",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedicalWhite
                    )
                }
            }
        }
    }
}
