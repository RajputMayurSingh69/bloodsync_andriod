package com.example.bloodsync_android.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.platform.LocalContext
import com.example.bloodsync_android.data.model.EligibilityStatus
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.*
import com.example.bloodsync_android.ui.theme.*
import com.example.bloodsync_android.util.ShareHelper

@Composable
fun HomeScreen(
    repository: BloodSyncRepository,
    onNavigateToEmergency: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val appColors = BloodSyncTheme.colors
    val context = LocalContext.current
    val profile by repository.userProfile
    val healthRecord by repository.healthRecord
    val eligibilityResult = remember(healthRecord) { healthRecord.calculateEligibility() }
    val unreadNotifs by repository.unreadNotificationCount
    val recentDonations = repository.donationHistory
    val upcomingAppointments = repository.appointments.filter { it.status.name == "UPCOMING" }
    val activeEmergencies = repository.emergencyRequests.filter { it.status.name != "CANCELLED" }
    val donors = repository.donors

    var selectedSearchGroup by remember { mutableStateOf<String?>("All") }
    var donorSearchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "BloodSync",
                subtitle = "Welcome back, ${profile.name.split(" ").firstOrNull() ?: "Donor"}",
                unreadCount = unreadNotifs,
                onNotificationClick = onNavigateToNotifications,
                onSettingsClick = onNavigateToSettings
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
            // 1. Prominent 24/7 Emergency Blood Request Banner
            item {
                EmergencyBannerCard(
                    onTriggerEmergency = onNavigateToEmergency
                )
            }

            // 2. Active Emergency Alert (if any ongoing in network)
            if (activeEmergencies.isNotEmpty()) {
                val latestEmg = activeEmergencies.first()
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToEmergency),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(StatusUrgentRedLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = latestEmg.bloodGroupNeeded,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = StatusUrgentRed
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(
                                        text = latestEmg.urgencyLevel.label,
                                        textColor = MedicalWhite,
                                        backgroundColor = StatusUrgentRed
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = latestEmg.requestedAt,
                                        fontSize = 11.sp,
                                        color = MedicalTextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${latestEmg.hospitalName} • ${latestEmg.unitsRequired} Unit(s)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalTextPrimary
                                )
                                Text(
                                    text = "${latestEmg.donorsNotifiedCount} nearby donors broadcasted",
                                    fontSize = 12.sp,
                                    color = MedicalTextSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    ShareHelper.shareEmergencySos(context, latestEmg)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF25D366), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share SOS to WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MedicalTextMuted
                            )
                        }
                    }
                }
            }

            // 3. User Donor Health & Eligibility Quick Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToHealth),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
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
                                        .size(36.dp)
                                        .background(
                                            when (eligibilityResult.status) {
                                                EligibilityStatus.ELIGIBLE -> StatusEligibleGreenLight
                                                EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmberLight
                                                EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRedLight
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = when (eligibilityResult.status) {
                                            EligibilityStatus.ELIGIBLE -> StatusEligibleGreen
                                            EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmber
                                            EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRed
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Donation Eligibility Status",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MedicalTextPrimary
                                    )
                                    Text(
                                        text = "Based on standard 90-day interval",
                                        fontSize = 11.sp,
                                        color = MedicalTextSecondary
                                    )
                                }
                            }

                            StatusBadge(
                                text = when (eligibilityResult.status) {
                                    EligibilityStatus.ELIGIBLE -> "Eligible Now"
                                    EligibilityStatus.ELIGIBLE_FUTURE -> "${eligibilityResult.daysRemaining}d Left"
                                    EligibilityStatus.NOT_ELIGIBLE -> "Deferred"
                                },
                                textColor = when (eligibilityResult.status) {
                                    EligibilityStatus.ELIGIBLE -> StatusEligibleGreen
                                    EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmber
                                    EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRed
                                },
                                backgroundColor = when (eligibilityResult.status) {
                                    EligibilityStatus.ELIGIBLE -> StatusEligibleGreenLight
                                    EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmberLight
                                    EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRedLight
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MedicalBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Blood Group",
                                    fontSize = 11.sp,
                                    color = MedicalTextSecondary
                                )
                                Text(
                                    text = profile.bloodGroup,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BloodRedPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = "Hemoglobin",
                                    fontSize = 11.sp,
                                    color = MedicalTextSecondary
                                )
                                Text(
                                    text = "${healthRecord.hemoglobinGPerDl} g/dL",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MedicalTextPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = "Blood Pressure",
                                    fontSize = 11.sp,
                                    color = MedicalTextSecondary
                                )
                                Text(
                                    text = "${healthRecord.systolicBp}/${healthRecord.diastolicBp}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MedicalTextPrimary
                                )
                            }

                            Button(
                                onClick = onNavigateToHealth,
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRedLight),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "View Tracker",
                                    color = BloodRedPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Key Metrics Grid (Donations, Lives Saved, Certificates)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Donations",
                        value = "${profile.totalDonations}",
                        subtitle = "Total verified",
                        icon = Icons.Default.Favorite,
                        tintColor = BloodRedPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToHistory)
                    )
                    StatMetricCard(
                        title = "Lives Saved",
                        value = "${profile.livesSaved}",
                        subtitle = "Est. impact (3x)",
                        icon = Icons.Default.Star,
                        tintColor = StatusEligibleGreen,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToHistory)
                    )
                    StatMetricCard(
                        title = "Certificates",
                        value = "${repository.certificates.size}",
                        subtitle = "Issued awards",
                        icon = Icons.Default.Verified,
                        tintColor = CertificateGold,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToCertificates)
                    )
                }
            }

            // 5. Quick Feature Shortcuts (Grid of all 6 features)
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MedicalTextPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "Emergency Request",
                        icon = Icons.Default.Warning,
                        color = BloodRedPrimary,
                        bgColor = BloodRedLight,
                        onClick = onNavigateToEmergency,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Book Slot",
                        icon = Icons.Default.CalendarToday,
                        color = StatusInfoBlue,
                        bgColor = StatusInfoBlueLight,
                        onClick = onNavigateToAppointments,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "Donation History",
                        icon = Icons.Default.DateRange,
                        color = Color(0xFF6B7280),
                        bgColor = MedicalSurfaceVariant,
                        onClick = onNavigateToHistory,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "My Certificate",
                        icon = Icons.Default.CardGiftcard,
                        color = CertificateGold,
                        bgColor = Color(0xFFFEF3C7),
                        onClick = onNavigateToCertificates,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 6. Upcoming Appointment or Quick Booking Prompt
            item {
                if (upcomingAppointments.isNotEmpty()) {
                    val apt = upcomingAppointments.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToAppointments),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = BloodRedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Upcoming Appointment",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MedicalTextPrimary
                                    )
                                }
                                StatusBadge(
                                    text = "Confirmed",
                                    textColor = StatusEligibleGreen,
                                    backgroundColor = StatusEligibleGreenLight
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = apt.bloodBankName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MedicalTextPrimary
                            )
                            Text(
                                text = "Scheduled: ${apt.date} at ${apt.timeSlot}",
                                fontSize = 13.sp,
                                color = MedicalTextSecondary
                            )
                            Text(
                                text = "Booking Ref: ${apt.referenceCode}",
                                fontSize = 11.sp,
                                color = MedicalTextMuted
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToAppointments),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ready to Donate?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MedicalTextPrimary
                                )
                                Text(
                                    text = "Schedule a slot at a blood bank near you in 60 seconds",
                                    fontSize = 12.sp,
                                    color = MedicalTextSecondary
                                )
                            }
                            Button(
                                onClick = onNavigateToAppointments,
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Book Slot", fontSize = 12.sp, color = MedicalWhite)
                            }
                        }
                    }
                }
            }

            // 7. Find Donors Nearby (Real-time Donor Directory Search)
            item {
                Text(
                    text = "Find Matching Donors Nearby",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MedicalTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Instant real-time search across local volunteer network",
                    style = MaterialTheme.typography.bodySmall,
                    color = MedicalTextSecondary
                )
            }

            // Filter pills: All, A+, A-, B+, B-, etc.
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf("All", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
                    items(filterOptions) { filter ->
                        val isSelected = selectedSearchGroup == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSearchGroup = filter }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BloodRedPrimary else MedicalBorder,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            color = if (isSelected) BloodRedLight else MedicalWhite
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) BloodRedPrimary else MedicalTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Donors list from repository
            val filteredDonors = donors.filter {
                val matchesGroup = selectedSearchGroup == "All" || it.bloodGroup == selectedSearchGroup
                val matchesQuery = donorSearchQuery.isBlank() || it.name.contains(donorSearchQuery, ignoreCase = true) || it.city.contains(donorSearchQuery, ignoreCase = true)
                matchesGroup && matchesQuery
            }

            if (filteredDonors.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = appColors.textMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedSearchGroup == "All") "No registered donors yet" else "No registered donors yet for $selectedSearchGroup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Invite voluntary blood donors in your community via WhatsApp.",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { ShareHelper.shareAppInvite(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Invite Donors via WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredDonors) { donor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(BloodRedLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = donor.bloodGroup,
                                        color = BloodRedPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (donor.name.isNotBlank()) donor.name else "Volunteer Donor",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = "${if (donor.city.isNotBlank()) donor.city else "Nearby"} • ${donor.totalDonations} Donations",
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (donor.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val msg = "Hello ${donor.name}, I am reaching out from BloodSync App regarding voluntary blood donation (${donor.bloodGroup})."
                                            ShareHelper.openWhatsApp(context, donor.phone, msg)
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF25D366), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "WhatsApp Donor",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onNavigateToEmergency,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary)
                                ) {
                                    Text(
                                        text = "Request",
                                        fontSize = 12.sp,
                                        color = BloodRedPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MedicalBorder, RoundedCornerShape(12.dp)),
        color = MedicalWhite
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(bgColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MedicalTextPrimary,
                lineHeight = 16.sp
            )
        }
    }
}
