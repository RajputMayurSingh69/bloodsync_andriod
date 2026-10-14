package com.bloodsync.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bloodsync.data.model.EmergencyRequest
import com.bloodsync.ui.theme.*

/**
 * High-Urgency Root Emergency Popup Dialog for the Blood Bank Portal.
 *
 * Characteristics:
 * - Rendered at the Root Composable level so it appears over EVERY screen.
 * - Non-dismissible by back-press or outside tap.
 * - Displays blood group, units, patient, hospital, urgency, and multiple count badge.
 * - Features pulsing siren beacon animation and high-contrast red emergency styling.
 * - Clicking "View Emergency Request" stops the siren, dismisses the dialog,
 *   and navigates to the request with a glowing border.
 */
@Composable
fun EmergencyAlertPopup(
    request: EmergencyRequest?,
    totalUnseenCount: Int,
    onViewEmergencyRequest: (String) -> Unit
) {
    if (request == null) return

    val appColors = BloodSyncTheme.colors

    // Intercept hardware/gesture Back button so user cannot accidentally dismiss
    BackHandler(enabled = true) {
        // Intentionally no-op: popup remains until user taps "View Emergency Request"
    }

    // Pulsing siren animation
    val infiniteTransition = rememberInfiniteTransition(label = "sirenPulse")
    val beaconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconScale"
    )
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Dialog(
        onDismissRequest = {
            // Intentionally no-op: non-dismissible
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = appColors.cardBackground,
                border = BorderStroke(2.dp, BloodRedPrimary.copy(alpha = beaconAlpha)),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Red Emergency Header with Siren Icon & Beacon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFDC2626),
                                        Color(0xFF991B1B)
                                    )
                                )
                            )
                            .padding(vertical = 18.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pulsing Siren Badge
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .graphicsLayer {
                                            scaleX = beaconScale
                                            scaleY = beaconScale
                                            alpha = beaconAlpha * 0.45f
                                        }
                                        .background(Color.White, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = BloodRedPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Text(
                                text = "🚨 EMERGENCY BLOOD REQUEST",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )

                            if (totalUnseenCount > 1) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = "⚠️ $totalUnseenCount Pending Emergency Requests!",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Request Details Card Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Blood Group & Units Hero Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(appColors.surfaceVariant, RoundedCornerShape(16.dp))
                                .border(1.dp, appColors.border, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = BloodRedPrimary
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = request.bloodGroupNeeded,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 22.sp
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "${request.unitsRequired} Units Required",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = BloodRedPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Patient: ${request.patientName}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = appColors.textPrimary
                                    )
                                }
                            }

                            // Urgency Badge
                            Surface(
                                color = appColors.redLight,
                                shape = RoundedCornerShape(50.dp),
                                border = BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = request.urgencyLevel.name,
                                    color = BloodRedPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Hospital & Location Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(appColors.redLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = null,
                                    tint = BloodRedPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = request.hospitalName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = appColors.textPrimary
                                )
                                if (request.hospitalAddress.isNotBlank()) {
                                    Text(
                                        text = request.hospitalAddress,
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                        }

                        // Time & Contact Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = appColors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Broadcasted: Just now",
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (request.contactPhone.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = StatusEligibleGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = request.contactPhone,
                                        fontSize = 12.sp,
                                        color = appColors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Primary CTA Button: "View Emergency Request"
                        Button(
                            onClick = {
                                onViewEmergencyRequest(request.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Emergency,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "View Emergency Request",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
