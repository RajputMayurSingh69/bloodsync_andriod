package com.example.bloodsync_android.ui.screens.health

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

    val eligibilityResult = remember(healthRecord) { healthRecord.calculateEligibility() }
    val appColors = BloodSyncTheme.colors

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Eligibility & Health",
                subtitle = "Medical Tracker & 90-Day Gap Rules",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = appColors.background
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
                    lastDonationDate = healthRecord.lastDonationDateString,
                    onBookAppointment = onBookAppointment
                )
            }

            // 2. Medical Checklist & Deferral Screen
            item {
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
                            text = "Health Deferral Checklist",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "Red Cross & WHO donation pre-screening checklist",
                            fontSize = 11.sp,
                            color = appColors.textSecondary
                        )

                        ChecklistRow(
                            label = "Tattoo or body piercing in last 6 months",
                            isChecked = healthRecord.hasTattooRecent,
                            onToggle = {
                                repository.updateHealthRecord(healthRecord.copy(hasTattooRecent = it))
                            }
                        )

                        ChecklistRow(
                            label = "Active cold, flu, sore throat, or fever in last 14 days",
                            isChecked = healthRecord.hasColdFeverRecent,
                            onToggle = {
                                repository.updateHealthRecord(healthRecord.copy(hasColdFeverRecent = it))
                            }
                        )

                        ChecklistRow(
                            label = "Antibiotics taken within the past 7 days",
                            isChecked = healthRecord.hasAntibioticsRecent,
                            onToggle = {
                                repository.updateHealthRecord(healthRecord.copy(hasAntibioticsRecent = it))
                            }
                        )

                        if (healthRecord.gender.equals("Female", ignoreCase = true)) {
                            ChecklistRow(
                                label = "Currently pregnant or within 6 months postpartum",
                                isChecked = healthRecord.isPregnant,
                                onToggle = {
                                    repository.updateHealthRecord(healthRecord.copy(isPregnant = it))
                                }
                            )
                        }
                    }
                }
            }

            // 3. Pre-Donation Clinical Health Tips
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = appColors.greenLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                color = StatusEligibleGreen
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
}

@Composable
fun EligibilityStatusBanner(
    result: EligibilityResult,
    lastDonationDate: String,
    onBookAppointment: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (result.status) {
                EligibilityStatus.ELIGIBLE -> appColors.greenLight
                EligibilityStatus.ELIGIBLE_FUTURE -> appColors.yellowLight
                EligibilityStatus.NOT_ELIGIBLE -> appColors.redLight
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (result.status) {
                EligibilityStatus.ELIGIBLE -> StatusEligibleGreen.copy(alpha = 0.4f)
                EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmber.copy(alpha = 0.4f)
                EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRed.copy(alpha = 0.4f)
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
                    tint = Color.White,
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
                color = appColors.textPrimary,
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
                    trackColor = appColors.yellowLight,
                )
                Text(
                    text = "${90 - result.daysRemaining} of 90 recovery days elapsed",
                    fontSize = 11.sp,
                    color = appColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (result.reasons.isNotEmpty() && result.status == EligibilityStatus.NOT_ELIGIBLE) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appColors.cardBackground, RoundedCornerShape(8.dp))
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
                            Text(text = reason, fontSize = 11.sp, color = appColors.textPrimary)
                        }
                    }
                }
            }

            if (result.status == EligibilityStatus.ELIGIBLE) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onBookAppointment,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusEligibleGreen),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schedule Donation Appointment", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChecklistRow(
    label: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
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
            color = if (isChecked) StatusUrgentRed else appColors.textPrimary,
            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 17.sp
        )
    }
}

@Composable
fun HealthTipItem(title: String, description: String) {
    val appColors = BloodSyncTheme.colors
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusEligibleGreen)
        Text(text = description, fontSize = 11.sp, color = appColors.textSecondary, lineHeight = 16.sp)
    }
}

