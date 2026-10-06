package com.bloodsync.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.EmergencyRequest
import com.bloodsync.data.model.EmergencyStatus
import com.bloodsync.data.model.UrgencyLevel
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.BloodSyncTopBar
import com.bloodsync.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class RequestFilter(val label: String) {
    ALL("All SOS"),
    ACTIVE("Active / Broadcasting"),
    FULFILLED("Fulfilled")
}

@Composable
fun BankRequestsScreen(
    repository: BloodSyncRepository,
    onNotificationClick: () -> Unit = {}
) {
    val requests = repository.emergencyRequests
    val unreadNotifs by repository.unreadNotificationCount
    val appColors = BloodSyncTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFilter by remember { mutableStateOf(RequestFilter.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastRefreshedTime by remember { mutableStateOf("Just now") }

    // Infinite rotation animation when refreshing
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshRotation"
    )

    fun triggerRefresh() {
        if (isRefreshing) return
        isRefreshing = true
        repository.refreshEmergencyRequests { count ->
            isRefreshing = false
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            lastRefreshedTime = timeFormat.format(Date())
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Synced with Firestore: $count requests active")
            }
        }
    }

    // Filter calculations
    val activeCount = requests.count { it.status != EmergencyStatus.FULFILLED && it.status != EmergencyStatus.CANCELLED }
    val fulfilledCount = requests.count { it.status == EmergencyStatus.FULFILLED }

    val filteredList = remember(requests, selectedFilter) {
        when (selectedFilter) {
            RequestFilter.ALL -> requests
            RequestFilter.ACTIVE -> requests.filter { it.status != EmergencyStatus.FULFILLED && it.status != EmergencyStatus.CANCELLED }
            RequestFilter.FULFILLED -> requests.filter { it.status == EmergencyStatus.FULFILLED }
        }
    }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Emergency Requests",
                subtitle = "Inbound Hospital & Patient SOS",
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick,
                actions = {
                    // Header Refresh Action Button
                    IconButton(
                        onClick = { triggerRefresh() },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh emergency requests",
                            tint = if (isRefreshing) BloodRedPrimary else appColors.textPrimary,
                            modifier = Modifier
                                .size(22.dp)
                                .then(if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // 1. Prominent Refresh Header Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { triggerRefresh() },
                    shape = RoundedCornerShape(50.dp),
                    color = if (isRefreshing) BloodRedLight else appColors.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRefreshing) BloodRedPrimary.copy(alpha = 0.4f) else appColors.border
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isRefreshing) BloodRedPrimary else StatusEligibleGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (isRefreshing) MedicalWhite else StatusEligibleGreen,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .then(if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isRefreshing) "Refreshing Live Inbound Feed..." else "Sync Inbound SOS Feed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isRefreshing) BloodRedPrimary else appColors.textPrimary
                                )
                                Text(
                                    text = "Last checked: $lastRefreshedTime",
                                    fontSize = 11.sp,
                                    color = appColors.textMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isRefreshing) BloodRedPrimary else BloodRedLight
                        ) {
                            Text(
                                text = if (isRefreshing) "Fetching..." else "Refresh Now",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRefreshing) MedicalWhite else BloodRedPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 2. Filter Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RequestFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val badgeText = when (filter) {
                            RequestFilter.ALL -> "${requests.size}"
                            RequestFilter.ACTIVE -> "$activeCount"
                            RequestFilter.FULFILLED -> "$fulfilledCount"
                        }
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
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MedicalWhite else appColors.textSecondary
                                )
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = if (isSelected) MedicalWhite.copy(alpha = 0.25f) else appColors.border
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) MedicalWhite else appColors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Request Items or Empty State
            if (filteredList.isEmpty()) {
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(StatusEligibleGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusEligibleGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No Requests Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = if (selectedFilter == RequestFilter.ALL)
                                    "There are currently zero active emergency requests broadcast in the network."
                                else
                                    "No requests found under the '${selectedFilter.label}' filter tab.",
                                fontSize = 13.sp,
                                color = appColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { triggerRefresh() },
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MedicalWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Re-check Firestore",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalWhite
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { request ->
                    val isFulfilled = request.status == EmergencyStatus.FULFILLED
                    val isImmediate = request.urgencyLevel == UrgencyLevel.IMMEDIATE

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isImmediate && !isFulfilled) BloodRedPrimary.copy(alpha = 0.35f) else appColors.border
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top Row: Blood Group + Urgency + Status
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
                                            text = request.bloodGroupNeeded,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MedicalWhite
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = request.patientName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appColors.textPrimary
                                        )
                                        Text(
                                            text = "${request.unitsRequired} Units Required",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BloodRedPrimary
                                        )
                                    }
                                }

                                // Urgency Badge
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = if (isFulfilled) StatusEligibleGreenLight else if (isImmediate) BloodRedLight else StatusWarningAmberLight,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isFulfilled) StatusEligibleGreen.copy(alpha = 0.3f) else if (isImmediate) BloodRedPrimary.copy(alpha = 0.3f) else StatusWarningAmber.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = if (isFulfilled) "FULFILLED" else request.urgencyLevel.name,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isFulfilled) StatusEligibleGreen else if (isImmediate) BloodRedPrimary else StatusWarningAmber
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = appColors.border,
                                thickness = 1.dp
                            )

                            // Hospital & Location Info
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(appColors.border.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = BloodRedPrimary
                                    )
                                }
                                Column {
                                    Text(
                                        text = request.hospitalName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textPrimary
                                    )
                                    if (request.hospitalAddress.isNotBlank()) {
                                        Text(
                                            text = request.hospitalAddress,
                                            fontSize = 11.sp,
                                            color = appColors.textSecondary
                                        )
                                    }
                                }
                            }

                            // Contact Phone Row with Direct Call Action
                            if (request.contactPhone.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(StatusEligibleGreenLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                                tint = StatusEligibleGreen
                                            )
                                        }
                                        Text(
                                            text = request.contactPhone,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = appColors.textPrimary
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .clickable {
                                                try {
                                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                                        data = Uri.parse("tel:${request.contactPhone}")
                                                    }
                                                    context.startActivity(callIntent)
                                                } catch (_: Exception) {}
                                            },
                                        shape = RoundedCornerShape(50.dp),
                                        color = StatusEligibleGreenLight,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = null,
                                                tint = StatusEligibleGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Call Contact",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusEligibleGreen
                                            )
                                        }
                                    }
                                }
                            }

                            // Additional Notes (if present)
                            if (request.additionalNotes.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = appColors.background,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                                ) {
                                    Text(
                                        text = "Note: ${request.additionalNotes}",
                                        fontSize = 11.sp,
                                        color = appColors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Footer: Time & Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Requested: ${request.requestedAt}",
                                    fontSize = 11.sp,
                                    color = appColors.textMuted
                                )

                                if (isFulfilled) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = StatusEligibleGreenLight
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = StatusEligibleGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Fulfilled by Bank",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusEligibleGreen
                                            )
                                        }
                                    }
                                }
                            }

                            // Mark as Fulfilled Action Button (Pill-shaped, Medical Green)
                            if (!isFulfilled && request.status != EmergencyStatus.CANCELLED) {
                                Button(
                                    onClick = {
                                        repository.fulfillEmergencyRequest(request.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Marked request for ${request.patientName} as Fulfilled!")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusEligibleGreen,
                                        contentColor = MedicalWhite
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Fulfill & Dispatch Units",
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
}
