package com.bloodsync.ui.screens.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.bloodsync.ui.components.BloodSyncTopBar
import com.bloodsync.ui.components.StatusBadge
import com.bloodsync.ui.theme.*
import com.bloodsync.util.ShareHelper

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

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Live Emergency Status",
                subtitle = "Broadcasting matching donors",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = BloodSyncTheme.colors.background
    ) { innerPadding ->
        if (request == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(appColors.redLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "No Active Emergency",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "There is currently no active emergency SOS broadcast. When a blood emergency is requested, live matching donor statuses will appear here in real time.",
                            fontSize = 13.sp,
                            color = appColors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Home", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 140.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Radar Pulse Header Card
                item {
                    LiveRadarStatusCard(request = request)
                }

                // Summary Stats Grid (Notified, Responded, ETA)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiveStatCard(
                            value = "${request.donorsNotifiedCount}",
                            label = "Donors Notified",
                            color = BloodRedPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        LiveStatCard(
                            value = "${request.responders.size}",
                            label = "Responders",
                            color = StatusEligibleGreen,
                            modifier = Modifier.weight(1f)
                        )
                        LiveStatCard(
                            value = "${request.unitsRequired} Units",
                            label = "Needed: ${request.bloodGroupNeeded}",
                            color = StatusWarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Responders List Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Responding Donors (${request.responders.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "Real-time updates",
                            fontSize = 11.sp,
                            color = StatusEligibleGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (request.responders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                        ) {
                            Text(
                                text = "Awaiting first responses... broadcast sent to nearby donors.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                } else {
                    items(request.responders, key = { it.id }) { responder ->
                        ResponderCard(
                            responder = responder,
                            onCallClick = {
                                callPhone(context, responder.phone)
                            }
                        )
                    }
                }

                // Hospital & Request Details Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Emergency Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = appColors.textPrimary
                                )

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .clickable { showEditDialog = true },
                                    color = appColors.redLight
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Details",
                                            tint = BloodRedPrimary,
                                            modifier = Modifier.size(13.dp)
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
                            HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Patient", fontSize = 12.sp, color = appColors.textSecondary)
                                Text(request.patientName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hospital", fontSize = 12.sp, color = appColors.textSecondary)
                                Text(request.hospitalName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Location", fontSize = 12.sp, color = appColors.textSecondary)
                                Text(request.hospitalAddress, fontSize = 12.sp, color = appColors.textSecondary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Emergency Contact", fontSize = 12.sp, color = appColors.textSecondary)
                                Text(request.contactPhone, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BloodRedPrimary)
                            }

                            if (request.additionalNotes.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Clinical Note", fontSize = 12.sp, color = appColors.textSecondary)
                                    Text(request.additionalNotes, fontSize = 12.sp, color = appColors.textPrimary)
                                }
                            }
                        }
                    }
                }

                // Share SOS on WhatsApp & Communication Apps
                item {
                    Button(
                        onClick = {
                            ShareHelper.shareEmergencySos(context, request)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusEligibleGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share SOS",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share SOS via WhatsApp & Apps",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }

                // Actions: Fulfill / Close / Cancel
                item {
                    if (request.status != EmergencyStatus.FULFILLED && request.status != EmergencyStatus.CANCELLED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    repository.fulfillEmergencyRequest(request.id)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusEligibleGreen),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark as Fulfilled", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    repository.cancelEmergencyRequest(request.id)
                                },
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(50.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusUrgentRed)
                            ) {
                                Text("Cancel", color = StatusUrgentRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            color = appColors.greenLight,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusEligibleGreen)
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
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    val appColors = BloodSyncTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(90.dp)
            ) {
                // Expanding radar circle (RenderThread GPU animation - zero recomposition)
                Box(
                    modifier = Modifier
                        .size(80.dp)
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
                        .size(60.dp)
                        .background(BloodRedPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = request.bloodGroupNeeded,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "NEEDED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    letterSpacing = 1.sp
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

@Composable
fun ResponderCard(
    responder: EmergencyResponder,
    onCallClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(appColors.greenLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = StatusEligibleGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = responder.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appColors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(
                            text = responder.bloodGroup,
                            textColor = BloodRedPrimary,
                            backgroundColor = appColors.redLight
                        )
                    }
                    Text(
                        text = "${responder.status} • ETA ${responder.etaMinutes} mins (${responder.distanceKm} km)",
                        fontSize = 12.sp,
                        color = StatusEligibleGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onCallClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = appColors.redLight)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call Donor",
                    tint = BloodRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun LiveStatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = appColors.textSecondary
            )
        }
    }
}

private fun callPhone(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:${phoneNumber.replace(" ", "").replace("-", "")}")
    }
    context.startActivity(intent)
}

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
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = BloodRedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = { Text("Hospital Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hospitalAddress,
                    onValueChange = { hospitalAddress = it },
                    label = { Text("Location / Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Emergency Contact Phone *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Clinical Note / Additional Info") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hospitalName.isNotBlank() && contactPhone.isNotBlank()) {
                        onSave(patientName, hospitalName, hospitalAddress, contactPhone, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = appColors.textSecondary)
            }
        },
        containerColor = appColors.cardBackground,
        shape = RoundedCornerShape(24.dp)
    )
}

