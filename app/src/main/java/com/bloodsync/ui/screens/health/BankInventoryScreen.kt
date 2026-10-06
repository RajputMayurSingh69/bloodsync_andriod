package com.bloodsync.ui.screens.health

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.BloodSyncTopBar
import com.bloodsync.ui.theme.*

private enum class InventoryFilter(val label: String) {
    ALL("All (8)"),
    CRITICAL("Critical (< 35%)"),
    ADEQUATE("Adequate (35-65%)"),
    OPTIMAL("Optimal (> 65%)")
}

@Composable
fun BankInventoryScreen(
    repository: BloodSyncRepository,
    onNotificationClick: () -> Unit = {}
) {
    val bloodStock = repository.bloodStock
    val capacities = repository.bloodStockCapacities
    val unreadNotifs by repository.unreadNotificationCount
    val appColors = BloodSyncTheme.colors

    var selectedFilter by remember { mutableStateOf(InventoryFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var editTargetGroup by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    // Fixed order of all 8 blood groups
    val allGroups = remember { listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-") }

    // Summary calculations
    val totalUnits = bloodStock.values.sum()
    val criticalGroups = allGroups.filter { grp ->
        val units = bloodStock[grp] ?: 0
        val maxCap = capacities[grp] ?: 100
        val pct = (units.toFloat() / maxCap.toFloat()) * 100f
        pct <= 35f
    }
    val optimalGroupsCount = allGroups.count { grp ->
        val units = bloodStock[grp] ?: 0
        val maxCap = capacities[grp] ?: 100
        val pct = (units.toFloat() / maxCap.toFloat()) * 100f
        pct > 65f
    }

    // Filtered groups
    val filteredGroups = allGroups.filter { grp ->
        val units = bloodStock[grp] ?: 0
        val maxCap = capacities[grp] ?: 100
        val pct = (units.toFloat() / maxCap.toFloat()) * 100f

        val matchesFilter = when (selectedFilter) {
            InventoryFilter.ALL -> true
            InventoryFilter.CRITICAL -> pct <= 35f
            InventoryFilter.ADEQUATE -> pct in 35.001f..65f
            InventoryFilter.OPTIMAL -> pct > 65f
        }

        val cleanQuery = searchQuery.trim().uppercase()
        val matchesSearch = cleanQuery.isEmpty() || grp.uppercase().contains(cleanQuery)

        matchesFilter && matchesSearch
    }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Blood Inventory",
                subtitle = "Live Reserve Matrix & Allocation",
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = appColors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. KPI Metrics Header (Unified 4-color palette & pill styling)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Stock
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Total Units",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appColors.textMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(StatusEligibleGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusEligibleGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$totalUnits",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Across 8 Groups",
                                fontSize = 10.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    // Critical Shortages
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = if (criticalGroups.isNotEmpty()) BloodRedLight else appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (criticalGroups.isNotEmpty()) BloodRedPrimary.copy(alpha = 0.3f) else appColors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Critical",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (criticalGroups.isNotEmpty()) BloodRedPrimary else appColors.textMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (criticalGroups.isNotEmpty()) BloodRedPrimary else StatusWarningAmberLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (criticalGroups.isNotEmpty()) MedicalWhite else StatusWarningAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${criticalGroups.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (criticalGroups.isNotEmpty()) BloodRedPrimary else appColors.textPrimary
                            )
                            Text(
                                text = if (criticalGroups.isNotEmpty()) "Shortages Detected" else "Zero Shortages",
                                fontSize = 10.sp,
                                color = if (criticalGroups.isNotEmpty()) BloodRedPrimary else appColors.textSecondary
                            )
                        }
                    }

                    // Optimal Groups
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Optimal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appColors.textMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(StatusEligibleGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ThumbUp,
                                        contentDescription = null,
                                        tint = StatusEligibleGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$optimalGroupsCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusEligibleGreen
                            )
                            Text(
                                text = "> 65% Capacity",
                                fontSize = 10.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                }
            }

            // 2. Urgent Supply Warning Banner (if shortages exist)
            if (criticalGroups.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = BloodRedLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.25f))
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
                                    text = "Critical Stock Shortage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BloodRedPrimary
                                )
                                Text(
                                    text = "Reserves under 35% for ${criticalGroups.joinToString(", ")}. Restock immediately to maintain emergency readiness.",
                                    fontSize = 12.sp,
                                    color = MedicalTextPrimary.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Filter blood groups (e.g., O-, A+, B)...",
                            fontSize = 13.sp,
                            color = appColors.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = appColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = appColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = appColors.cardBackground,
                        unfocusedContainerColor = appColors.cardBackground,
                        focusedBorderColor = BloodRedPrimary,
                        unfocusedBorderColor = appColors.border,
                        cursorColor = BloodRedPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Filter Chips (Pill-shaped)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InventoryFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .clickable { selectedFilter = filter },
                            shape = RoundedCornerShape(50.dp),
                            color = if (isSelected) BloodRedPrimary else appColors.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BloodRedPrimary else appColors.border
                            )
                        ) {
                            Text(
                                text = filter.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MedicalWhite else appColors.textSecondary
                            )
                        }
                    }
                }
            }

            // 5. Blood Group Inventory Cards or Empty Search State
            if (filteredGroups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(StatusWarningAmberLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = StatusWarningAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "No Blood Groups Match Filter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "No blood groups found matching '$searchQuery' under '${selectedFilter.label}'.",
                                fontSize = 13.sp,
                                color = appColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilter = InventoryFilter.ALL
                                },
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                            ) {
                                Text(
                                    text = "Reset Filters",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalWhite
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredGroups, key = { it }) { group ->
                    val units = bloodStock[group] ?: 0
                    val maxCap = capacities[group] ?: 100
                    val pct = ((units.toFloat() / maxCap.toFloat()) * 100f).coerceIn(0f, 100f)

                    val isLow = pct <= 35f
                    val isOptimal = pct > 65f

                    val statusLabel = when {
                        isLow -> "CRITICAL NEED"
                        isOptimal -> "OPTIMAL RESERVE"
                        else -> "ADEQUATE"
                    }
                    val statusColor = when {
                        isLow -> BloodRedPrimary
                        isOptimal -> StatusEligibleGreen
                        else -> StatusWarningAmber
                    }
                    val statusBg = when {
                        isLow -> BloodRedLight
                        isOptimal -> StatusEligibleGreenLight
                        else -> StatusWarningAmberLight
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLow) BloodRedLight.copy(alpha = 0.35f) else appColors.cardBackground
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isLow) BloodRedPrimary.copy(alpha = 0.25f) else appColors.border
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top Row: Group Badge + Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Blood Group Pill
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = BloodRedPrimary
                                    ) {
                                        Text(
                                            text = group,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MedicalWhite
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Blood Group $group",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appColors.textPrimary
                                        )
                                        Text(
                                            text = "Storage Capacity: $maxCap Units",
                                            fontSize = 11.sp,
                                            color = appColors.textMuted
                                        )
                                    }
                                }

                                // Status Pill
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = statusBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = statusLabel,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor
                                    )
                                }
                            }

                            // Middle: Units in Reserve + Percentage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$units",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = "Units Available",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = appColors.textSecondary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                Text(
                                    text = "${pct.toInt()}% full",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            // Visual Reserve Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(appColors.border)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (pct / 100f).coerceIn(0.02f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(statusColor)
                                )
                            }

                            HorizontalDivider(
                                color = appColors.border,
                                thickness = 1.dp
                            )

                            // Bottom Actions: Quick Adjustments & Exact Edit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // -1 Issue Button
                                OutlinedButton(
                                    onClick = { repository.incrementStock(group, -1) },
                                    enabled = units > 0,
                                    shape = RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = BloodRedPrimary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Issue unit",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "−1 Issue",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // +1 Restock Button
                                Button(
                                    onClick = { repository.incrementStock(group, 1) },
                                    enabled = units < maxCap,
                                    shape = RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusEligibleGreen,
                                        contentColor = MedicalWhite
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Restock unit",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+1 Receive",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Custom Edit / Audit Button
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { editTargetGroup = group },
                                    shape = CircleShape,
                                    color = appColors.cardBackground,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit units",
                                            tint = appColors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct Quantity Update Dialog
    editTargetGroup?.let { group ->
        val currentUnits = bloodStock[group] ?: 0
        val maxCap = capacities[group] ?: 100
        var inputUnitsText by remember { mutableStateOf("$currentUnits") }

        Dialog(onDismissRequest = { editTargetGroup = null }) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = appColors.cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = BloodRedPrimary
                            ) {
                                Text(
                                    text = group,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MedicalWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Adjust Reserve Stock",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                        }

                        IconButton(onClick = { editTargetGroup = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = appColors.textMuted
                            )
                        }
                    }

                    Text(
                        text = "Specify exact total units on hand for blood group $group. Maximum storage capacity is $maxCap units.",
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = inputUnitsText,
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() } && newVal.length <= 4) {
                                inputUnitsText = newVal
                            }
                        },
                        label = { Text("Available Units") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRedPrimary,
                            cursorColor = BloodRedPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick increments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("+5", "+10", "+25").forEach { deltaStr ->
                            val delta = deltaStr.toInt()
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50.dp))
                                    .clickable {
                                        val cur = inputUnitsText.toIntOrNull() ?: 0
                                        inputUnitsText = (cur + delta).coerceAtMost(maxCap).toString()
                                    },
                                shape = RoundedCornerShape(50.dp),
                                color = StatusEligibleGreenLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = deltaStr,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusEligibleGreen
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editTargetGroup = null },
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = appColors.textSecondary)
                        }

                        Button(
                            onClick = {
                                val entered = inputUnitsText.toIntOrNull() ?: 0
                                repository.updateBloodStock(group, entered)
                                editTargetGroup = null
                            },
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Units", color = MedicalWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
