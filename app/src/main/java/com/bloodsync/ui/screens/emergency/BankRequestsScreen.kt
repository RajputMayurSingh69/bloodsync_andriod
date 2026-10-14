package com.bloodsync.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.EmergencyRequest
import com.bloodsync.data.model.EmergencyStatus
import com.bloodsync.data.model.UrgencyLevel
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private enum class RequestFilter(val label: String) {
    TODAY("Today"),
    DAYS_7("7 Days"),
    DAYS_30("30 Days"),
    CUSTOM("Custom")
}

@Composable
fun BankRequestsScreen(
    repository: BloodSyncRepository,
    highlightedRequestId: String? = null,
    onAcknowledgeRequest: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val requests = repository.emergencyRequests
    val unreadNotifs by repository.unreadNotificationCount
    val appColors = BloodSyncTheme.colors
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFilter by remember { mutableStateOf(RequestFilter.TODAY) }
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
        repository.refreshEmergencyRequests { _ ->
            isRefreshing = false
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            lastRefreshedTime = timeFormat.format(Date())
        }
    }

    // Filter calculations
    val now = remember { System.currentTimeMillis() }
    fun getDiff(req: EmergencyRequest): Long {
        return try {
            val reqTime = req.requestedAt.toLong()
            val diff = now - reqTime
            if (diff < 0) 0L else diff
        } catch (_: Exception) {
            if (req.requestedAt == "Just now") 0L else Long.MAX_VALUE
        }
    }

    val activeRequests = remember(requests) {
        requests.filter { it.status == EmergencyStatus.BROADCASTING }
    }
    val todayCount = remember(activeRequests) { activeRequests.count { getDiff(it) <= 86400_000L } }
    val days7Count = remember(activeRequests) { activeRequests.count { getDiff(it) <= 7 * 86400_000L } }
    val days30Count = remember(activeRequests) { activeRequests.count { getDiff(it) <= 30L * 86400_000L } }
    val customCount = remember(activeRequests) { activeRequests.size }

    val filteredList = remember(activeRequests, selectedFilter) {
        activeRequests.filter { req ->
            val diff = getDiff(req)
            when (selectedFilter) {
                RequestFilter.TODAY -> diff <= 86400_000L
                RequestFilter.DAYS_7 -> diff <= 7 * 86400_000L
                RequestFilter.DAYS_30 -> diff <= 30L * 86400_000L
                RequestFilter.CUSTOM -> true
            }
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll into view when an emergency request is highlighted
    LaunchedEffect(highlightedRequestId, filteredList) {
        if (!highlightedRequestId.isNullOrBlank()) {
            val targetIdx = filteredList.indexOfFirst { it.id == highlightedRequestId }
            if (targetIdx >= 0) {
                listState.animateScrollToItem((targetIdx + 2).coerceAtLeast(0))
            } else if (activeRequests.any { it.id == highlightedRequestId }) {
                selectedFilter = RequestFilter.CUSTOM
            }
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
                                .graphicsLayer {
                                    if (isRefreshing) {
                                        rotationZ = rotationAngle
                                    }
                                }
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Prominent Refresh Header Banner
            item {
                BloodSyncCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { triggerRefresh() },
                    backgroundColor = if (isRefreshing) appColors.primaryLight else appColors.cardBackground,
                    borderColor = if (isRefreshing) appColors.primary.copy(alpha = 0.4f) else appColors.border
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isRefreshing) BloodRedPrimary else StatusEligibleGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (isRefreshing) Color.White else appColors.successGreen,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer {
                                            if (isRefreshing) {
                                                rotationZ = rotationAngle
                                            }
                                        }
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
                                    text = "Last synced: $lastRefreshedTime",
                                    fontSize = 11.sp,
                                    color = appColors.textMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isRefreshing) appColors.primary else appColors.primaryLight
                        ) {
                            Text(
                                text = if (isRefreshing) "Syncing..." else "Refresh Now",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRefreshing) Color.White else appColors.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 2. Filter Pills Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RequestFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val badgeCount = when (filter) {
                            RequestFilter.TODAY -> todayCount
                            RequestFilter.DAYS_7 -> days7Count
                            RequestFilter.DAYS_30 -> days30Count
                            RequestFilter.CUSTOM -> customCount
                        }
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
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else appColors.textSecondary
                                )
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else appColors.primaryLight
                                ) {
                                    Text(
                                        text = "$badgeCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else appColors.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                    EmptyState(
                        title = "No Inbound Requests Found",
                        message = if (selectedFilter == RequestFilter.CUSTOM)
                            "There are currently zero active emergency requests broadcast in the network."
                        else
                            "No emergency requests match the '${selectedFilter.label}' filter criteria.",
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                items(filteredList, key = { it.id }) { request ->
                    val isFulfilled = request.status == EmergencyStatus.FULFILLED
                    val isImmediate = request.urgencyLevel == UrgencyLevel.IMMEDIATE
                    val isTargetHighlighted = request.id == highlightedRequestId

                    // Infinite pulsing red glow animation for newly alerted emergency request
                    val infinitePulse = rememberInfiniteTransition(label = "reqPulse")
                    val pulseAlpha by infinitePulse.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    BloodSyncCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isTargetHighlighted) {
                                    onAcknowledgeRequest(request.id)
                                }
                            },
                        borderColor = when {
                            isTargetHighlighted -> BloodRedPrimary.copy(alpha = pulseAlpha)
                            isImmediate && !isFulfilled -> BloodRedPrimary.copy(alpha = 0.5f)
                            else -> SoftBorderPink
                        }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Pulsing Alert Banner for Highlighted Request
                            if (isTargetHighlighted) {
                                Surface(
                                    color = BloodRedPrimary.copy(alpha = 0.95f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAcknowledgeRequest(request.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🚨 NEW INBOUND EMERGENCY • ACTIVE ALERT",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = Color.White,
                                            modifier = Modifier.clickable { onAcknowledgeRequest(request.id) }
                                        ) {
                                            Text(
                                                text = "Acknowledge",
                                                color = BloodRedPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Header Area
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        when {
                                            isTargetHighlighted -> BloodRedLight.copy(alpha = 0.5f)
                                            isImmediate && !isFulfilled -> BloodRedLight.copy(alpha = 0.3f)
                                            else -> WarmLightSurface
                                        }
                                    )
                                    .padding(16.dp)
                            ) {
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
                                            shape = RoundedCornerShape(14.dp),
                                            color = BloodRedPrimary
                                        ) {
                                            Box(
                                                modifier = Modifier.size(54.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = request.bloodGroupNeeded,
                                                    fontSize = 19.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MedicalWhite
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = request.patientName,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = appColors.textPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.WaterDrop,
                                                    contentDescription = null,
                                                    tint = BloodRedPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${request.unitsRequired} Units Required",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BloodRedPrimary
                                                )
                                            }
                                        }
                                    }

                                    StatusPillBadge(
                                        text = if (isFulfilled) "FULFILLED" else request.urgencyLevel.name,
                                        backgroundColor = if (isFulfilled) StatusEligibleGreenLight else if (isImmediate) BloodRedLight else StatusWarningAmberLight,
                                        contentColor = if (isFulfilled) StatusEligibleGreen else if (isImmediate) BloodRedPrimary else StatusWarningAmber,
                                        showBeacon = isImmediate && !isFulfilled
                                    )
                                }
                            }

                            HorizontalDivider(color = SoftBorderPink, thickness = 0.5.dp)

                            // Hospital & Contact Details
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BloodRedLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalHospital,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = BloodRedPrimary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = request.hospitalName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
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

                                if (request.contactPhone.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = WarmLightSurface,
                                        border = BorderStroke(1.dp, SoftBorderPink),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = null,
                                                    tint = StatusEligibleGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = request.contactPhone,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
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
                                                color = StatusEligibleGreen
                                            ) {
                                                Text(
                                                    text = "CALL ATTENDANT",
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MedicalWhite
                                                )
                                            }
                                        }
                                    }
                                }

                                if (request.additionalNotes.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = StatusWarningAmberLight.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, StatusWarningAmber.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = StatusWarningAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = request.additionalNotes,
                                                fontSize = 12.sp,
                                                color = appColors.textPrimary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Footer Area with timestamp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(WarmLightSurface)
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
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
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = appColors.textMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = formatRelativeTime(request.requestedAt),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = appColors.textMuted
                                        )
                                    }

                                    if (isFulfilled) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = StatusEligibleGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Fulfilled",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusEligibleGreen
                                            )
                                        }
                                    }
                                }
                            }

                            // 1-Tap Approve / Allocate & Dispatch Button
                            if (!isFulfilled && request.status != EmergencyStatus.CANCELLED) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    BloodSyncButton(
                                        text = "Fulfill & Dispatch Units",
                                        onClick = {
                                            onAcknowledgeRequest(request.id)
                                            repository.fulfillEmergencyRequest(request.id)
                                        },
                                        leadingIcon = Icons.Default.VolunteerActivism,
                                        gradient = BrushGradients.SuccessPill,
                                        modifier = Modifier.fillMaxWidth()
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

private fun formatRelativeTime(timestampStr: String): String {
    try {
        val timestamp = timestampStr.toLong()
        val diff = System.currentTimeMillis() - timestamp
        if (diff < 0) return "Just now"
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} mins ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            else -> "${diff / 86400_000} days ago"
        }
    } catch (_: Exception) {
        return timestampStr
    }
}
