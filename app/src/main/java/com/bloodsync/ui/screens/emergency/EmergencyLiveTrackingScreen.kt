package com.bloodsync.ui.screens.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.EmergencyRequest
import com.bloodsync.data.model.EmergencyResponder
import com.bloodsync.data.model.EmergencyStatus
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*
import com.bloodsync.util.ShareHelper

/**
 * Redesigned Emergency Live Tracking Screen.
 * Soft Medical Red & White aesthetic, 4-stage animated stepper,
 * live radar pulse, live responder cards with circular blood badges & ETA,
 * WhatsApp SOS share, in-place edit dialog, and atomic fulfill/cancel actions.
 */
@Composable
fun EmergencyLiveTrackingScreen(
    repository: BloodSyncRepository,
    requestId: String,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val emergencyRequests = repository.emergencyRequests
    val unreadNotifs by repository.unreadNotificationCount
    var showEditDialog by remember { mutableStateOf(false) }

    val request = remember(emergencyRequests, requestId) {
        if (requestId.isNotBlank()) {
            emergencyRequests.find { it.id == requestId }
        } else {
            emergencyRequests.firstOrNull { 
                it.status == EmergencyStatus.BROADCASTING &&
                !it.patientName.contains("Jane Doe", ignoreCase = true) &&
                !it.hospitalName.contains("Metro General", ignoreCase = true) &&
                !it.patientName.equals("any one", ignoreCase = true) &&
                !it.hospitalName.equals("no one", ignoreCase = true) &&
                !it.id.startsWith("emg_dummy")
            }
        }
    }

    val currentUserId = repository.userProfile.value.id
    val cloudResponders = remember { mutableStateListOf<EmergencyResponder>() }
    var isResponding by remember { mutableStateOf(false) }
    var hasAlreadyResponded by remember { mutableStateOf(false) }

    LaunchedEffect(request?.id) {
        val reqId = request?.id
        if (!reqId.isNullOrBlank()) {
            repository.listenToEmergencyResponders(reqId) { updatedList ->
                cloudResponders.clear()
                cloudResponders.addAll(updatedList)
                hasAlreadyResponded = updatedList.any { 
                    it.id == currentUserId || 
                    (it.phone.isNotBlank() && it.phone == repository.userProfile.value.phone) 
                }
            }
        }
    }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Live SOS Tracking",
                subtitle = "Real-Time Emergency Radar",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = appColors.background
    ) { innerPadding ->
        if (request == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No Active Emergency",
                    message = "There is currently no active emergency SOS broadcast. When a blood emergency is requested, live matching donor statuses will appear here in real time.",
                    icon = Icons.Rounded.Emergency,
                    actionText = "Back to Home",
                    onActionClick = onBackClick
                )
            }
        } else {
            val listState = rememberLazyListState()
            val activeResponders = if (cloudResponders.isNotEmpty()) cloudResponders else request.responders

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── 1. 4-Stage Animated Stepper ──────────────────────────────
                item {
                    EmergencyStatusStepper(
                        status = request.status,
                        respondersCount = activeResponders.size
                    )
                }

                // ── 2. Live Radar Pulse Header Card ──────────────────────────
                item {
                    LiveRadarStatusCard(request = request)
                }

                // ── 3. Summary Stats Row (Animated Counters) ─────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BloodSyncCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            elevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedCounter(
                                    targetValue = request.donorsNotifiedCount.coerceAtLeast(activeResponders.size),
                                    fontSize = 20.sp,
                                    color = BloodRedPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Notified",
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        BloodSyncCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            elevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedCounter(
                                    targetValue = activeResponders.size,
                                    fontSize = 20.sp,
                                    color = StatusEligibleGreen
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Responders",
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        BloodSyncCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            elevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${request.unitsRequired}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = StatusWarningAmber
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${request.bloodGroupNeeded} Units",
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // ── 4. Volunteer Response Action ─────────────────────────────
                item {
                    val isOwner = request.requesterId == currentUserId || 
                        (request.contactPhone.isNotBlank() && request.contactPhone == repository.userProfile.value.phone)
                    val isBroadcasting = request.status == EmergencyStatus.BROADCASTING

                    if (isOwner && isBroadcasting) {
                        BloodSyncOutlinedButton(
                            text = "Cancel Emergency SOS",
                            onClick = {
                                repository.cancelEmergencyRequest(request.id)
                                android.widget.Toast.makeText(context, "Emergency SOS Cancelled", android.widget.Toast.LENGTH_SHORT).show()
                                onBackClick()
                            },
                            icon = Icons.Rounded.Close,
                            textColor = BloodRedPrimary,
                            borderColor = BloodRedPrimary
                        )
                    } else if (hasAlreadyResponded) {
                        BloodSyncCard(
                            backgroundColor = appColors.greenLight,
                            borderColor = StatusEligibleGreen.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(StatusEligibleGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "You Accepted This SOS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = StatusEligibleGreen
                                    )
                                    Text(
                                        text = "The hospital and patient have been notified that you are en route.",
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                        }
                    } else if (isBroadcasting) {
                        BloodSyncButton(
                            text = "🙋 I CAN DONATE (ACCEPT SOS)",
                            onClick = {
                                isResponding = true
                                repository.respondToEmergency(
                                    requestId = request.id,
                                    status = "ACCEPTED",
                                    notes = "Responding voluntarily to emergency broadcast",
                                    onSuccess = {
                                        isResponding = false
                                        hasAlreadyResponded = true
                                    },
                                    onFailure = {
                                        isResponding = false
                                    }
                                )
                            },
                            isLoading = isResponding,
                            icon = Icons.Rounded.VolunteerActivism,
                            height = 54.dp,
                            gradient = Brush.horizontalGradient(
                                listOf(Color(0xFF2E9E5B), Color(0xFF1E7E45))
                            )
                        )
                    }
                }

                // ── 5. Responding Donors Section ──────────────────────────────
                item {
                    SectionHeader(
                        title = "Responding Donors (${activeResponders.size})",
                        subtitle = "Real-time updates via live stream"
                    )
                }

                if (activeResponders.isEmpty()) {
                    item {
                        BloodSyncCard(elevation = 1.dp) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(appColors.redLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = BloodRedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Awaiting first responses... Broadcast sent to nearby registered donors.",
                                    fontSize = 13.sp,
                                    color = appColors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(activeResponders, key = { it.id }) { responder ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(350)) + slideInVertically(tween(350))
                        ) {
                            ModernResponderCard(
                                responder = responder,
                                onCallClick = { callPhone(context, responder.phone) }
                            )
                        }
                    }
                }

                // ── 6. Emergency Details Card ─────────────────────────────────
                item {
                    BloodSyncCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Emergency Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .clickable { showEditDialog = true },
                                color = appColors.redLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit Details",
                                        tint = BloodRedPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Edit",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BloodRedPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = appColors.border, thickness = 0.75.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow("Patient Name", request.patientName, appColors)
                        DetailRow("Hospital", request.hospitalName, appColors)
                        DetailRow("Location", request.hospitalAddress, appColors)
                        DetailRow("Emergency Contact", request.contactPhone, appColors, isPhone = true)

                        if (request.additionalNotes.isNotBlank()) {
                            DetailRow("Clinical Notes", request.additionalNotes, appColors)
                        }
                    }
                }

                // ── 7. Share SOS via WhatsApp ─────────────────────────────────
                item {
                    BloodSyncButton(
                        text = "Share SOS via WhatsApp & Apps",
                        onClick = { ShareHelper.shareEmergencySos(context, request) },
                        icon = Icons.Rounded.Share,
                        height = 52.dp,
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFF25D366), Color(0xFF128C7E))
                        )
                    )
                }

                // ── 8. Fulfill / Cancel Action Row ────────────────────────────
                item {
                    if (request.status != EmergencyStatus.FULFILLED && request.status != EmergencyStatus.CANCELLED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BloodSyncButton(
                                text = "Mark Fulfilled",
                                onClick = { repository.fulfillEmergencyRequest(request.id) },
                                icon = Icons.Rounded.CheckCircle,
                                height = 50.dp,
                                gradient = Brush.horizontalGradient(
                                    listOf(Color(0xFF2E9E5B), Color(0xFF1E7E45))
                                ),
                                modifier = Modifier.weight(1.2f)
                            )

                            BloodSyncOutlinedButton(
                                text = "Cancel",
                                onClick = { repository.cancelEmergencyRequest(request.id) },
                                height = 50.dp,
                                textColor = StatusUrgentRed,
                                borderColor = StatusUrgentRed,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    } else {
                        BloodSyncCard(
                            backgroundColor = appColors.greenLight,
                            borderColor = StatusEligibleGreen.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = StatusEligibleGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "This emergency blood request has been fulfilled. Thank you to all donors!",
                                    color = StatusEligibleGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (showEditDialog && request != null) {
                EditEmergencyDetailsDialog(
                    request = request,
                    onDismiss = { showEditDialog = false },
                    onSave = { pName, hName, hLoc, phone, note ->
                        repository.updateEmergencyRequestDetails(
                            id = request.id,
                            patientName = pName,
                            hospitalName = hName,
                            hospitalAddress = hLoc,
                            contactPhone = phone,
                            notes = note
                        )
                        showEditDialog = false
                    }
                )
            }
        }
    }
}

/**
 * 4-Stage Animated Stepper:
 * Stage 1: BROADCASTING (Radar Active)
 * Stage 2: DONOR MATCHED (Responders count > 0)
 * Stage 3: DONOR EN ROUTE (Donor en route)
 * Stage 4: FULFILLED (Request fulfilled)
 */
@Composable
private fun EmergencyStatusStepper(
    status: EmergencyStatus,
    respondersCount: Int
) {
    val appColors = BloodSyncTheme.colors

    val currentStep = when {
        status == EmergencyStatus.FULFILLED -> 4
        status == EmergencyStatus.CANCELLED -> 1
        respondersCount > 0 -> 3
        else -> 1
    }

    val steps = listOf("Broadcast", "Matched", "En Route", "Fulfilled")

    BloodSyncCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Live Dispatch Status",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, stepName ->
                    val stepIndex = index + 1
                    val isCompleted = stepIndex < currentStep
                    val isCurrent = stepIndex == currentStep

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Circle Indicator
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    when {
                                        isCompleted -> StatusEligibleGreen
                                        isCurrent -> BloodRedPrimary
                                        else -> appColors.surfaceVariant
                                    },
                                    CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isCompleted -> StatusEligibleGreen
                                        isCurrent -> BloodRedPrimary
                                        else -> appColors.border
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "$stepIndex",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else appColors.textMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stepName,
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isCurrent -> BloodRedPrimary
                                isCompleted -> StatusEligibleGreen
                                else -> appColors.textMuted
                            }
                        )
                    }

                    // Connecting Line between steps
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .offset(y = (-10).dp)
                                .background(
                                    if (stepIndex < currentStep) StatusEligibleGreen else appColors.border
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Radar Pulse Card with expanding wave rings.
 */
@Composable
fun LiveRadarStatusCard(request: EmergencyRequest) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarWave"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    val appColors = BloodSyncTheme.colors

    BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp)
            ) {
                // Expanding radar circle
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .graphicsLayer {
                            scaleX = waveScale
                            scaleY = waveScale
                            alpha = waveAlpha
                        }
                        .background(StatusUrgentRed, CircleShape)
                )

                // Center red hub
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(BloodRedGradientStart, BloodRedGradientEnd)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = request.bloodGroupNeeded,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "NEEDED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(StatusUrgentRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE BROADCAST ACTIVE",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = BloodRedPrimary,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = "${request.unitsRequired} unit(s) of ${request.bloodGroupNeeded} blood requested at ${request.hospitalName}",
                fontSize = 13.sp,
                color = appColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Modern Responder Card with circular blood avatar, ETA, and phone call button.
 */
@Composable
private fun ModernResponderCard(
    responder: EmergencyResponder,
    onCallClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors

    BloodSyncCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular Blood Group Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(appColors.redLight, CircleShape)
                        .border(1.5.dp, BloodRedPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = responder.bloodGroup,
                        color = BloodRedPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (responder.name.isNotBlank()) responder.name else "Volunteer Donor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appColors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified",
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "ETA ${responder.etaMinutes} mins • ${"%.1f".format(responder.distanceKm)} km away",
                        fontSize = 12.sp,
                        color = StatusEligibleGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Circular Phone Button
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCallClick),
                color = appColors.greenLight,
                border = BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.35f)),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = "Call Donor",
                        tint = StatusEligibleGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    appColors: BloodSyncColors,
    isPhone: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = appColors.textSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPhone) BloodRedPrimary else appColors.textPrimary
        )
    }
}

private fun callPhone(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:${phoneNumber.replace(" ", "").replace("-", "")}")
    }
    context.startActivity(intent)
}

/**
 * Edit Emergency Details Dialog (restyled with 24dp card & soft medical inputs).
 */
@Composable
fun EditEmergencyDetailsDialog(
    request: EmergencyRequest,
    onDismiss: () -> Unit,
    onSave: (patientName: String, hospitalName: String, hospitalAddress: String, contactPhone: String, notes: String) -> Unit
) {
    var patientName by remember { mutableStateOf(request.patientName) }
    var hospitalName by remember { mutableStateOf(request.hospitalName) }
    var hospitalAddress by remember { mutableStateOf(request.hospitalAddress) }
    var contactPhone by remember { mutableStateOf(request.contactPhone) }
    var notes by remember { mutableStateOf(request.additionalNotes) }
    val appColors = BloodSyncTheme.colors

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
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = BloodRedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Edit Emergency Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = appColors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BloodSyncTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = "Patient Name",
                    leadingIcon = Icons.Rounded.Person
                )

                BloodSyncTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = "Hospital Name *",
                    leadingIcon = Icons.Rounded.LocalHospital
                )

                BloodSyncTextField(
                    value = hospitalAddress,
                    onValueChange = { hospitalAddress = it },
                    label = "Location / Address",
                    leadingIcon = Icons.Rounded.LocationOn
                )

                BloodSyncTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = "Emergency Contact Phone *",
                    leadingIcon = Icons.Rounded.Phone
                )

                BloodSyncTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Clinical Notes / Instructions",
                    singleLine = false,
                    leadingIcon = Icons.Rounded.Notes
                )
            }
        },
        confirmButton = {
            BloodSyncButton(
                text = "Save Changes",
                onClick = {
                    if (hospitalName.isNotBlank() && contactPhone.isNotBlank()) {
                        onSave(patientName, hospitalName, hospitalAddress, contactPhone, notes)
                    }
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
        containerColor = appColors.cardBackground,
        shape = RoundedCornerShape(24.dp)
    )
}
