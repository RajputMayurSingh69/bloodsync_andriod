package com.bloodsync.ui.screens.health

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
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

    // Fixed order of all 8 blood groups
    val allGroups = remember { listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-") }

    // Summary calculations
    val totalUnits = remember(bloodStock) { bloodStock.values.sum() }
    val criticalGroups = remember(bloodStock, capacities) {
        allGroups.filter { grp ->
            val units = bloodStock[grp] ?: 0
            val maxCap = capacities[grp] ?: 100
            val pct = (units.toFloat() / maxCap.toFloat()) * 100f
            pct <= 35f
        }
    }
    val optimalGroupsCount = remember(bloodStock, capacities) {
        allGroups.count { grp ->
            val units = bloodStock[grp] ?: 0
            val maxCap = capacities[grp] ?: 100
            val pct = (units.toFloat() / maxCap.toFloat()) * 100f
            pct > 65f
        }
    }

    // Filtered groups
    val filteredGroups = remember(bloodStock, capacities, selectedFilter, searchQuery) {
        allGroups.filter { grp ->
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
            // 1. KPI Metrics Header with AnimatedCounters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Stock
                    BloodSyncCard(modifier = Modifier.weight(1f)) {
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
                                        .clip(RoundedCornerShape(8.dp))
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
                            AnimatedCounter(
                                targetValue = totalUnits,
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
                    BloodSyncCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = if (criticalGroups.isNotEmpty()) appColors.primaryLight else appColors.cardBackground,
                        borderColor = if (criticalGroups.isNotEmpty()) appColors.primary.copy(alpha = 0.4f) else appColors.border
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (criticalGroups.isNotEmpty()) BloodRedPrimary else StatusWarningAmberLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (criticalGroups.isNotEmpty()) Color.White else appColors.warningAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            AnimatedCounter(
                                targetValue = criticalGroups.size,
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
                    BloodSyncCard(modifier = Modifier.weight(1f)) {
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
                                        .clip(RoundedCornerShape(8.dp))
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
                            AnimatedCounter(
                                targetValue = optimalGroupsCount,
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
                    BloodSyncCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BloodRedLight,
                        borderColor = BloodRedPrimary.copy(alpha = 0.35f)
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BloodRedPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MedicalWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Critical Stock Shortage",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = BloodRedPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Reserves under 35% for ${criticalGroups.joinToString(", ")}. Restock immediately to maintain emergency readiness.",
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Search Bar
            item {
                BloodSyncTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search Blood Group",
                    placeholder = "Filter blood groups (e.g. O-, A+, B)...",
                    leadingIcon = Icons.Default.Search,
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MedicalTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Filter Chips (Pill-shaped)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InventoryFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .clickable { selectedFilter = filter },
                            shape = RoundedCornerShape(50.dp),
                            color = if (isSelected) appColors.primary else appColors.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) appColors.primary else appColors.border
                            )
                        ) {
                            Text(
                                text = filter.label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else appColors.textSecondary
                            )
                        }
                    }
                }
            }

            // 5. Blood Group Inventory Cards or Empty Search State
            if (filteredGroups.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Blood Groups Found",
                        message = "No blood groups found matching '$searchQuery' under '${selectedFilter.label}'.",
                        icon = Icons.Default.Info,
                        actionText = "Reset Filters",
                        onActionClick = {
                            searchQuery = ""
                            selectedFilter = InventoryFilter.ALL
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
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

                    BloodSyncCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (isLow) appColors.primaryLight.copy(alpha = 0.4f) else appColors.cardBackground,
                        borderColor = if (isLow) appColors.primary.copy(alpha = 0.35f) else appColors.border
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top Row: Group Badge + Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = BloodRedPrimary
                                    ) {
                                        Text(
                                            text = group,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MedicalWhite
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Blood Group $group",
                                            fontSize = 15.sp,
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

                                StatusPillBadge(
                                    text = statusLabel,
                                    backgroundColor = statusBg,
                                    contentColor = statusColor,
                                    showBeacon = isLow
                                )
                            }

                            // Middle: Units in Reserve + Percentage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$units",
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = "Units Available",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = appColors.textSecondary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                Text(
                                    text = "${pct.toInt()}% full",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            // Capacity Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(WarmLightSurface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (pct / 100f).coerceIn(0.02f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(statusColor)
                                )
                            }

                            HorizontalDivider(
                                color = SoftBorderPink,
                                thickness = 0.5.dp
                            )

                            // Bottom Actions: Quick Adjustments & Exact Edit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                    border = BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Issue unit",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "−1 Issue",
                                        fontSize = 12.sp,
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
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Restock unit",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+1 Receive",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Custom Edit / Audit Button
                                Surface(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .clickable { editTargetGroup = group },
                                    shape = RoundedCornerShape(50.dp),
                                    color = WarmLightSurface,
                                    border = BorderStroke(1.dp, SoftBorderPink)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit units",
                                            tint = TextSecondaryGrey,
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
    }

    // Direct Quantity Update Dialog
    editTargetGroup?.let { group ->
        val currentUnits = bloodStock[group] ?: 0
        val maxCap = capacities[group] ?: 100
        var inputUnitsText by remember { mutableStateOf("$currentUnits") }

        Dialog(onDismissRequest = { editTargetGroup = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MedicalWhite,
                border = BorderStroke(1.dp, SoftBorderPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BloodRedPrimary
                            ) {
                                Text(
                                    text = group,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MedicalWhite,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Text(
                                text = "Adjust Reserve",
                                fontSize = 17.sp,
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
                        text = "Specify exact total units on hand for blood group $group. Storage capacity limit is $maxCap units.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        lineHeight = 18.sp
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
                            unfocusedBorderColor = SoftBorderPink,
                            cursorColor = BloodRedPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick increment pills
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
                                border = BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = deltaStr,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
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
                            border = BorderStroke(1.dp, SoftBorderPink),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = appColors.textSecondary, fontWeight = FontWeight.SemiBold)
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
