package com.example.bloodsync_android.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
 * Super Simple, Clean & Eye-Catching Home Dashboard with Modern Pill-Shape Design.
 * Available Donors feed shifted to dedicated directory screen for zero homepage clutter.
 * Strict Palette: RED, GREEN, WHITE, YELLOW ONLY.
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
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDonors: () -> Unit = {}
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

    val firstName = profile.name.trim().split(" ").firstOrNull { it.isNotBlank() }
    val greetingSubtitle = if (!firstName.isNullOrBlank()) {
        "Hello, $firstName • ${profile.bloodGroup.ifBlank { "Voluntary Donor" }}"
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

        // Main Clean Dashboard
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==============================================================
            // 1. EYE-CATCHING EMERGENCY SOS PILL HERO CARD (RED)
            // ==============================================================
            item {
                if (activeEmergencies.isNotEmpty()) {
                    val latestEmg = activeEmergencies.first()
                    ActiveEmergencyPillCard(
                        emergency = latestEmg,
                        onViewDetails = onNavigateToEmergency,
                        onShareWhatsApp = { ShareHelper.shareEmergencySos(context, latestEmg) }
                    )
                } else {
                    EmergencySosPillCard(
                        onRequestBloodClick = onNavigateToEmergency
                    )
                }
            }

            // ==============================================================
            // 2. 4 PILL NEO-ACTION TILES (RED, GREEN, WHITE, YELLOW)
            // ==============================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PillActionTile(
                        title = "Request Blood",
                        subtitle = "Instant SOS Broadcast",
                        icon = Icons.Default.Warning,
                        accentColor = BloodRedPrimary,
                        bgColor = appColors.redLight,
                        onClick = onNavigateToEmergency,
                        modifier = Modifier.weight(1f)
                    )

                    PillActionTile(
                        title = "Find Donors",
                        subtitle = "${donors.size} Donors Active",
                        icon = Icons.Default.PersonSearch,
                        accentColor = StatusEligibleGreen,
                        bgColor = appColors.greenLight,
                        onClick = onNavigateToDonors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PillActionTile(
                        title = "Blood Banks",
                        subtitle = "${bloodBanks.size} Verified Centers",
                        icon = Icons.Default.LocalHospital,
                        accentColor = BloodRedPrimary,
                        bgColor = appColors.redLight,
                        onClick = onNavigateToAppointments,
                        modifier = Modifier.weight(1f)
                    )

                    PillActionTile(
                        title = "Book Slot",
                        subtitle = if (eligibilityResult.daysRemaining > 0) "${eligibilityResult.daysRemaining}d Cooldown" else "Ready to Donate",
                        icon = Icons.Default.CalendarToday,
                        accentColor = StatusWarningAmber,
                        bgColor = appColors.yellowLight,
                        onClick = onNavigateToAppointments,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==============================================================
            // 3. WHO 90-DAY SAFETY DIAL & DONOR METER (PILL CARD)
            // ==============================================================
            item {
                WhoSafetyGaugePillCard(
                    profile = profile,
                    eligibilityStatus = eligibilityResult.status,
                    daysRemaining = eligibilityResult.daysRemaining,
                    onHealthClick = onNavigateToHealth,
                    onDonationsClick = onNavigateToHistory
                )
            }

            // ==============================================================
            // 4. CLEAN DIRECTORY PILL SHORTCUT: ACTIVE DONORS (GREEN)
            // ==============================================================
            item {
                DirectoryPillBanner(
                    badge = "ACTIVE DIRECTORY",
                    title = "Voluntary Donors Online",
                    subtitle = "Browse ${donors.size} registered donors filtered by blood group and city.",
                    icon = Icons.Default.People,
                    actionText = "Browse Donors →",
                    accentColor = StatusEligibleGreen,
                    bgColor = appColors.greenLight,
                    onActionClick = onNavigateToDonors
                )
            }

            // ==============================================================
            // 5. CLEAN DIRECTORY PILL SHORTCUT: CERTIFIED BLOOD BANKS (RED)
            // ==============================================================
            item {
                DirectoryPillBanner(
                    badge = "24/7 BLOOD STOCK",
                    title = "Certified Blood Banks",
                    subtitle = "Verified regional hospital blood storage reserves ready for emergency dispatch.",
                    icon = Icons.Default.LocalHospital,
                    actionText = "View Centers →",
                    accentColor = BloodRedPrimary,
                    bgColor = appColors.redLight,
                    onActionClick = onNavigateToAppointments
                )
            }
        }
    }
}

// =======================================================================
// COMPONENT 1: EMERGENCY SOS PILL HERO CARD (RADAR ANIMATION)
// =======================================================================
@Composable
private fun EmergencySosPillCard(
    onRequestBloodClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "sosPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onRequestBloodClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BloodRedPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Pulse circle glow on top right
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 28.dp, y = (-28).dp)
                    .scale(pulseScale)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        text = "10km Radius",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Need Blood Urgently?",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Broadcast an instant emergency SOS to all matching voluntary donors nearby.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Full Pill Tactile White Button
                Button(
                    onClick = onRequestBloodClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
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
// COMPONENT 2: ACTIVE EMERGENCY HERO PILL CARD
// =======================================================================
@Composable
private fun ActiveEmergencyPillCard(
    emergency: EmergencyRequest,
    onViewDetails: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.redLight),
        border = androidx.compose.foundation.BorderStroke(2.dp, BloodRedPrimary)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                    textColor = Color.White,
                    backgroundColor = BloodRedPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${emergency.unitsRequired} Unit(s) Needed at ${emergency.hospitalName}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = appColors.textPrimary
            )

            if (emergency.hospitalAddress.isNotBlank()) {
                Text(
                    text = emergency.hospitalAddress,
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text("View & Track", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }

                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusEligibleGreen),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// =======================================================================
// COMPONENT 3: PILL NEO-ACTION TILE
// =======================================================================
@Composable
private fun PillActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .border(1.dp, appColors.border, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = appColors.cardBackground,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 14.sp,
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

// =======================================================================
// COMPONENT 4: WHO 90-DAY SAFETY GAUGE PILL CARD
// =======================================================================
@Composable
private fun WhoSafetyGaugePillCard(
    profile: UserProfile,
    eligibilityStatus: EligibilityStatus,
    daysRemaining: Int,
    onHealthClick: () -> Unit,
    onDonationsClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
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
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onHealthClick),
        shape = RoundedCornerShape(28.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Health Donut
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    val trackColor = appColors.border
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 6.dp.toPx()
                        val arcSize = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Background track
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth)
                        )

                        // Colored Arc
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
                            color = StatusWarningAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Middle: Blood Group & Eligibility Title
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = BloodRedPrimary,
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = profile.bloodGroup.ifBlank { "O+" },
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (isEligible) "Ready to Donate" else "$daysRemaining Days to Recovery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = appColors.textPrimary
                        )
                    }

                    Text(
                        text = if (isEligible) "WHO Clinical Safety: Cleared" else "WHO 90-Day Medical Gap Rule",
                        fontSize = 12.sp,
                        color = if (isEligible) StatusEligibleGreen else StatusWarningAmber
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = appColors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 3 Clean Counters (Pill clickable)
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
                    Text("Donations", fontSize = 11.sp, color = appColors.textSecondary)
                }

                Box(modifier = Modifier.height(24.dp).width(1.dp).background(appColors.divider))

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
                    Text("Lives Saved", fontSize = 11.sp, color = appColors.textSecondary)
                }

                Box(modifier = Modifier.height(24.dp).width(1.dp).background(appColors.divider))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onHealthClick)
                ) {
                    Text(
                        text = "90-Day",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = StatusWarningAmber
                    )
                    Text("WHO Interval", fontSize = 11.sp, color = appColors.textSecondary)
                }
            }
        }
    }
}

// =======================================================================
// COMPONENT 5: DIRECTORY PILL BANNER
// =======================================================================
@Composable
private fun DirectoryPillBanner(
    badge: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionText: String,
    accentColor: Color,
    bgColor: Color,
    onActionClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onActionClick),
        shape = RoundedCornerShape(26.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(accentColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = appColors.textSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = actionText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
