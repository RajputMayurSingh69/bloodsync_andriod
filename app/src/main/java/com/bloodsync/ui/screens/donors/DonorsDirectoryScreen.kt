package com.bloodsync.ui.screens.donors

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.UserProfile
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*
import com.bloodsync.util.BloodCompatibilityHelper

/**
 * Redesigned Available Donors Directory Screen.
 * Soft Medical Red & White aesthetic, 24dp cards, 50dp pill chips,
 * circular blood avatars with red outline, live available beacons, and smooth animations.
 */
@Composable
fun DonorsDirectoryScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val strings = com.bloodsync.util.LocalAppStrings.current
    val donors = repository.donors
    val unreadNotifs by repository.unreadNotificationCount

    var selectedGroup by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var matchCompatibleOnly by remember { mutableStateOf(true) }
    val appColors = BloodSyncTheme.colors

    val filteredDonors = remember(donors, selectedGroup, searchQuery, matchCompatibleOnly) {
        donors.filter { donor ->
            val matchesGroup = when {
                selectedGroup == "All" -> true
                matchCompatibleOnly -> BloodCompatibilityHelper.isDonorCompatible(donor.bloodGroup, selectedGroup)
                else -> donor.bloodGroup.trim().equals(selectedGroup.trim(), ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                donor.name.contains(searchQuery, ignoreCase = true) ||
                donor.city.contains(searchQuery, ignoreCase = true) ||
                donor.bloodGroup.contains(searchQuery, ignoreCase = true)
            }
            matchesGroup && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = strings.donorsDirectoryTitle,
                subtitle = "${donors.size} ${strings.availableNow}",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = appColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Search Bar (16dp rounded pill with soft pink border) ──────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = appColors.cardBackground,
                border = BorderStroke(1.dp, appColors.border),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(appColors.redLight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = BloodRedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                strings.searchDonorsPlaceholder,
                                fontSize = 14.sp,
                                color = appColors.textMuted
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = BloodRedPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = appColors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Horizontal Blood Group Filter Pills (50dp rounded) ───────────
            val bloodGroups = listOf("All", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bloodGroups) { group ->
                    val isSelected = selectedGroup == group
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "pillScale"
                    )

                    Surface(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { selectedGroup = group }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BloodRedPrimary else appColors.border,
                                shape = RoundedCornerShape(50.dp)
                            ),
                        color = if (isSelected) Color.Transparent else appColors.cardBackground,
                        shadowElevation = if (isSelected) 3.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            Brush.horizontalGradient(
                                                listOf(BloodRedGradientStart, BloodRedGradientEnd)
                                            )
                                        )
                                    } else Modifier
                                )
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = group,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else appColors.textPrimary
                            )
                        }
                    }
                }
            }

            // ── Smart Compatibility Toggle Banner ────────────────────────────
            if (selectedGroup != "All") {
                val compatibleList = BloodCompatibilityHelper.getCompatibleDonorGroups(selectedGroup).joinToString(", ")
                BloodSyncCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = if (matchCompatibleOnly) appColors.redLight else appColors.cardBackground,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (matchCompatibleOnly) BloodRedPrimary.copy(alpha = 0.35f) else appColors.border
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { matchCompatibleOnly = !matchCompatibleOnly }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (matchCompatibleOnly) BloodRedPrimary else appColors.border,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (matchCompatibleOnly) Icons.Rounded.Check else Icons.Rounded.FilterList,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (matchCompatibleOnly) "Smart Compatibility Active" else "Exact Matches Only",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (matchCompatibleOnly) BloodRedPrimary else appColors.textPrimary
                                )
                                Text(
                                    text = if (matchCompatibleOnly) "Can accept from: $compatibleList" else "Showing only $selectedGroup donors",
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                        Surface(
                            color = if (matchCompatibleOnly) BloodRedPrimary.copy(alpha = 0.15f) else appColors.surfaceVariant,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = if (matchCompatibleOnly) "Smart" else "Exact",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Results Count Row ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${filteredDonors.size} Donors Available",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(StatusEligibleGreen, CircleShape)
                    )
                }

                StatusPillBadge(
                    text = when {
                        selectedGroup == "All" -> "All Groups"
                        matchCompatibleOnly -> "Compatible: $selectedGroup"
                        else -> "Exact: $selectedGroup"
                    },
                    textColor = BloodRedPrimary,
                    backgroundColor = appColors.redLight
                )
            }

            // ── Donors Feed / Empty State ─────────────────────────────────────
            if (filteredDonors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BloodSyncCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .background(appColors.redLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                BloodDropIcon(size = 34.dp, tint = BloodRedPrimary)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No active donors for $selectedGroup",
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Need blood urgently? Broadcast an emergency SOS to reach registered donors nearby.",
                                fontSize = 13.sp,
                                color = appColors.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            BloodSyncButton(
                                text = "Broadcast Emergency SOS",
                                onClick = onNavigateToEmergency,
                                leadingIcon = Icons.Rounded.Warning,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDonors, key = { it.id }) { donor ->
                        ModernDonorCard(
                            donor = donor,
                            onRequestClick = onNavigateToEmergency
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Clean Donor Card with 24dp radius, red circular avatar outline,
 * live green available beacon and rounded "Request SOS" pill button.
 */
@Composable
private fun ModernDonorCard(
    donor: UserProfile,
    onRequestClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors

    BloodSyncCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular Blood Group Avatar with Red Outline & Soft Glow
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(appColors.redLight, CircleShape)
                        .border(1.5.dp, BloodRedPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = donor.bloodGroup,
                        color = BloodRedPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = (-0.3).sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (donor.name.isNotBlank()) donor.name else "Volunteer Donor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = appColors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified",
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Green Available Beacon Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(StatusEligibleGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Available Now • ${if (donor.city.isNotBlank()) donor.city else "Nearby"}",
                            fontSize = 12.sp,
                            color = StatusEligibleGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (donor.totalDonations > 0) {
                        Text(
                            text = "${donor.totalDonations} Donations Cleared",
                            fontSize = 11.sp,
                            color = appColors.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Rounded Red "Request SOS" Pill Button
            Surface(
                color = BloodRedPrimary,
                shape = RoundedCornerShape(50.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .clickable(onClick = onRequestClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Emergency,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SOS",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
