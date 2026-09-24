package com.example.bloodsync_android.ui.screens.health

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.bloodsync_android.data.model.EligibilityResult
import com.example.bloodsync_android.data.model.EligibilityStatus
import com.example.bloodsync_android.data.model.HealthRecord
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*

@Composable
fun HealthTrackerScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onBookAppointment: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val healthRecord by repository.healthRecord
    val unreadNotifs by repository.unreadNotificationCount

    // Local state for editing health vitals
    var age by remember { mutableIntStateOf(healthRecord.age) }
    var gender by remember { mutableStateOf(healthRecord.gender) }
    var weightKg by remember { mutableDoubleStateOf(healthRecord.weightKg) }
    var lastDonationDateString by remember { mutableStateOf(healthRecord.lastDonationDateString) }
    var hemoglobinGPerDl by remember { mutableDoubleStateOf(healthRecord.hemoglobinGPerDl) }
    var systolicBp by remember { mutableIntStateOf(healthRecord.systolicBp) }
    var diastolicBp by remember { mutableIntStateOf(healthRecord.diastolicBp) }
    var pulseBpm by remember { mutableIntStateOf(healthRecord.pulseBpm) }

    var hasTattooRecent by remember { mutableStateOf(healthRecord.hasTattooRecent) }
    var hasColdFeverRecent by remember { mutableStateOf(healthRecord.hasColdFeverRecent) }
    var hasAntibioticsRecent by remember { mutableStateOf(healthRecord.hasAntibioticsRecent) }
    var isPregnant by remember { mutableStateOf(healthRecord.isPregnant) }

    var showEditSheet by remember { mutableStateOf(false) }

    // Live computed eligibility
    val currentRecord = remember(
        age, gender, weightKg, lastDonationDateString,
        hemoglobinGPerDl, systolicBp, diastolicBp, pulseBpm,
        hasTattooRecent, hasColdFeverRecent, hasAntibioticsRecent, isPregnant
    ) {
        HealthRecord(
            age = age,
            gender = gender,
            weightKg = weightKg,
            lastDonationDateString = lastDonationDateString,
            hemoglobinGPerDl = hemoglobinGPerDl,
            systolicBp = systolicBp,
            diastolicBp = diastolicBp,
            pulseBpm = pulseBpm,
            hasTattooRecent = hasTattooRecent,
            hasColdFeverRecent = hasColdFeverRecent,
            hasAntibioticsRecent = hasAntibioticsRecent,
            isPregnant = isPregnant
        )
    }

    val eligibilityResult = remember(currentRecord) { currentRecord.calculateEligibility() }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Eligibility & Health",
                subtitle = "Medical Tracker & 90-Day Gap Rules",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick,
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Vitals",
                            tint = BloodRedPrimary
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = BloodSyncTheme.colors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dynamic Eligibility Status Card with Countdown
            item {
                EligibilityStatusBanner(
                    result = eligibilityResult,
                    lastDonationDate = lastDonationDateString,
                    onBookAppointment = onBookAppointment
                )
            }

            // 2. Clinical Vitals Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Donor Vitals",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MedicalTextPrimary
                            )
                            TextButton(
                                onClick = { showEditSheet = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Update", color = BloodRedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VitalItem(
                                title = "Hemoglobin",
                                value = "$hemoglobinGPerDl g/dL",
                                status = if (hemoglobinGPerDl >= 12.5) "Normal" else "Low",
                                isNormal = hemoglobinGPerDl >= 12.5,
                                modifier = Modifier.weight(1f)
                            )
                            VitalItem(
                                title = "Weight",
                                value = "$weightKg kg",
                                status = if (weightKg >= 50.0) "Pass" else "Below min",
                                isNormal = weightKg >= 50.0,
                                modifier = Modifier.weight(1f)
                            )
                            VitalItem(
                                title = "Blood Press.",
                                value = "$systolicBp/$diastolicBp",
                                status = if (systolicBp in 90..140 && diastolicBp in 60..90) "Normal" else "Abnormal",
                                isNormal = systolicBp in 90..140 && diastolicBp in 60..90,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VitalItem(
                                title = "Age",
                                value = "$age yrs",
                                status = if (age in 18..65) "Eligible" else "Ineligible",
                                isNormal = age in 18..65,
                                modifier = Modifier.weight(1f)
                            )
                            VitalItem(
                                title = "Pulse",
                                value = "$pulseBpm bpm",
                                status = if (pulseBpm in 50..100) "Normal" else "Check",
                                isNormal = pulseBpm in 50..100,
                                modifier = Modifier.weight(1f)
                            )
                            VitalItem(
                                title = "Last Donation",
                                value = lastDonationDateString,
                                status = "90d Rule",
                                isNormal = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Medical Checklist & Deferral Screen
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Health Deferral Checklist",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MedicalTextPrimary
                        )
                        Text(
                            text = "Red Cross & WHO donation pre-screening checklist",
                            fontSize = 11.sp,
                            color = MedicalTextSecondary
                        )

                        ChecklistRow(
                            label = "Tattoo or body piercing in last 6 months",
                            isChecked = hasTattooRecent,
                            onToggle = {
                                hasTattooRecent = it
                                repository.updateHealthRecord(currentRecord.copy(hasTattooRecent = it))
                            }
                        )

                        ChecklistRow(
                            label = "Active cold, flu, sore throat, or fever in last 14 days",
                            isChecked = hasColdFeverRecent,
                            onToggle = {
                                hasColdFeverRecent = it
                                repository.updateHealthRecord(currentRecord.copy(hasColdFeverRecent = it))
                            }
                        )

                        ChecklistRow(
                            label = "Antibiotics taken within the past 7 days",
                            isChecked = hasAntibioticsRecent,
                            onToggle = {
                                hasAntibioticsRecent = it
                                repository.updateHealthRecord(currentRecord.copy(hasAntibioticsRecent = it))
                            }
                        )

                        if (gender.equals("Female", ignoreCase = true)) {
                            ChecklistRow(
                                label = "Currently pregnant or within 6 months postpartum",
                                isChecked = isPregnant,
                                onToggle = {
                                    isPregnant = it
                                    repository.updateHealthRecord(currentRecord.copy(isPregnant = it))
                                }
                            )
                        }
                    }
                }
            }

            // 4. Pre-Donation Clinical Health Tips
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = StatusEligibleGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Donor Preparation Guidelines",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF14532D)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        HealthTipItem("💧 Hydrate", "Drink 500ml (16 oz) of water or fruit juice before your donation.")
                        HealthTipItem("🥗 Boost Iron", "Eat iron-rich foods (spinach, beans, eggs, whole grains) 24h prior.")
                        HealthTipItem("🚫 Avoid Fatty Foods", "Avoid fatty foods before donation as they affect blood testing.")
                        HealthTipItem("😴 Rest Well", "Get at least 7-8 hours of sound sleep the night before donation.")
                    }
                }
            }
        }
    }

    // Modal to Edit Vitals
    if (showEditSheet) {
        EditVitalsDialog(
            record = currentRecord,
            onDismiss = { showEditSheet = false },
            onSave = { updated ->
                age = updated.age
                gender = updated.gender
                weightKg = updated.weightKg
                lastDonationDateString = updated.lastDonationDateString
                hemoglobinGPerDl = updated.hemoglobinGPerDl
                systolicBp = updated.systolicBp
                diastolicBp = updated.diastolicBp
                pulseBpm = updated.pulseBpm
                repository.updateHealthRecord(updated)
                showEditSheet = false
            }
        )
    }
}

@Composable
fun EligibilityStatusBanner(
    result: EligibilityResult,
    lastDonationDate: String,
    onBookAppointment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (result.status) {
                EligibilityStatus.ELIGIBLE -> StatusEligibleGreenLight
                EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmberLight
                EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRedLight
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (result.status) {
                EligibilityStatus.ELIGIBLE -> Color(0xFF86EFAC)
                EligibilityStatus.ELIGIBLE_FUTURE -> Color(0xFFFDE68A)
                EligibilityStatus.NOT_ELIGIBLE -> Color(0xFFFECACA)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        when (result.status) {
                            EligibilityStatus.ELIGIBLE -> StatusEligibleGreen
                            EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmber
                            EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRed
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (result.status) {
                        EligibilityStatus.ELIGIBLE -> Icons.Default.Check
                        EligibilityStatus.ELIGIBLE_FUTURE -> Icons.Default.HourglassEmpty
                        EligibilityStatus.NOT_ELIGIBLE -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = MedicalWhite,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (result.status) {
                    EligibilityStatus.ELIGIBLE -> "YOU ARE ELIGIBLE TO DONATE"
                    EligibilityStatus.ELIGIBLE_FUTURE -> "ELIGIBLE ON ${result.nextEligibleDate.uppercase()}"
                    EligibilityStatus.NOT_ELIGIBLE -> "TEMPORARILY INELIGIBLE"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = when (result.status) {
                    EligibilityStatus.ELIGIBLE -> StatusEligibleGreen
                    EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmber
                    EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRed
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (result.status) {
                    EligibilityStatus.ELIGIBLE -> "Your health parameters meet all medical standards. You have satisfied the 3-month gap rule and can donate today."
                    EligibilityStatus.ELIGIBLE_FUTURE -> "3-Month Medical Gap Rule: Donors can only donate once every 3 months (90 days). ${result.daysRemaining} days remaining until your next eligible donation."
                    EligibilityStatus.NOT_ELIGIBLE -> "One or more health vitals or checklist items require attention before donating."
                },
                fontSize = 13.sp,
                color = MedicalTextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (result.status == EligibilityStatus.ELIGIBLE_FUTURE) {
                Spacer(modifier = Modifier.height(12.dp))
                // Progress Bar for 90-day gap
                val progress = ((90 - result.daysRemaining).coerceIn(0, 90) / 90f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = StatusWarningAmber,
                    trackColor = Color(0xFFFEF3C7),
                )
                Text(
                    text = "${90 - result.daysRemaining} of 90 recovery days elapsed",
                    fontSize = 11.sp,
                    color = MedicalTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (result.reasons.isNotEmpty() && result.status == EligibilityStatus.NOT_ELIGIBLE) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MedicalWhite, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Specific Deferral Factors:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StatusUrgentRed
                    )
                    result.reasons.forEach { reason ->
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(StatusUrgentRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = reason, fontSize = 11.sp, color = MedicalTextPrimary)
                        }
                    }
                }
            }

            if (result.status == EligibilityStatus.ELIGIBLE) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onBookAppointment,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusEligibleGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schedule Donation Appointment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VitalItem(
    title: String,
    value: String,
    status: String,
    isNormal: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MedicalSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MedicalBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = MedicalTextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MedicalTextPrimary)
            Text(
                text = status,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isNormal) StatusEligibleGreen else StatusUrgentRed
            )
        }
    }
}

@Composable
fun ChecklistRow(
    label: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle(!isChecked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(checkedColor = BloodRedPrimary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isChecked) StatusUrgentRed else MedicalTextPrimary,
            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 17.sp
        )
    }
}

@Composable
fun HealthTipItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF14532D))
        Text(text = description, fontSize = 11.sp, color = Color(0xFF166534), lineHeight = 16.sp)
    }
}

@Composable
fun EditVitalsDialog(
    record: HealthRecord,
    onDismiss: () -> Unit,
    onSave: (HealthRecord) -> Unit
) {
    var ageText by remember { mutableStateOf("${record.age}") }
    var weightText by remember { mutableStateOf("${record.weightKg}") }
    var hbText by remember { mutableStateOf("${record.hemoglobinGPerDl}") }
    var systolicText by remember { mutableStateOf("${record.systolicBp}") }
    var diastolicText by remember { mutableStateOf("${record.diastolicBp}") }
    var lastDonationText by remember { mutableStateOf(record.lastDonationDateString) }
    var selectedGender by remember { mutableStateOf(record.gender) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Update Donor Vitals", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = { Text("Age (18-65)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hbText,
                        onValueChange = { hbText = it },
                        label = { Text("Hemoglobin (g/dL)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = selectedGender,
                        onValueChange = { selectedGender = it },
                        label = { Text("Gender (M/F)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = systolicText,
                        onValueChange = { systolicText = it },
                        label = { Text("Systolic BP") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = diastolicText,
                        onValueChange = { diastolicText = it },
                        label = { Text("Diastolic BP") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = lastDonationText,
                    onValueChange = { lastDonationText = it },
                    label = { Text("Last Donation (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = record.copy(
                        age = ageText.toIntOrNull() ?: record.age,
                        weightKg = weightText.toDoubleOrNull() ?: record.weightKg,
                        hemoglobinGPerDl = hbText.toDoubleOrNull() ?: record.hemoglobinGPerDl,
                        systolicBp = systolicText.toIntOrNull() ?: record.systolicBp,
                        diastolicBp = diastolicText.toIntOrNull() ?: record.diastolicBp,
                        lastDonationDateString = lastDonationText,
                        gender = selectedGender
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
            ) {
                Text("Recalculate Eligibility")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MedicalTextSecondary)
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = MedicalWhite
    )
}
