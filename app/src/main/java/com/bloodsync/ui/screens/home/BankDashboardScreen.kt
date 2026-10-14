package com.bloodsync.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
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
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*

/**
 * Clean, simplified, and box-styled Blood Bank Portal.
 * Features:
 * - Prominent Government Clinical License number display with instant edit/verification dialog
 * - Modular box-based hierarchy (Facility command box, 2x2 metric KPI boxes, 4x2 blood stock snapshot boxes, 2x2 quick action boxes, and recent alerts box)
 * - Clean borders, crisp typography, and high accessibility
 */
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

    // Effective license number: pulls from profile, address tag (Lic: ...), or saved prefs
    val effectiveLicense = remember(userProfile.licenseNumber, userProfile.address) {
        repository.getEffectiveLicenseNumber()
    }

    var showEditLicenseDialog by remember { mutableStateOf(false) }
    var editLicenseInput by remember { mutableStateOf("") }

    val activeRequests = remember(requests) {
        requests.filter { it.status != EmergencyStatus.FULFILLED && it.status != EmergencyStatus.CANCELLED }
    }
    val totalStockUnits = remember(bloodStock) { bloodStock.values.sum() }

    val allGroups = remember { listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-") }

    // Critical shortage groups (< 35% capacity)
    val criticalGroups = remember(bloodStock) {
        allGroups.filter { grp ->
            val units = bloodStock[grp] ?: 0
            val cap = repository.bloodStockCapacities[grp] ?: 100
            (units.toFloat() / cap.toFloat()) <= 0.35f
        }
    }

    // Pulse animation for LIVE SYSTEM beacon
    val pulseTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

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
            // ========================================================
            // 1. BOX: FACILITY STATUS & PROMINENT LICENSE NUMBER
            // ========================================================
            item {
                BloodSyncCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = appColors.border
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top Badges Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusEligibleGreenLight,
                                border = BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusEligibleGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "VERIFIED BLOOD BANK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusEligibleGreen
                                    )
                                }
                            }

                            // Pulsing LIVE SYSTEM Beacon Box
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = StatusEligibleGreenLight,
                                border = BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((10 * pulseScale).dp)
                                                .background(
                                                    StatusEligibleGreen.copy(alpha = pulseAlpha),
                                                    CircleShape
                                                )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(StatusEligibleGreen, CircleShape)
                                        )
                                    }
                                    Text(
                                        text = "LIVE SYSTEM",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusEligibleGreen
                                    )
                                }
                            }
                        }

                        // Facility Name
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = userProfile.name.ifBlank { "Central Regional Blood Bank" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Authorized Clinical Establishment & Blood Transfusion Unit",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }

                        // DEDICATED PROMINENT LICENSE NUMBER BOX
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (effectiveLicense.isNotBlank()) BloodRedLight else StatusWarningAmberLight,
                            border = BorderStroke(
                                1.dp,
                                if (effectiveLicense.isNotBlank()) BloodRedPrimary.copy(alpha = 0.4f) else StatusWarningAmber.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editLicenseInput = effectiveLicense
                                    showEditLicenseDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (effectiveLicense.isNotBlank()) BloodRedPrimary else StatusWarningAmber,
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = "Clinical License",
                                            tint = MedicalWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "GOVERNMENT CLINICAL LICENSE",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (effectiveLicense.isNotBlank()) BloodRedPrimary else StatusWarningAmber
                                        )
                                        Text(
                                            text = if (effectiveLicense.isNotBlank()) effectiveLicense else "No License Added (Tap to Add)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = appColors.textPrimary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (effectiveLicense.isNotBlank()) BloodRedPrimary.copy(alpha = 0.12f) else StatusWarningAmber.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = if (effectiveLicense.isNotBlank()) BloodRedPrimary else StatusWarningAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (effectiveLicense.isNotBlank()) "Edit" else "Add",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (effectiveLicense.isNotBlank()) BloodRedPrimary else StatusWarningAmber
                                        )
                                    }
                                }
                            }
                        }

                        // Mini-Info Boxes Row (City, Phone, Hours)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FacilityInfoMiniBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.LocationOn,
                                title = "CITY",
                                value = userProfile.city.ifBlank { "National Hub" }
                            )
                            FacilityInfoMiniBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Phone,
                                title = "HELPLINE",
                                value = userProfile.phone.ifBlank { "24/7 Desk" }
                            )
                            FacilityInfoMiniBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.AccessTime,
                                title = "DISPATCH",
                                value = "24/7 Active"
                            )
                        }
                    }
                }
            }

            // ========================================================
            // 2. CRITICAL SUPPLY SHORTAGE ALERT (Conditional Box)
            // ========================================================
            if (criticalGroups.isNotEmpty()) {
                item {
                    BloodSyncCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToInventory() },
                        backgroundColor = BloodRedLight,
                        borderColor = BloodRedPrimary.copy(alpha = 0.35f)
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BloodRedPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MedicalWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Critical Supply Shortage (${criticalGroups.size} Groups)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = BloodRedPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Low reserves in ${criticalGroups.joinToString(", ")}. Tap to review stock.",
                                    fontSize = 11.5.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 15.sp
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

            // ========================================================
            // 3. BOX: 2x2 CORE METRICS (Simple Box Tiles)
            // ========================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PORTAL OVERVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = appColors.textMuted,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SimpleMetricBox(
                            modifier = Modifier.weight(1f),
                            title = "Active SOS",
                            count = activeRequests.size,
                            subtitle = if (activeRequests.isNotEmpty()) "Urgent alerts" else "Zero pending",
                            icon = Icons.Default.Warning,
                            accentColor = BloodRedPrimary,
                            bgColor = BloodRedLight,
                            onClick = onNavigateToRequests
                        )

                        SimpleMetricBox(
                            modifier = Modifier.weight(1f),
                            title = "Blood Reserve",
                            count = totalStockUnits,
                            subtitle = "Units ready",
                            icon = Icons.Default.Favorite,
                            accentColor = StatusEligibleGreen,
                            bgColor = StatusEligibleGreenLight,
                            onClick = onNavigateToInventory
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SimpleMetricBox(
                            modifier = Modifier.weight(1f),
                            title = "Appointments",
                            count = appointments.size,
                            subtitle = "Scheduled donors",
                            icon = Icons.Default.DateRange,
                            accentColor = StatusWarningAmber,
                            bgColor = StatusWarningAmberLight,
                            onClick = onNavigateToAppointments
                        )

                        SimpleMetricBox(
                            modifier = Modifier.weight(1f),
                            title = "Donor Registry",
                            count = donors.size,
                            subtitle = "Verified pool",
                            icon = Icons.Default.Person,
                            accentColor = BloodRedPrimary,
                            bgColor = BloodRedLight,
                            onClick = null
                        )
                    }
                }
            }

            // ========================================================
            // 4. BOX: BLOOD GROUP STOCK SNAPSHOT (8 Groups Grid)
            // ========================================================
            item {
                BloodSyncCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = appColors.border
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Blood Stock Snapshot",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = "Current units across 8 blood groups",
                                    fontSize = 11.5.sp,
                                    color = appColors.textSecondary
                                )
                            }
                            Text(
                                text = "Manage →",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary,
                                modifier = Modifier
                                    .clickable { onNavigateToInventory() }
                                    .padding(4.dp)
                            )
                        }

                        // 4x2 Grid of neat Blood Group Stock Boxes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (grp in allGroups.take(4)) {
                                    val units = bloodStock[grp] ?: 0
                                    val cap = repository.bloodStockCapacities[grp] ?: 100
                                    val isLow = (units.toFloat() / cap.toFloat()) <= 0.35f
                                    BloodGroupMiniBox(
                                        modifier = Modifier.weight(1f),
                                        group = grp,
                                        units = units,
                                        isCritical = isLow,
                                        onClick = onNavigateToInventory
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (grp in allGroups.drop(4)) {
                                    val units = bloodStock[grp] ?: 0
                                    val cap = repository.bloodStockCapacities[grp] ?: 100
                                    val isLow = (units.toFloat() / cap.toFloat()) <= 0.35f
                                    BloodGroupMiniBox(
                                        modifier = Modifier.weight(1f),
                                        group = grp,
                                        units = units,
                                        isCritical = isLow,
                                        onClick = onNavigateToInventory
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 5. BOX: QUICK COMMAND ACTIONS (2x2 Box Tiles)
            // ========================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "QUICK COMMAND ACTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = appColors.textMuted,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionTileBox(
                            modifier = Modifier.weight(1f),
                            title = "Emergency SOS",
                            subtitle = "${activeRequests.size} Active Hospital Calls",
                            icon = Icons.Default.Notifications,
                            accentColor = BloodRedPrimary,
                            onClick = onNavigateToRequests
                        )

                        ActionTileBox(
                            modifier = Modifier.weight(1f),
                            title = "Inventory Units",
                            subtitle = "Update Blood Stocks",
                            icon = Icons.AutoMirrored.Filled.List,
                            accentColor = StatusEligibleGreen,
                            onClick = onNavigateToInventory
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionTileBox(
                            modifier = Modifier.weight(1f),
                            title = "Donor Schedule",
                            subtitle = "${appointments.size} Appointments",
                            icon = Icons.Default.DateRange,
                            accentColor = StatusWarningAmber,
                            onClick = onNavigateToAppointments
                        )

                        ActionTileBox(
                            modifier = Modifier.weight(1f),
                            title = "Clinical License",
                            subtitle = if (effectiveLicense.isNotBlank()) "License: $effectiveLicense" else "Tap to Add License",
                            icon = Icons.Default.Badge,
                            accentColor = BloodRedPrimary,
                            onClick = {
                                editLicenseInput = effectiveLicense
                                showEditLicenseDialog = true
                            }
                        )
                    }
                }
            }

            // ========================================================
            // 6. BOX: RECENT EMERGENCY REQUESTS
            // ========================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT EMERGENCY SOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = appColors.textMuted,
                        letterSpacing = 0.5.sp
                    )
                    if (activeRequests.isNotEmpty()) {
                        Text(
                            text = "View All (${activeRequests.size}) →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BloodRedPrimary,
                            modifier = Modifier
                                .clickable { onNavigateToRequests() }
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (activeRequests.isEmpty()) {
                item {
                    BloodSyncCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = appColors.border
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusEligibleGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusEligibleGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "All Clear - Zero Active Emergencies",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = "All inbound emergency requests have been dispatched or resolved.",
                                    fontSize = 11.5.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                items(activeRequests.take(3), key = { it.id }) { req ->
                    BloodSyncCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToRequests() },
                        borderColor = appColors.border
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BloodRedPrimary
                                ) {
                                    Text(
                                        text = req.bloodGroupNeeded,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        fontSize = 14.sp,
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
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${req.unitsRequired} Units • ${req.hospitalName}",
                                        fontSize = 11.5.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }

                            StatusPillBadge(
                                text = req.urgencyLevel.name,
                                backgroundColor = if (req.urgencyLevel == UrgencyLevel.IMMEDIATE) BloodRedLight else StatusWarningAmberLight,
                                contentColor = if (req.urgencyLevel == UrgencyLevel.IMMEDIATE) BloodRedPrimary else StatusWarningAmber,
                                showBeacon = req.urgencyLevel == UrgencyLevel.IMMEDIATE
                            )
                        }
                    }
                }
            }
        }
    }

    // ========================================================
    // EDIT CLINICAL LICENSE DIALOG
    // ========================================================
    if (showEditLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showEditLicenseDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = BloodRedPrimary
                    )
                    Text(
                        text = "Government License",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = appColors.textPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter or update your official clinical establishment registration / drug control license number (e.g. DL-7829-BB or MH-BB-9021).",
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = editLicenseInput,
                        onValueChange = { editLicenseInput = it },
                        label = { Text("Clinical License Number") },
                        placeholder = { Text("e.g., DL-7829-BB") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editLicenseInput.trim()
                        if (trimmed.isNotBlank()) {
                            repository.updateBloodBankLicense(trimmed)
                        }
                        showEditLicenseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save & Verify", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditLicenseDialog = false }
                ) {
                    Text("Cancel", color = appColors.textMuted)
                }
            },
            containerColor = appColors.surface,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

// ========================================================
// REUSABLE BOX COMPONENTS
// ========================================================

/**
 * Clean Mini-Info Box (for City, Phone, Operating Hours)
 */
@Composable
private fun FacilityInfoMiniBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String
) {
    val appColors = BloodSyncTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = WarmLightSurface,
        border = BorderStroke(1.dp, SoftBorderPink)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BloodRedPrimary,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = title,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textMuted
                )
            }
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                maxLines = 1
            )
        }
    }
}

/**
 * Simple 2x2 Metric KPI Box Tile
 */
@Composable
private fun SimpleMetricBox(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    onClick: (() -> Unit)? = null
) {
    val appColors = BloodSyncTheme.colors
    BloodSyncCard(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        borderColor = appColors.border
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
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

            AnimatedCounter(
                targetValue = count,
                fontSize = 24.sp,
                color = appColors.textPrimary,
                fontWeight = FontWeight.ExtraBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = appColors.textMuted
                )
            }
        }
    }
}

/**
 * Blood Group Mini Box (4x2 Grid)
 */
@Composable
private fun BloodGroupMiniBox(
    modifier: Modifier = Modifier,
    group: String,
    units: Int,
    isCritical: Boolean,
    onClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isCritical) BloodRedLight else WarmLightSurface,
        border = BorderStroke(1.dp, if (isCritical) BloodRedPrimary.copy(alpha = 0.4f) else SoftBorderPink)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = group,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCritical) BloodRedPrimary else appColors.textPrimary
            )
            Text(
                text = "$units U",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCritical) BloodRedDark else appColors.textSecondary
            )
        }
    }
}

/**
 * 2x2 Quick Action Command Box Tile
 */
@Composable
private fun ActionTileBox(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    BloodSyncCard(
        modifier = modifier.clickable { onClick() },
        borderColor = appColors.border
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = appColors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = appColors.textMuted,
                    maxLines = 1
                )
            }
        }
    }
}
