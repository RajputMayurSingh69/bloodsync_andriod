package com.example.bloodsync_android.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.bloodsync_android.ui.components.BloodDropIcon
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*
import com.example.bloodsync_android.util.ShareHelper

/**
 * Eye-Catching, Clean & Simple Home Dashboard.
 * Strict Palette: RED, GREEN, WHITE, YELLOW ONLY.
 * Features live radar pulse, WHO safety dial, quick blood filters, and 1-tap SOS.
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

    val firstName = profile.name.trim().split(" ").firstOrNull { it.isNotBlank() }
    val greetingSubtitle = if (!firstName.isNullOrBlank()) {
        "Hello, $firstName • Voluntary Donor"
    } else {
        "24/7 Voluntary Blood Donor Network"
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

        // Main Scrollable Area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==============================================================
            // 1. EYE-CATCHING EMERGENCY SOS PULSE BANNER (RED)
            // ==============================================================
            item {
                if (activeEmergencies.isNotEmpty()) {
                    val latestEmg = activeEmergencies.first()
                    ActiveEmergencyPulseCard(
                        emergency = latestEmg,
                        onViewDetails = onNavigateToEmergency,
                        onShareWhatsApp = { ShareHelper.shareEmergencySos(context, latestEmg) }
                    )
                } else {
                    EmergencySosPulseCard(
                        onRequestBloodClick = onNavigateToEmergency
                    )
                }
            }

            // ==============================================================
            // 2. QUICK ACTIONS (STRICT RED, GREEN, WHITE, YELLOW)
            // ==============================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernActionTile(
                        title = "Request Blood",
                        subtitle = "Instant SOS Broadcast",
                        icon = Icons.Default.Warning,
                        accentColor = BloodRedPrimary,
                        bgColor = BloodRedLight,
                        onClick = onNavigateToEmergency,
                        modifier = Modifier.weight(1f)
                    )

                    ModernActionTile(
                        title = "Find Donors",
                        subtitle = "${donors.size} Donors Active",
                        icon = Icons.Default.PersonSearch,
                        accentColor = StatusEligibleGreen,
                        bgColor = StatusEligibleGreenLight,
                        onClick = onNavigateToEmergency,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernActionTile(
                        title = "Blood Banks",
                        subtitle = "24/7 Verified Centers",
                        icon = Icons.Default.LocalHospital,
                        accentColor = BloodRedPrimary,
                        bgColor = BloodRedLight,
                        onClick = onNavigateToAppointments,
                        modifier = Modifier.weight(1f)
                    )

                    ModernActionTile(
                        title = "Book Slot",
                        subtitle = if (eligibilityResult.daysRemaining > 0) "${eligibilityResult.daysRemaining}d Cooldown" else "Ready to Donate",
                        icon = Icons.Default.CalendarToday,
                        accentColor = StatusWarningAmber,
                        bgColor = StatusWarningAmberLight,
                        onClick = onNavigateToAppointments,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==============================================================
            // 3. EYE-CATCHING WHO 90-DAY SAFETY DIAL & DONOR STATUS
            // ==============================================================
            item {
                WhoSafetyGaugeCard(
                    profile = profile,
                    eligibilityStatus = eligibilityResult.status,
                    daysRemaining = eligibilityResult.daysRemaining,
                    onHealthClick = onNavigateToHealth,
                    onDonationsClick = onNavigateToHistory
                )
            }

            // ==============================================================
            // 4. QUICK BLOOD GROUP FILTER CAROUSEL (SPRING ANIMATED PILLS)
            // ==============================================================
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Find Donors by Group",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MedicalTextPrimary
                        )

                        Text(
                            text = "Tap to filter",
                            fontSize = 11.sp,
                            color = MedicalTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val bloodGroups = listOf("All", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bloodGroups) { group ->
                            val isSelected = selectedSearchGroup == group
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "chipScale"
                            )

                            Surface(
                                modifier = Modifier
                                    .scale(scale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedSearchGroup = group }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) BloodRedPrimary else MedicalBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                color = if (isSelected) BloodRedPrimary else MedicalWhite,
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = group,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MedicalWhite else MedicalTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==============================================================
            // 5. MATCHING DONORS FEED (WITH PULSING ACTIVE BEACON)
            // ==============================================================
            val filteredDonors = donors.filter {
                if (selectedSearchGroup == "All") true
                else it.bloodGroup.equals(selectedSearchGroup, ignoreCase = true)
            }

            if (filteredDonors.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MedicalWhite,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(BloodRedLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                BloodDropIcon(size = 28.dp, tint = BloodRedPrimary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No registered donors yet for $selectedSearchGroup",
                                fontWeight = FontWeight.Bold,
                                color = MedicalTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Broadcast an emergency request to alert nearby volunteers instantly.",
                                fontSize = 12.sp,
                                color = MedicalTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToEmergency,
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Broadcast Emergency SOS", color = MedicalWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredDonors) { donor ->
                    LiveDonorFeedCard(
                        donor = donor,
                        onWhatsAppClick = {
                            val msg = "Hello ${donor.name}, I am reaching out from BloodSync App regarding voluntary blood donation (${donor.bloodGroup}). Are you available?"
                            ShareHelper.openWhatsApp(context, donor.phone, msg)
                        },
                        onRequestClick = onNavigateToEmergency
                    )
                }
            }

            // ==============================================================
            // 6. CERTIFIED BLOOD BANKS DIRECTORY
            // ==============================================================
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
                            color = MedicalTextPrimary
                        )
                        Text(
                            text = "24/7 Verified regional blood stock",
                            fontSize = 12.sp,
                            color = MedicalTextSecondary
                        )
                    }

                    TextButton(onClick = onNavigateToAppointments) {
                        Text("View All", color = BloodRedPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(bloodBanks.take(2)) { bank ->
                CertifiedBankFeedCard(
                    bank = bank,
                    onBookClick = onNavigateToAppointments
                )
            }
        }
    }
}

// =======================================================================
// EYE-CATCHING COMPONENT: EMERGENCY SOS PULSE CARD (RADAR ANIMATION)
// =======================================================================
@Composable
private fun EmergencySosPulseCard(
    onRequestBloodClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "sosPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onRequestBloodClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BloodRedPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Pulse circle glow on top right
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .offset(x = 24.dp, y = (-24).dp)
                    .scale(pulseScale)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "24/7 ACTIVE RADAR",
                                color = MedicalWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        text = "10km Radius",
                        color = MedicalWhite.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Need Blood Urgently?",
                    color = MedicalWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Broadcast an instant emergency alert to all matching donors nearby.",
                    color = MedicalWhite.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Tactile White Button
                Button(
                    onClick = onRequestBloodClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalWhite),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = BloodRedPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REQUEST BLOOD (1-TAP SOS)",
                        color = BloodRedPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// =======================================================================
// COMPONENT: ACTIVE EMERGENCY HERO WITH LIVE BEACON
// =======================================================================
@Composable
private fun ActiveEmergencyPulseCard(
    emergency: EmergencyRequest,
    onViewDetails: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BloodRedLight),
        border = androidx.compose.foundation.BorderStroke(2.dp, BloodRedPrimary)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(BloodRedPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE SOS BROADCAST ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = BloodRedPrimary
                    )
                }

                StatusBadge(
                    text = emergency.bloodGroupNeeded,
                    textColor = MedicalWhite,
                    backgroundColor = BloodRedPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${emergency.unitsRequired} Unit(s) Needed at ${emergency.hospitalName}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MedicalTextPrimary
            )

            if (emergency.hospitalAddress.isNotBlank()) {
                Text(
                    text = emergency.hospitalAddress,
                    fontSize = 12.sp,
                    color = MedicalTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("View & Track", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MedicalWhite)
                }

                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusEligibleGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MedicalWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", color = MedicalWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// =======================================================================
// MODERN ACTION TILE (BIG, TACTILE, ENGAGING)
// =======================================================================
@Composable
private fun ModernActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MedicalBorder, RoundedCornerShape(14.dp)),
        color = MedicalWhite,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MedicalTextSecondary,
                maxLines = 1
            )
        }
    }
}

// =======================================================================
// EYE-CATCHING COMPONENT: WHO 90-DAY SAFETY DIAL & DONOR METER
// =======================================================================
@Composable
private fun WhoSafetyGaugeCard(
    profile: UserProfile,
    eligibilityStatus: EligibilityStatus,
    daysRemaining: Int,
    onHealthClick: () -> Unit,
    onDonationsClick: () -> Unit
) {
    val isEligible = eligibilityStatus == EligibilityStatus.ELIGIBLE
    val progressFraction = if (isEligible) 1.0f else ((90 - daysRemaining).coerceAtLeast(0) / 90f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "gaugeAnim"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onHealthClick),
        shape = RoundedCornerShape(16.dp),
        color = MedicalWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Circular Health Dial
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 6.dp.toPx()
                        val arcSize = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Background track
                        drawArc(
                            color = MedicalBorder,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth)
                        )

                        // Dynamic Colored Arc
                        drawArc(
                            color = if (isEligible) StatusEligibleGreen else AlertYellow,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    if (isEligible) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Ready",
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "${daysRemaining}d",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = AlertYellowDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Middle: Blood Group & Eligibility Title
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = BloodRedPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile.bloodGroup.ifBlank { "O+" },
                                    color = MedicalWhite,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEligible) "Ready to Donate" else "$daysRemaining Days to Recovery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MedicalTextPrimary
                        )
                    }

                    Text(
                        text = if (isEligible) "WHO Clinical Safety: Cleared" else "WHO 90-Day Medical Gap Rule",
                        fontSize = 12.sp,
                        color = if (isEligible) StatusEligibleGreen else AlertYellowDark
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MedicalTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MedicalDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 3 Clean Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onDonationsClick)
                ) {
                    Text(
                        text = "${profile.totalDonations}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = BloodRedPrimary
                    )
                    Text("Donations", fontSize = 11.sp, color = MedicalTextSecondary)
                }

                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MedicalDivider))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onDonationsClick)
                ) {
                    Text(
                        text = "${profile.livesSaved}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = StatusEligibleGreen
                    )
                    Text("Lives Saved", fontSize = 11.sp, color = MedicalTextSecondary)
                }

                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MedicalDivider))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onHealthClick)
                ) {
                    Text(
                        text = "90-Day",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = AlertYellowDark
                    )
                    Text("WHO Interval", fontSize = 11.sp, color = MedicalTextSecondary)
                }
            }
        }
    }
}

// =======================================================================
// COMPONENT: LIVE DONOR CARD WITH PULSING ACTIVE BEACON
// =======================================================================
@Composable
private fun LiveDonorFeedCard(
    donor: UserProfile,
    onWhatsAppClick: () -> Unit,
    onRequestClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "donorBeacon")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MedicalWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
        shadowElevation = 1.dp
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
                // Big Red Blood Pill
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(BloodRedPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = donor.bloodGroup,
                        color = MedicalWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (donor.name.isNotBlank()) donor.name else "Volunteer Donor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MedicalTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        // Pulsing Green Active Beacon
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(StatusEligibleGreen.copy(alpha = beaconAlpha), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Active Now • ${if (donor.city.isNotBlank()) donor.city else "Nearby"}",
                            fontSize = 12.sp,
                            color = StatusEligibleGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Round Green Contact Button
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(StatusEligibleGreen, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Contact Donor",
                        tint = MedicalWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Request Action Button (Red)
                Button(
                    onClick = onRequestClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text(
                        text = "Request",
                        fontSize = 12.sp,
                        color = MedicalWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =======================================================================
// COMPONENT: CERTIFIED BLOOD BANK FEED CARD
// =======================================================================
@Composable
private fun CertifiedBankFeedCard(
    bank: BloodBank,
    onBookClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onBookClick),
        shape = RoundedCornerShape(14.dp),
        color = MedicalWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
        shadowElevation = 1.dp
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
                    fontSize = 15.sp,
                    color = MedicalTextPrimary
                )
                Text(
                    text = "${bank.address} • ${bank.distanceKm} km away",
                    fontSize = 12.sp,
                    color = MedicalTextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(StatusEligibleGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "24/7 Verified Blood Storage",
                        fontSize = 11.sp,
                        color = StatusEligibleGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onBookClick,
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Book Slot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedicalWhite)
            }
        }
    }
}
