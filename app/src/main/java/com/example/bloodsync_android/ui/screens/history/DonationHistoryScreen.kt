package com.example.bloodsync_android.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.bloodsync_android.data.model.DonationRecord
import com.example.bloodsync_android.data.model.DonationStatus
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.EmptyStateView
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*

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
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log Donation",
                            tint = BloodRedPrimary
                        )
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Lifetime Impact Summary Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Donor Lifetime Impact",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = "Blood Group: ${profile.bloodGroup}",
                                    fontSize = 12.sp,
                                    color = BloodRedPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { showLogDonationDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                                shape = RoundedCornerShape(50.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Donation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            DonationMetricStat(
                                value = "${profile.totalDonations}",
                                label = "Total Donations"
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(appColors.divider)
                            )
                            DonationMetricStat(
                                value = "${profile.totalDonations * 450} ml",
                                label = "Blood Donated"
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(appColors.divider)
                            )
                            DonationMetricStat(
                                value = "${profile.livesSaved}",
                                label = "Lives Saved"
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Complete Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            }

            if (donationHistory.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No Donation Records",
                        message = "You have not logged any blood donations yet.",
                        icon = Icons.Default.DateRange,
                        actionText = "Log First Donation",
                        onActionClick = { showLogDonationDialog = true }
                    )
                }
            } else {
                items(donationHistory, key = { it.id }) { record ->
                    DonationHistoryCard(
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
fun DonationHistoryCard(
    record: DonationRecord,
    onClick: () -> Unit,
    onCertificateClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(appColors.redLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = record.bloodGroup,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = BloodRedPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = record.hospitalName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "${record.date} • ${record.unitsDonated} Unit (450ml)",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    }
                }

                StatusBadge(
                    text = when (record.status) {
                        DonationStatus.VERIFIED -> "Verified"
                        DonationStatus.COMPLETED -> "Completed"
                        DonationStatus.IN_REVIEW -> "In Review"
                    },
                    textColor = when (record.status) {
                        DonationStatus.VERIFIED -> StatusEligibleGreen
                        DonationStatus.COMPLETED -> StatusEligibleGreen
                        DonationStatus.IN_REVIEW -> StatusWarningAmber
                    },
                    backgroundColor = when (record.status) {
                        DonationStatus.VERIFIED -> appColors.greenLight
                        DonationStatus.COMPLETED -> appColors.greenLight
                        DonationStatus.IN_REVIEW -> appColors.yellowLight
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = appColors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.location,
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }

                if (record.certificateId != null) {
                    TextButton(
                        onClick = onCertificateClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = CertificateGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "View Certificate",
                            fontSize = 12.sp,
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
                    text = "Donation Record Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Ref: ${record.id}",
                    fontSize = 11.sp,
                    color = appColors.textMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("Hospital / Blood Bank", record.hospitalName)
                DetailRow("Location", record.location)
                DetailRow("Date", record.date)
                DetailRow("Blood Group", record.bloodGroup)
                DetailRow("Units Donated", "${record.unitsDonated} Unit (450ml)")
                DetailRow("Donation Type", record.donationType)
                DetailRow("Recorded Hemoglobin", "${record.hemoglobinRecorded} g/dL")
                DetailRow("Blood Pressure", record.bloodPressure)
                DetailRow("Pulse Rate", "${record.pulseRate} bpm")
                DetailRow("Attending Medical Staff", record.doctorOrPhlebotomist)
                DetailRow("Medical Notes", record.notes)
            }
        },
        confirmButton = {
            if (record.certificateId != null) {
                Button(
                    onClick = { onViewCertificate(record.certificateId) },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Certificate", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = appColors.textSecondary)
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = appColors.cardBackground
    )
}

@Composable
fun LogDonationDialog(
    repository: BloodSyncRepository,
    onDismiss: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    var hospitalName by remember { mutableStateOf("Central City Blood Bank") }
    var location by remember { mutableStateOf("Donor Station 3") }
    var selectedUnits by remember { mutableIntStateOf(1) }
    var hemoglobinText by remember { mutableStateOf("14.2") }
    var doctorName by remember { mutableStateOf("Dr. Sarah Vance, MD") }
    var notes by remember { mutableStateOf("Routine voluntary donation. No adverse effects.") }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Blood Donation",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = appColors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = { Text("Hospital / Blood Center") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ward / Station Location") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hemoglobinText,
                        onValueChange = { hemoglobinText = it },
                        label = { Text("Hemoglobin (g/dL)") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = "$selectedUnits Unit",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Units") },
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Staff / Doctor Name") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Clinical Notes") },
                    maxLines = 2,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
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
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text("Log & Generate Certificate", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = appColors.textSecondary)
            }
        },
        shape = RoundedCornerShape(26.dp),
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
    value: String,
    label: String
) {
    val appColors = BloodSyncTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = appColors.textPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = appColors.textSecondary
        )
    }
}
