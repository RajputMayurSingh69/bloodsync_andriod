package com.example.bloodsync_android.ui.screens.emergency

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.model.EmergencyRequest
import com.example.bloodsync_android.data.model.EmergencyResponder
import com.example.bloodsync_android.data.model.EmergencyStatus
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*
import com.example.bloodsync_android.util.ShareHelper

@Composable
fun EmergencyLiveTrackingScreen(
    repository: BloodSyncRepository,
    requestId: String,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val context = LocalContext.current
    val emergencyRequests = repository.emergencyRequests
    val unreadNotifs by repository.unreadNotificationCount

    val request = remember(emergencyRequests, requestId) {
        emergencyRequests.find { it.id == requestId } ?: emergencyRequests.firstOrNull()
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
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Emergency request not found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
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
                            color = MedicalTextPrimary
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
                            colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
                        ) {
                            Text(
                                text = "Awaiting first responses... broadcast sent to nearby donors.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MedicalTextSecondary
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
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Emergency Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MedicalTextPrimary
                            )
                            HorizontalDivider(color = MedicalDivider, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Patient", fontSize = 12.sp, color = MedicalTextSecondary)
                                Text(request.patientName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hospital", fontSize = 12.sp, color = MedicalTextSecondary)
                                Text(request.hospitalName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Location", fontSize = 12.sp, color = MedicalTextSecondary)
                                Text(request.hospitalAddress, fontSize = 12.sp, color = MedicalTextSecondary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Emergency Contact", fontSize = 12.sp, color = MedicalTextSecondary)
                                Text(request.contactPhone, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BloodRedPrimary)
                            }

                            if (request.additionalNotes.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Clinical Note", fontSize = 12.sp, color = MedicalTextSecondary)
                                    Text(request.additionalNotes, fontSize = 12.sp, color = MedicalTextPrimary)
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
                                shape = RoundedCornerShape(10.dp)
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
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusUrgentRed)
                            ) {
                                Text("Cancel", color = StatusUrgentRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            color = StatusEligibleGreenLight,
                            shape = RoundedCornerShape(10.dp),
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
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
                // Expanding radar circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(waveScale)
                        .background(StatusUrgentRed.copy(alpha = waveAlpha), CircleShape)
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
                            color = MedicalWhite
                        )
                        Text(
                            text = "NEEDED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedicalWhite
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
                    color = BloodRedDark,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "${request.unitsRequired} unit(s) of ${request.bloodGroupNeeded} blood requested at ${request.hospitalName}",
                fontSize = 13.sp,
                color = MedicalTextSecondary,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
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
                        .background(StatusEligibleGreenLight, CircleShape),
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
                            color = MedicalTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(
                            text = responder.bloodGroup,
                            textColor = BloodRedPrimary,
                            backgroundColor = BloodRedLight
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
                colors = IconButtonDefaults.iconButtonColors(containerColor = BloodRedLight)
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
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
                color = MedicalTextSecondary
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
