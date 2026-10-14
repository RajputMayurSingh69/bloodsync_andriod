package com.bloodsync.ui.screens.health

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.EligibilityResult
import com.bloodsync.data.model.EligibilityStatus
import com.bloodsync.data.model.HealthRecord
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*

/**
 * Redesigned Health Tracker & 90-Day Eligibility Screen.
 * Soft Medical Red & White theme, animated WHO circular recovery gauge,
 * green eligible banner / amber cooldown banner, and interactive pre-donation checklist.
 */
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
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = appColors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Dynamic Eligibility Status Card with WHO Circular Gauge ─
            item {
                EligibilityStatusBanner(
                    result = eligibilityResult,
                    lastDonationDate = healthRecord.lastDonationDateString,
                    onBookAppointment = onBookAppointment
                )
            }

            // ── 2. Medical Checklist & Deferral Screen ───────────────────
            item {
                BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(appColors.redLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FactCheck,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Health Deferral Checklist",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "WHO clinical pre-donation assessment",
                                fontSize = 12.sp,
                                color = appColors.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = appColors.border, thickness = 0.75.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    ChecklistRow(
                        label = "Tattoo or body piercing in last 6 months",
                        isChecked = healthRecord.hasTattooRecent,
                        onToggle = {
                            repository.updateHealthRecord(healthRecord.copy(hasTattooRecent = it))
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ChecklistRow(
                        label = "Active cold, flu, sore throat, or fever in last 14 days",
                        isChecked = healthRecord.hasColdFeverRecent,
                        onToggle = {
                            repository.updateHealthRecord(healthRecord.copy(hasColdFeverRecent = it))
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ChecklistRow(
                        label = "Antibiotics taken within the past 7 days",
                        isChecked = healthRecord.hasAntibioticsRecent,
                        onToggle = {
                            repository.updateHealthRecord(healthRecord.copy(hasAntibioticsRecent = it))
                        }
                    )

                    if (healthRecord.gender.equals("Female", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(8.dp))
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

            // ── 3. Pre-Donation Clinical Health Guidelines ───────────────
            item {
                BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(appColors.greenLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HealthAndSafety,
                                contentDescription = null,
                                tint = StatusEligibleGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Clinical Preparation Guidelines",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Recommended medical steps before giving blood",
                                fontSize = 12.sp,
                                color = appColors.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HealthTipItem(
                        icon = "💧",
                        title = "Hydrate Well",
                        description = "Drink 500ml (16 oz) of water or juice 1-2 hours before donation."
                    )
                    HealthTipItem(
                        icon = "🥗",
                        title = "Iron-Rich Foods",
                        description = "Consume spinach, beans, eggs, or lentils to maintain optimal hemoglobin."
                    )
                    HealthTipItem(
                        icon = "🚫",
                        title = "Avoid Fatty Meals",
                        description = "Skip burgers, fried food, or heavy creams as fats interfere with blood screening."
                    )
                    HealthTipItem(
                        icon = "😴",
                        title = "Adequate Sleep",
                        description = "Get at least 7-8 hours of sound sleep the night prior to your appointment."
                    )
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
    val accentColor = when (result.status) {
        EligibilityStatus.ELIGIBLE -> StatusEligibleGreen
        EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmber
        EligibilityStatus.NOT_ELIGIBLE -> BloodRedPrimary
    }
    val containerBg = when (result.status) {
        EligibilityStatus.ELIGIBLE -> appColors.greenLight
        EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmberLight
        EligibilityStatus.NOT_ELIGIBLE -> appColors.redLight
    }

    BloodSyncCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = containerBg,
        borderColor = accentColor.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // WHO Circular Gauge / Status Badge
            if (result.status == EligibilityStatus.ELIGIBLE_FUTURE) {
                val targetProgress = ((90 - result.daysRemaining).coerceIn(0, 90) / 90f)
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    label = "gapProgress"
                )

                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = appColors.border,
                        strokeWidth = 9.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = StatusWarningAmber,
                        strokeWidth = 9.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.daysRemaining}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = StatusWarningAmber
                        )
                        Text(
                            text = "days left",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.textSecondary
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (result.status) {
                            EligibilityStatus.ELIGIBLE -> Icons.Rounded.Check
                            else -> Icons.Rounded.Close
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = when (result.status) {
                    EligibilityStatus.ELIGIBLE -> "ELIGIBLE TO DONATE"
                    EligibilityStatus.ELIGIBLE_FUTURE -> "COOLDOWN: ${result.daysRemaining} DAYS REMAINING"
                    EligibilityStatus.NOT_ELIGIBLE -> "TEMPORARILY INELIGIBLE"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (result.status) {
                    EligibilityStatus.ELIGIBLE -> "Your health parameters meet all medical standards. You have satisfied the 3-month gap rule and can donate blood today."
                    EligibilityStatus.ELIGIBLE_FUTURE -> "WHO 90-Day Medical Gap Rule: Donors can only donate once every 90 days to ensure full red-blood-cell and iron replenishment."
                    EligibilityStatus.NOT_ELIGIBLE -> "One or more health vitals or checklist items require attention before donating."
                },
                fontSize = 13.sp,
                color = appColors.textPrimary,
                modifier = Modifier.padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (result.status == EligibilityStatus.ELIGIBLE_FUTURE) {
                Spacer(modifier = Modifier.height(10.dp))
                StatusPillBadge(
                    text = "Next Eligible Date: ${result.nextEligibleDate}",
                    backgroundColor = appColors.cardBackground,
                    contentColor = StatusWarningAmber,
                    showBeacon = true
                )
            }

            if (result.reasons.isNotEmpty() && result.status == EligibilityStatus.NOT_ELIGIBLE) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appColors.cardBackground, RoundedCornerShape(14.dp))
                        .border(1.dp, appColors.border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Specific Deferral Factors:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BloodRedPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    result.reasons.forEach { reason ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(BloodRedPrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = reason, fontSize = 12.sp, color = appColors.textPrimary)
                        }
                    }
                }
            }

            if (result.status == EligibilityStatus.ELIGIBLE) {
                Spacer(modifier = Modifier.height(18.dp))
                BloodSyncButton(
                    text = "Schedule Donation Appointment",
                    onClick = onBookAppointment,
                    icon = Icons.Rounded.CalendarToday,
                    height = 50.dp,
                    gradient = Brush.horizontalGradient(
                        listOf(Color(0xFF2E9E5B), Color(0xFF1E7E45))
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
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
    val bgCol by animateColorAsState(
        targetValue = if (isChecked) appColors.redLight else Color.Transparent,
        label = "checkBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle(!isChecked) },
        color = bgCol,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isChecked) BloodRedPrimary.copy(alpha = 0.35f) else appColors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = BloodRedPrimary,
                    checkmarkColor = Color.White,
                    uncheckedColor = appColors.border
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (isChecked) BloodRedPrimary else appColors.textPrimary,
                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun HealthTipItem(
    icon: String,
    title: String,
    description: String
) {
    val appColors = BloodSyncTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 20.sp, modifier = Modifier.padding(top = 1.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = appColors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = appColors.textSecondary,
                lineHeight = 17.sp
            )
        }
    }
}
