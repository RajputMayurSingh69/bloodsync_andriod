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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.model.BloodBank
import com.example.bloodsync_android.data.model.EligibilityStatus
import com.example.bloodsync_android.data.model.EmergencyRequest
import com.example.bloodsync_android.data.model.UserProfile
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*
import com.example.bloodsync_android.util.ShareHelper

/**
 * Ultra-Clean, Professional, Bug-Free Home Dashboard for BloodSync.
 * Designed with modern healthcare aesthetics, zero clutter, high readability,
 * and 100% functional, responsive interactive elements.
 */
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
    val activeEmergencies = repository.emergencyRequests.filter { it.status.name != "CANCELLED" }
    val donors = repository.donors
    val bloodBanks = repository.bloodBanks

    var selectedSearchGroup by remember { mutableStateOf("All") }

    // Friendly, bug-free display name (prevents awkward trailing comma like "Welcome back, ")
    val firstName = profile.name.trim().split(" ").firstOrNull { it.isNotBlank() }
    val greetingSubtitle = if (!firstName.isNullOrBlank()) {
        "Welcome back, $firstName 👋"
    } else {
        "Save lives today • Verified Network"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // Top App Bar
        BloodSyncTopBar(
            title = "BloodSync",
            subtitle = greetingSubtitle,
            unreadCount = unreadNotifs,
            onNotificationClick = onNavigateToNotifications,
            onSettingsClick = onNavigateToSettings
        )

        // Main Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. HERO SECTION: EMERGENCY ALERT OR SOS TRIGGER
            // ==========================================
            item {
                if (activeEmergencies.isNotEmpty()) {
                    val latestEmg = activeEmergencies.first()
                    ActiveEmergencyHeroCard(
                        emergency = latestEmg,
                        onViewDetails = onNavigateToEmergency,
                        onShareWhatsApp = { ShareHelper.shareEmergencySos(context, latestEmg) }
                    )
                } else {
                    EmergencyBroadcastHeroCard(
                        onBroadcastClick = onNavigateToEmergency
                    )
                }
            }

            // ==========================================
            // 2. DONOR IMPACT & ELIGIBILITY STRIP
            // ==========================================
            item {
                DonorImpactCard(
                    profile = profile,
                    eligibilityStatus = eligibilityResult.status,
                    daysRemaining = eligibilityResult.daysRemaining,
                    certificatesCount = repository.certificates.size,
                    onDonationsClick = onNavigateToHistory,
                    onCertificatesClick = onNavigateToCertificates,
                    onHealthClick = onNavigateToHealth
                )
            }

            // ==========================================
            // 3. QUICK SERVICES (Clean 4-Icon Grid)
            // ==========================================
            item {
                Text(
                    text = "Quick Services",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ServiceTile(
                        title = "Emergency SOS",
                        subtitle = "Broadcast urgent need",
                        icon = Icons.Default.Warning,
                        iconTint = BloodRedPrimary,
                        bgColor = BloodRedLight,
                        onClick = onNavigateToEmergency,
                        modifier = Modifier.weight(1f)
                    )
                    ServiceTile(
                        title = "Book Slot",
                        subtitle = "Schedule donation",
                        icon = Icons.Default.CalendarToday,
                        iconTint = StatusInfoBlue,
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
                    ServiceTile(
                        title = "Donation History",
                        subtitle = "Verified donor records",
                        icon = Icons.Default.DateRange,
                        iconTint = Color(0xFF6B7280),
                        bgColor = MedicalSurfaceVariant,
                        onClick = onNavigateToHistory,
                        modifier = Modifier.weight(1f)
                    )
                    ServiceTile(
                        title = "Certificates",
                        subtitle = "Recognition awards",
                        icon = Icons.Default.CardGiftcard,
                        iconTint = CertificateGold,
                        bgColor = Color(0xFFFEF3C7),
                        onClick = onNavigateToCertificates,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==========================================
            // 4. VERIFIED COMMUNITY DONORS
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Matching Donors Nearby",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "Connect with active voluntary donors in real-time",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    }

                    Surface(
                        color = BloodRedLight,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${donors.size} Active",
                            color = BloodRedPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Blood Group Filter Pills
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf("All", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
                    items(filterOptions) { filter ->
                        val isSelected = selectedSearchGroup == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedSearchGroup = filter }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BloodRedPrimary else appColors.border,
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            color = if (isSelected) BloodRedPrimary else appColors.cardBackground
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.White else appColors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Filtered Donors List
            val filteredDonors = donors.filter {
                selectedSearchGroup == "All" || it.bloodGroup.equals(selectedSearchGroup, ignoreCase = true)
            }

            if (filteredDonors.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = appColors.cardBackground,
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
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No registered donors yet for $selectedSearchGroup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Invite voluntary blood donors in your area via WhatsApp.",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { ShareHelper.shareAppInvite(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Invite on WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredDonors) { donor ->
                    DonorListItemCard(
                        donor = donor,
                        onWhatsAppClick = {
                            val msg = "Hello ${donor.name}, I am reaching out from BloodSync App regarding urgent voluntary blood donation (${donor.bloodGroup}). Are you available?"
                            ShareHelper.openWhatsApp(context, donor.phone, msg)
                        },
                        onRequestClick = onNavigateToEmergency
                    )
                }
            }

            // ==========================================
            // 5. CERTIFIED REGIONAL BLOOD BANKS
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Certified Blood Banks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "Verified regional centers with 24/7 blood storage",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    }

                    TextButton(onClick = onNavigateToAppointments) {
                        Text("View All", color = BloodRedPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items(bloodBanks.take(2)) { bank ->
                BloodBankQuickCard(
                    bank = bank,
                    onBookClick = onNavigateToAppointments
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: Active Emergency Hero Card
// ==========================================
@Composable
private fun ActiveEmergencyHeroCard(
    emergency: EmergencyRequest,
    onViewDetails: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
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
                            .size(10.dp)
                            .background(StatusUrgentRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE EMERGENCY SOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = StatusUrgentRed,
                        letterSpacing = 0.5.sp
                    )
                }

                StatusBadge(
                    text = emergency.urgencyLevel.label,
                    textColor = Color.White,
                    backgroundColor = StatusUrgentRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(StatusUrgentRedLight, CircleShape)
                        .border(1.5.dp, StatusUrgentRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emergency.bloodGroupNeeded,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = StatusUrgentRed
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = emergency.hospitalName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "${emergency.unitsRequired} Unit(s) Needed • ${emergency.requestedAt}",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                    if (emergency.hospitalAddress.isNotBlank()) {
                        Text(
                            text = emergency.hospitalAddress,
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("Respond / Track", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: Clean Emergency Broadcast Card (When no active emergencies)
// ==========================================
@Composable
private fun EmergencyBroadcastHeroCard(
    onBroadcastClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onBroadcastClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "24/7 EMERGENCY BLOOD SOS",
                            color = Color(0xFFFECACA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Need Blood Urgently?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Broadcast live request to all verified donors nearby instantly",
                        color = Color(0xFFFEE2E2),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    onClick = onBroadcastClick,
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Broadcast",
                        color = BloodRedPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: Donor Impact & Status Strip
// ==========================================
@Composable
private fun DonorImpactCard(
    profile: UserProfile,
    eligibilityStatus: EligibilityStatus,
    daysRemaining: Int,
    certificatesCount: Int,
    onDonationsClick: () -> Unit,
    onCertificatesClick: () -> Unit,
    onHealthClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Blood Group + Eligibility Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(BloodRedLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.bloodGroup.ifBlank { "O+" },
                            color = BloodRedPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Registered Blood Group",
                            fontSize = 11.sp,
                            color = appColors.textMuted
                        )
                        Text(
                            text = "Group ${profile.bloodGroup.ifBlank { "O+" }} Donor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = appColors.textPrimary
                        )
                    }
                }

                // Eligibility Badge
                StatusBadge(
                    text = when (eligibilityStatus) {
                        EligibilityStatus.ELIGIBLE -> "● Eligible to Donate"
                        EligibilityStatus.ELIGIBLE_FUTURE -> "● ${daysRemaining}d to Recovery"
                        EligibilityStatus.NOT_ELIGIBLE -> "● Deferred"
                    },
                    textColor = when (eligibilityStatus) {
                        EligibilityStatus.ELIGIBLE -> StatusEligibleGreen
                        EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmber
                        EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRed
                    },
                    backgroundColor = when (eligibilityStatus) {
                        EligibilityStatus.ELIGIBLE -> StatusEligibleGreenLight
                        EligibilityStatus.ELIGIBLE_FUTURE -> StatusWarningAmberLight
                        EligibilityStatus.NOT_ELIGIBLE -> StatusUrgentRedLight
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // 3-Metric Counter Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Donations
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDonationsClick)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${profile.totalDonations}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BloodRedPrimary
                    )
                    Text(
                        text = "Donations",
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(appColors.divider)
                )

                // Metric 2: Lives Saved
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDonationsClick)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${profile.livesSaved}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusEligibleGreen
                    )
                    Text(
                        text = "Lives Saved",
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(appColors.divider)
                )

                // Metric 3: Certificates
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCertificatesClick)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$certificatesCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CertificateGold
                    )
                    Text(
                        text = "Certificates",
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct link to Health Tracker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onHealthClick)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View Health Vitals & 90-Day Tracker",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BloodRedPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BloodRedPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: Quick Service Tile (2x2 Grid)
// ==========================================
@Composable
private fun ServiceTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, appColors.border, RoundedCornerShape(14.dp)),
        color = appColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = appColors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: Community Donor List Item
// ==========================================
@Composable
private fun DonorListItemCard(
    donor: UserProfile,
    onWhatsAppClick: () -> Unit,
    onRequestClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BloodRedLight, CircleShape)
                        .border(1.dp, BloodRedContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = donor.bloodGroup,
                        color = BloodRedPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (donor.name.isNotBlank()) donor.name else "Volunteer Donor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appColors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Donor",
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "${if (donor.city.isNotBlank()) donor.city else "Nearby"} • ${donor.totalDonations} Donations",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // One-Tap WhatsApp Action
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF25D366), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "WhatsApp Donor",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Request Action
                OutlinedButton(
                    onClick = onRequestClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary)
                ) {
                    Text(
                        text = "Request",
                        fontSize = 12.sp,
                        color = BloodRedPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: Certified Blood Bank Quick Card
// ==========================================
@Composable
private fun BloodBankQuickCard(
    bank: BloodBank,
    onBookClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onBookClick),
        shape = RoundedCornerShape(14.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bank.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = appColors.textPrimary
                )
                Text(
                    text = "${bank.address} • ${bank.distanceKm} km away",
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )
                Text(
                    text = "Hours: ${bank.openHours} • ${bank.bloodStockStatus}",
                    fontSize = 11.sp,
                    color = StatusEligibleGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onBookClick,
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Book Slot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
