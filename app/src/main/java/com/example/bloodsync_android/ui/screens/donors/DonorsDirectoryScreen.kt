package com.example.bloodsync_android.ui.screens.donors

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.bloodsync_android.data.model.UserProfile
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodDropIcon
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*
import com.example.bloodsync_android.util.ShareHelper

/**
 * Dedicated Available Donors Directory Screen.
 * Moved out of the homepage to keep the home UI clean, simple, and uncluttered.
 * Pill-shaped design language, strict RED/GREEN/WHITE/YELLOW palette.
 */
@Composable
fun DonorsDirectoryScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val context = LocalContext.current
    val donors = repository.donors
    val unreadNotifs by repository.unreadNotificationCount

    var selectedGroup by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredDonors = remember(donors, selectedGroup, searchQuery) {
        donors.filter { donor ->
            val matchesGroup = if (selectedGroup == "All") true else donor.bloodGroup.equals(selectedGroup, ignoreCase = true)
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
                title = "Available Donors",
                subtitle = "${donors.size} registered lifesavers online",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = BloodSyncTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Pill Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(50.dp),
                color = MedicalWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MedicalTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by city or donor name...", fontSize = 13.sp, color = MedicalTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = BloodRedPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MedicalTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Blood Group Filter Carousel (Pill Shape)
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
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) BloodRedPrimary else MedicalBorder,
                                shape = RoundedCornerShape(50.dp)
                            ),
                        color = if (isSelected) BloodRedPrimary else MedicalWhite,
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = group,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) MedicalWhite else MedicalTextPrimary
                            )
                        }
                    }
                }
            }

            // Results count badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredDonors.size} Donors Found",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalTextSecondary
                )
                StatusBadge(
                    text = if (selectedGroup == "All") "All Groups" else "$selectedGroup Group",
                    textColor = BloodRedPrimary,
                    backgroundColor = BloodRedLight
                )
            }

            // Donors Feed List
            if (filteredDonors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MedicalWhite,
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(BloodRedLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                BloodDropIcon(size = 32.dp, tint = BloodRedPrimary)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No active donors found for $selectedGroup",
                                fontWeight = FontWeight.Bold,
                                color = MedicalTextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Need blood urgently? Broadcast an emergency SOS to notify matching volunteers nearby immediately.",
                                fontSize = 12.sp,
                                color = MedicalTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onNavigateToEmergency,
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Broadcast Emergency SOS", color = MedicalWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDonors, key = { it.id }) { donor ->
                        DonorPillCard(
                            donor = donor,
                            onWhatsAppClick = {
                                val msg = "Hello ${donor.name}, I am reaching out from BloodSync App regarding voluntary blood donation (${donor.bloodGroup}). Are you available?"
                                ShareHelper.openWhatsApp(context, donor.phone, msg)
                            },
                            onRequestClick = onNavigateToEmergency
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Pill-Shaped Donor Card.
 */
@Composable
private fun DonorPillCard(
    donor: UserProfile,
    onWhatsAppClick: () -> Unit,
    onRequestClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MedicalWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
        shadowElevation = 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Round Pill Blood Group Avatar (Red)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(BloodRedPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = donor.bloodGroup,
                        color = MedicalWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (donor.name.isNotBlank()) donor.name else "Volunteer Donor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MedicalTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        // Pulsing Green Active Beacon
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(StatusEligibleGreen.copy(alpha = beaconAlpha), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Now • ${if (donor.city.isNotBlank()) donor.city else "Nearby"}",
                            fontSize = 12.sp,
                            color = StatusEligibleGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (donor.totalDonations > 0) {
                        Text(
                            text = "${donor.totalDonations} Previous Donations",
                            fontSize = 11.sp,
                            color = MedicalTextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Green Pill Contact Button (WhatsApp / Call)
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(StatusEligibleGreen, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Contact Donor",
                        tint = MedicalWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Red Pill SOS Request Action Button
                Button(
                    onClick = onRequestClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text(
                        text = "Request",
                        fontSize = 12.sp,
                        color = MedicalWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
