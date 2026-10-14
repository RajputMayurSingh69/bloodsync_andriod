package com.bloodsync.ui.screens.history

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.DonationRecord
import com.bloodsync.data.model.DonationStatus
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Redesigned Donation History Screen.
 * Soft Medical Red & White theme with 24dp cards, animated impact counters,
 * vertical timeline history cards, and log donation dialog.
 */
@Composable
fun DonationHistoryScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onViewCertificate: (certificateId: String) -> Unit,
    onNotificationClick: () -> Unit
) {
    val donationHistory = repository.donationHistory
    val profile by repository.userProfile
    val unreadNotifs by repository.unreadNotificationCount

    var selectedDonationForDetails by remember { mutableStateOf<DonationRecord?>(null) }
    var showLogDonationDialog by remember { mutableStateOf(false) }

    val appColors = BloodSyncTheme.colors

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Donation History",
                subtitle = "${donationHistory.size} verified records",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick,
                actions = {
                    IconButton(onClick = { showLogDonationDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Log Donation",
                            tint = BloodRedPrimary
                        )
                    }
                }
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
            // ── Lifetime Impact Summary Card ─────────────────────────────────
            item {
                BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Donor Lifetime Impact",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Blood Group: ${profile.bloodGroup}",
                                fontSize = 12.sp,
                                color = BloodRedPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        BloodSyncButton(
                            text = "+ Log Donation",
                            onClick = { showLogDonationDialog = true },
                            height = 40.dp,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = appColors.border, thickness = 0.75.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        DonationMetricStat(
                            count = profile.totalDonations,
                            suffix = "",
                            label = "Donations"
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(appColors.border)
                        )
                        DonationMetricStat(
                            count = profile.totalDonations * 450,
                            suffix = " ml",
                            label = "Blood Donated"
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(appColors.border)
                        )
                        DonationMetricStat(
                            count = profile.livesSaved,
                            suffix = "",
                            label = "Lives Saved"
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Verified Records Timeline",
                    subtitle = "Complete chronological log"
                )
            }

            if (donationHistory.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Donation Records",
                        message = "You have not logged any blood donations yet. Track your journey and earn milestone certificates.",
                        icon = Icons.Rounded.EventNote,
                        actionText = "Log First Donation",
                        onActionClick = { showLogDonationDialog = true }
                    )
                }
            } else {
                items(donationHistory, key = { it.id }) { record ->
                    ModernDonationHistoryCard(
                        record = record,
                        onClick = { selectedDonationForDetails = record },
                        onCertificateClick = {
                            record.certificateId?.let { certId ->
                                onViewCertificate(certId)
                            }
                        }
                    )
                }
            }
        }
    }

    // Detail Dialog for individual donation record
    selectedDonationForDetails?.let { record ->
        DonationDetailDialog(
            record = record,
            onDismiss = { selectedDonationForDetails = null },
            onViewCertificate = { certId ->
                selectedDonationForDetails = null
                onViewCertificate(certId)
            }
        )
    }

    // Log New Donation Dialog
    if (showLogDonationDialog) {
        LogDonationDialog(
            repository = repository,
            onDismiss = { showLogDonationDialog = false }
        )
    }
}

@Composable
fun ModernDonationHistoryCard(
    record: DonationRecord,
    onClick: () -> Unit,
    onCertificateClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors

    BloodSyncCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(appColors.redLight, CircleShape)
                        .border(1.5.dp, BloodRedPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = record.bloodGroup,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = BloodRedPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = record.hospitalName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = appColors.textPrimary
                    )
                    Text(
                        text = "${record.date} • ${record.unitsDonated} Unit (450ml)",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            StatusPillBadge(
                text = when (record.status) {
                    DonationStatus.VERIFIED -> "Verified"
                    DonationStatus.COMPLETED -> "Completed"
                    DonationStatus.IN_REVIEW -> "In Review"
                },
                backgroundColor = when (record.status) {
                    DonationStatus.VERIFIED -> appColors.greenLight
                    DonationStatus.COMPLETED -> appColors.greenLight
                    DonationStatus.IN_REVIEW -> StatusWarningAmberLight
                },
                contentColor = when (record.status) {
                    DonationStatus.VERIFIED -> StatusEligibleGreen
                    DonationStatus.COMPLETED -> StatusEligibleGreen
                    DonationStatus.IN_REVIEW -> StatusWarningAmber
                },
                showBeacon = false
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = appColors.border, thickness = 0.75.dp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = appColors.textMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = record.location,
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )
            }

            if (record.certificateId != null) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFFFFF8E1),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CertificateGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .clickable(onClick = onCertificateClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = CertificateGoldDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Certificate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CertificateGoldDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonationDetailDialog(
    record: DonationRecord,
    onDismiss: () -> Unit,
    onViewCertificate: (String) -> Unit
) {
    val appColors = BloodSyncTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Donation Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = appColors.textPrimary
                )
                Text(
                    text = "Reference: ${record.id}",
                    fontSize = 11.sp,
                    color = appColors.textMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Hospital", record.hospitalName)
                DetailRow("Location", record.location)
                DetailRow("Date", record.date)
                DetailRow("Blood Group", record.bloodGroup)
                DetailRow("Units Donated", "${record.unitsDonated} Unit (450ml)")
                DetailRow("Donation Type", record.donationType)
                DetailRow("Recorded Hemoglobin", "${record.hemoglobinRecorded} g/dL")
                DetailRow("Blood Pressure", record.bloodPressure)
                DetailRow("Pulse Rate", "${record.pulseRate} bpm")
                DetailRow("Medical Staff", record.doctorOrPhlebotomist)
                DetailRow("Clinical Notes", record.notes)
            }
        },
        confirmButton = {
            if (record.certificateId != null) {
                BloodSyncButton(
                    text = "View Certificate",
                    onClick = { onViewCertificate(record.certificateId) },
                    icon = Icons.Rounded.WorkspacePremium,
                    height = 44.dp,
                    gradient = Brush.horizontalGradient(
                        listOf(Color(0xFFF57F17), Color(0xFFE65100))
                    ),
                    modifier = Modifier.widthIn(max = 170.dp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = appColors.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = appColors.cardBackground
    )
}

@Composable
fun LogDonationDialog(
    repository: BloodSyncRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    var hospitalName by remember { mutableStateOf("Central City Blood Bank") }
    var location by remember { mutableStateOf("Donor Station 3") }
    var selectedUnits by remember { mutableIntStateOf(1) }
    var hemoglobinText by remember { mutableStateOf("14.2") }
    var doctorName by remember { mutableStateOf("Dr. Sarah Vance, MD") }
    var notes by remember { mutableStateOf("Routine voluntary donation. No adverse effects.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(appColors.redLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = BloodRedPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Log Blood Donation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = appColors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BloodSyncTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = "Hospital / Blood Center",
                    leadingIcon = Icons.Rounded.LocalHospital
                )

                BloodSyncTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Ward / Station Location",
                    leadingIcon = Icons.Rounded.LocationOn
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BloodSyncTextField(
                        value = hemoglobinText,
                        onValueChange = { hemoglobinText = it },
                        label = "Hemoglobin (g/dL)",
                        modifier = Modifier.weight(1f)
                    )
                    BloodSyncTextField(
                        value = "$selectedUnits Unit",
                        onValueChange = {},
                        readOnly = true,
                        label = "Units",
                        modifier = Modifier.weight(1f)
                    )
                }

                BloodSyncTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = "Staff / Doctor Name",
                    leadingIcon = Icons.Rounded.Person
                )

                BloodSyncTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Clinical Notes",
                    singleLine = false,
                    leadingIcon = Icons.Rounded.Notes
                )
            }
        },
        confirmButton = {
            BloodSyncButton(
                text = "Save & Verify",
                onClick = {
                    val hb = hemoglobinText.toDoubleOrNull() ?: 14.0
                    repository.addDonationRecord(
                        hospitalName = hospitalName,
                        location = location,
                        bloodGroup = repository.userProfile.value.bloodGroup,
                        units = selectedUnits,
                        donationType = "Whole Blood (450ml)",
                        hemoglobin = hb,
                        doctorName = doctorName,
                        notes = notes
                    )
                    onDismiss()
                },
                height = 46.dp,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = appColors.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = appColors.cardBackground
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    val appColors = BloodSyncTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = appColors.textSecondary,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = appColors.textPrimary,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
fun DonationMetricStat(
    count: Int,
    suffix: String,
    label: String
) {
    val appColors = BloodSyncTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedCounter(
            targetValue = count,
            suffix = suffix,
            fontSize = 18.sp,
            color = BloodRedPrimary,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = appColors.textSecondary
        )
    }
}
