package com.bloodsync.ui.screens.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.EligibilityStatus
import com.bloodsync.data.model.EmergencyRequest
import com.bloodsync.data.model.EmergencyStatus
import com.bloodsync.data.model.UserProfile
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.BloodDropIcon
import com.bloodsync.ui.components.BloodSyncTopBar
import com.bloodsync.ui.components.StatusBadge
import com.bloodsync.ui.theme.*
import com.bloodsync.util.ShareHelper

/**
 * BloodSync Home Dashboard
 * Premium modern UI — clean, fast, readable. Medical-grade strict palette.
 * Strict 4-color palette: RED, GREEN, WHITE, YELLOW only.
 */
@Composable
fun HomeScreen(
    repository: BloodSyncRepository,
    onNavigateToEmergency: () -> Unit,
    onNavigateToLiveTracking: (String) -> Unit = {},
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
    val strings = com.bloodsync.util.LocalAppStrings.current
    val profile by repository.userProfile
    val healthRecord by repository.healthRecord
    val eligibilityResult = remember(healthRecord) { healthRecord.calculateEligibility() }
    val unreadNotifs by repository.unreadNotificationCount
    val activeEmergencies = repository.emergencyRequests.filter {
        it.status == EmergencyStatus.BROADCASTING &&
        !it.patientName.contains("Jane Doe", ignoreCase = true) &&
        !it.hospitalName.contains("Metro General", ignoreCase = true) &&
        !it.patientName.equals("any one", ignoreCase = true) &&
        !it.hospitalName.equals("no one", ignoreCase = true) &&
        !it.id.startsWith("emg_dummy")
    }
    val donors = repository.donors
    val bloodBanks = repository.bloodBanks

    val firstName = profile.name.trim().split(" ").firstOrNull { it.isNotBlank() }
    val greetingSubtitle = if (!firstName.isNullOrBlank()) {
        "Hello, $firstName • ${profile.bloodGroup.ifBlank { strings.profile }}"
    } else {
        strings.donorNetworkSubtitle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        BloodSyncTopBar(
            title = strings.appName,
            subtitle = greetingSubtitle,
            unreadCount = unreadNotifs,
            onNotificationClick = onNavigateToNotifications,
            onSettingsClick = onNavigateToSettings
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. EMERGENCY SOS HERO CARD ──────────────────────────────
            item {
                if (activeEmergencies.isNotEmpty()) {
                    val latestEmg = activeEmergencies.first()
                    ActiveEmergencyHeroCard(
                        emergency = latestEmg,
                        onViewDetails = { onNavigateToLiveTracking(latestEmg.id) },
                        onShareWhatsApp = { ShareHelper.shareEmergencySos(context, latestEmg) }
                    )
                } else {
                    EmergencySosHeroCard(onRequestBloodClick = onNavigateToEmergency)
                }
            }

            // ── 2. QUICK ACTION GRID (2×2) ──────────────────────────────
            item {
                QuickActionGrid(
                    donorCount = donors.size,
                    bloodBankCount = bloodBanks.size,
                    daysRemaining = eligibilityResult.daysRemaining,
                    isEligible = eligibilityResult.status == EligibilityStatus.ELIGIBLE,
                    strings = strings,
                    context = context,
                    appColors = appColors,
                    onNavigateToDonors = onNavigateToDonors,
                    onNavigateToAppointments = onNavigateToAppointments
                )
            }

            // ── 3. WHO SAFETY GAUGE ─────────────────────────────────────
            item {
                WhoSafetyGaugeCard(
                    profile = profile,
                    eligibilityStatus = eligibilityResult.status,
                    daysRemaining = eligibilityResult.daysRemaining,
                    onHealthClick = onNavigateToHealth,
                    onDonationsClick = onNavigateToHistory
                )
            }

            // ── 4. DONOR DIRECTORY BANNER ───────────────────────────────
            item {
                DirectoryBanner(
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

            // ── 5. BLOOD BANK BANNER ─────────────────────────────────────
            item {
                DirectoryBanner(
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
// COMPONENT 1: EMERGENCY SOS HERO CARD — Radar pulse + gradient
// =======================================================================
@Composable
private fun EmergencySosHeroCard(onRequestBloodClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")

    val pulse1Scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "p1s"
    )
    val pulse1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "p1a"
    )
    val pulse2Scale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "p2s"
    )
    val pulse2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "p2a"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(0.04f),
                spotColor = BloodRedPrimary.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onRequestBloodClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BloodRedSecondary, BloodRedDark),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // Dual pulse circles top-right
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 35.dp, y = (-35).dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pulse1Scale
                            scaleY = pulse1Scale
                            alpha = pulse1Alpha
                        }
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pulse2Scale
                            scaleY = pulse2Scale
                            alpha = pulse2Alpha
                        }
                        .background(Color.White.copy(alpha = 0.10f), CircleShape)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Live badge
                Surface(
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "24/7 ACTIVE RADAR",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val strings = com.bloodsync.util.LocalAppStrings.current
                Text(
                    text = strings.needBloodUrgently,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.broadcastSosDesc,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // CTA Button (Pill shaped)
                Button(
                    onClick = onRequestBloodClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = BloodRedPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.requestBloodSos,
                        color = BloodRedPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}

// =======================================================================
// COMPONENT 2: ACTIVE EMERGENCY CARD
// =======================================================================
@Composable
private fun ActiveEmergencyHeroCard(
    emergency: EmergencyRequest,
    onViewDetails: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(0.04f),
                spotColor = BloodRedPrimary.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BloodRedSecondary, BloodRedDark)
                    )
                )
        ) {
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
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(7.dp).background(Color.White, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "LIVE BROADCAST",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = "${emergency.bloodGroupNeeded} NEEDED",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "🚨 Live SOS Broadcast Active",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${emergency.unitsRequired} Unit(s) of ${emergency.bloodGroupNeeded} needed at ${emergency.hospitalName}.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onViewDetails,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            "VIEW & TRACK",
                            color = BloodRedPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = onShareWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusEligibleGreen),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.height(48.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// =======================================================================
// COMPONENT 3: QUICK ACTION GRID (2×2)
// =======================================================================
@Composable
private fun QuickActionGrid(
    donorCount: Int,
    bloodBankCount: Int,
    daysRemaining: Int,
    isEligible: Boolean,
    strings: com.bloodsync.util.TranslationStrings,
    context: android.content.Context,
    appColors: com.bloodsync.ui.theme.BloodSyncColors,
    onNavigateToDonors: () -> Unit,
    onNavigateToAppointments: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionTile(
                title = strings.findDonorsTitle,
                count = donorCount,
                suffix = "Donors Online",
                icon = Icons.Default.PersonSearch,
                accentColor = StatusEligibleGreen,
                bgColor = appColors.greenLight,
                onClick = onNavigateToDonors,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                title = strings.bloodBanksTitle,
                count = bloodBankCount,
                suffix = "Centers",
                icon = Icons.Default.LocalHospital,
                accentColor = BloodRedPrimary,
                bgColor = appColors.redLight,
                onClick = onNavigateToAppointments,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionTile(
                title = strings.bookSlotTitle,
                customSubtitle = if (daysRemaining > 0) "$daysRemaining ${strings.waitingDays}" else strings.eligibleNow,
                subtitleColor = if (daysRemaining > 0) StatusWarningAmber else StatusEligibleGreen,
                icon = Icons.Default.CalendarToday,
                accentColor = if (daysRemaining > 0) StatusWarningAmber else StatusEligibleGreen,
                bgColor = if (daysRemaining > 0) appColors.yellowLight else appColors.greenLight,
                onClick = onNavigateToAppointments,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                title = strings.emergencyCall,
                customSubtitle = "Dial 108 Helpline",
                subtitleColor = BloodRedPrimary,
                icon = Icons.Default.PhoneInTalk,
                accentColor = BloodRedPrimary,
                bgColor = appColors.redLight,
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = android.net.Uri.parse("tel:108")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =======================================================================
// COMPONENT 4: QUICK ACTION TILE (24dp card with AnimatedCounter)
// =======================================================================
@Composable
private fun QuickActionTile(
    title: String,
    count: Int? = null,
    suffix: String? = null,
    customSubtitle: String? = null,
    subtitleColor: Color? = null,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileScale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(0.02f),
                spotColor = BloodRedPrimary.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .border(1.dp, appColors.border, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = appColors.cardBackground,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                letterSpacing = (-0.1).sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            if (count != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.bloodsync.ui.components.AnimatedCounter(
                        targetValue = count,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = suffix ?: "",
                        fontSize = 11.sp,
                        color = appColors.textSecondary,
                        maxLines = 1
                    )
                }
            } else {
                Text(
                    text = customSubtitle ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = subtitleColor ?: appColors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// =======================================================================
// COMPONENT 5: WHO SAFETY GAUGE CARD (24dp card, soft red shadow)
// =======================================================================
@Composable
private fun WhoSafetyGaugeCard(
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
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "gaugeAnim"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(0.02f),
                spotColor = BloodRedPrimary.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onHealthClick),
        shape = RoundedCornerShape(24.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Arc Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    val trackColor = appColors.primaryLight
                    val arcColor = if (isEligible) StatusEligibleGreen else StatusWarningAmber
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 7.dp.toPx()
                        val arcSize = size.minDimension - strokeW
                        val topLeft = Offset(strokeW / 2, strokeW / 2)
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeW)
                        )
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    if (isEligible) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Ready",
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(
                            text = "${daysRemaining}d",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = StatusWarningAmber,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = BloodRedPrimary,
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = profile.bloodGroup.ifBlank { "O+" },
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = if (isEligible) "Ready to Donate" else "$daysRemaining Days Left",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = appColors.textPrimary,
                            letterSpacing = (-0.1).sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEligible) "WHO Clinical Safety: Cleared" else "WHO 90-Day Medical Gap Rule",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Stats row with AnimatedCounter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    count = profile.totalDonations,
                    label = "Donations",
                    color = BloodRedPrimary,
                    onClick = onDonationsClick
                )
                Box(modifier = Modifier.height(28.dp).width(1.dp).background(appColors.divider))
                StatItem(
                    count = profile.livesSaved,
                    label = "Lives Saved",
                    color = StatusEligibleGreen,
                    onClick = onDonationsClick
                )
                Box(modifier = Modifier.height(28.dp).width(1.dp).background(appColors.divider))
                StatItemText(
                    text = "90-Day",
                    label = "WHO Interval",
                    color = StatusWarningAmber,
                    onClick = onHealthClick
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        com.bloodsync.ui.components.AnimatedCounter(
            targetValue = count,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = appColors.textSecondary
        )
    }
}

@Composable
private fun StatItemText(
    text: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = (-0.3).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = appColors.textSecondary
        )
    }
}

// =======================================================================
// COMPONENT 6: DIRECTORY BANNER (24dp card, pill button)
// =======================================================================
@Composable
private fun DirectoryBanner(
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
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(0.02f),
                spotColor = BloodRedPrimary.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onActionClick),
        shape = RoundedCornerShape(24.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Badge pill
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(accentColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(bgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                letterSpacing = (-0.1).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = appColors.textSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
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
