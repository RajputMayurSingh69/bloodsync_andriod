package com.bloodsync.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.EmergencyStatus
import com.bloodsync.data.model.UrgencyLevel
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.BloodSyncTopBar
import com.bloodsync.ui.theme.*

@Composable
fun BankDashboardScreen(
    repository: BloodSyncRepository,
    onNavigateToRequests: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val userProfile = repository.userProfile.value
    val unreadNotifs by repository.unreadNotificationCount
    val requests = repository.emergencyRequests
    val bloodStock = repository.bloodStock
    val appointments = repository.appointments
    val donors = repository.donors
    val appColors = BloodSyncTheme.colors

    val activeRequests = remember(requests) {
        requests.filter { it.status != EmergencyStatus.FULFILLED && it.status != EmergencyStatus.CANCELLED }
    }
    val totalStockUnits = remember(bloodStock) { bloodStock.values.sum() }

    // Critical shortage groups (< 35% capacity)
    val criticalGroups = remember(bloodStock) {
        listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-").filter { grp ->
            val units = bloodStock[grp] ?: 0
            val cap = repository.bloodStockCapacities[grp] ?: 100
            (units.toFloat() / cap.toFloat()) <= 0.35f
        }
    }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Blood Bank Portal",
                subtitle = "Central Command & Dispatch",
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = appColors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Welcome Card with Institution Status
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = appColors.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VERIFIED BLOOD BANK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = StatusEligibleGreenLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(StatusEligibleGreen, CircleShape)
                                    )
                                    Text(
                                        text = "LIVE SYSTEM",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusEligibleGreen
                                    )
                                }
                            }
                        }

                        Text(
                            text = userProfile.name.ifBlank { "Central Blood Bank" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = appColors.textPrimary
                        )

                        Text(
                            text = "Monitor live hospital emergencies, allocate blood reserve units, and coordinate voluntary donor appointments.",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 2. Critical Stock Shortage Alert Banner (if any)
            if (criticalGroups.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onNavigateToInventory() },
                        shape = RoundedCornerShape(20.dp),
                        color = BloodRedLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BloodRedPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MedicalWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Critical Supply Shortage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BloodRedPrimary
                                )
                                Text(
                                    text = "Low stock in ${criticalGroups.joinToString(", ")}. Tap to review inventory.",
                                    fontSize = 11.sp,
                                    color = MedicalTextPrimary.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 3. 2x2 Metric Cards Grid (Strict 4-color palette, pill & rounded cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Requests & Stock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UnifiedDashboardCard(
                            modifier = Modifier.weight(1f),
                            title = "Active SOS Requests",
                            value = "${activeRequests.size}",
                            subtitle = if (activeRequests.isNotEmpty()) "Urgent Hospital Need" else "Zero Pending",
                            icon = Icons.Default.Warning,
                            badgeColor = BloodRedPrimary,
                            badgeBg = BloodRedLight,
                            onClick = onNavigateToRequests
                        )

                        UnifiedDashboardCard(
                            modifier = Modifier.weight(1f),
                            title = "Blood Reserve",
                            value = "$totalStockUnits",
                            subtitle = "Units in Stock",
                            icon = Icons.Default.Favorite,
                            badgeColor = StatusEligibleGreen,
                            badgeBg = StatusEligibleGreenLight,
                            onClick = onNavigateToInventory
                        )
                    }

                    // Row 2: Appointments & Donors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UnifiedDashboardCard(
                            modifier = Modifier.weight(1f),
                            title = "Appointments",
                            value = "${appointments.size}",
                            subtitle = "Scheduled Donors",
                            icon = Icons.Default.DateRange,
                            badgeColor = StatusWarningAmber,
                            badgeBg = StatusWarningAmberLight,
                            onClick = onNavigateToAppointments
                        )

                        UnifiedDashboardCard(
                            modifier = Modifier.weight(1f),
                            title = "Donor Registry",
                            value = "${donors.size}",
                            subtitle = "Verified Community",
                            icon = Icons.Default.Person,
                            badgeColor = BloodRedPrimary,
                            badgeBg = BloodRedLight,
                            onClick = null
                        )
                    }
                }
            }

            // 4. Quick Action Pill Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Actions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = appColors.textPrimary
                    )

                    // Primary Action: View Inbound Emergency SOS
                    Button(
                        onClick = onNavigateToRequests,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MedicalWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Emergency SOS Requests (${activeRequests.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedicalWhite
                        )
                    }

                    // Secondary Action: Manage Inventory Matrix
                    OutlinedButton(
                        onClick = onNavigateToInventory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(50.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRedPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Blood Stock & Allocation",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5. Recent Active Emergency Requests (Preview List)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Emergency Requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = appColors.textPrimary
                    )
                    if (activeRequests.isNotEmpty()) {
                        Text(
                            text = "View All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BloodRedPrimary,
                            modifier = Modifier.clickable { onNavigateToRequests() }
                        )
                    }
                }
            }

            if (activeRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusEligibleGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusEligibleGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "All Clear - Zero Active Emergencies",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = "All recent requests have been fulfilled or resolved.",
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                items(activeRequests.take(3), key = { it.id }) { req ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToRequests() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = BloodRedPrimary
                                ) {
                                    Text(
                                        text = req.bloodGroupNeeded,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MedicalWhite
                                    )
                                }
                                Column {
                                    Text(
                                        text = req.patientName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = "${req.unitsRequired} Units • ${req.hospitalName}",
                                        fontSize = 11.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = if (req.urgencyLevel == UrgencyLevel.IMMEDIATE) BloodRedLight else StatusWarningAmberLight
                            ) {
                                Text(
                                    text = req.urgencyLevel.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (req.urgencyLevel == UrgencyLevel.IMMEDIATE) BloodRedPrimary else StatusWarningAmber
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
private fun UnifiedDashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    badgeBg: Color,
    onClick: (() -> Unit)? = null
) {
    val appColors = BloodSyncTheme.colors
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = appColors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = appColors.textPrimary
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = appColors.textMuted
            )
        }
    }
}
