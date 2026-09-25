package com.example.bloodsync_android.ui.screens.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.model.AppNotification
import com.example.bloodsync_android.data.model.NotificationType
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.EmptyStateView
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*

@Composable
fun NotificationCenterScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onNavigateToTarget: (targetScreen: String, targetId: String?) -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val notifications = repository.notifications
    val unreadCount by repository.unreadNotificationCount

    var selectedFilter by remember { mutableStateOf<NotificationType?>(null) }
    var showPermissionBanner by remember { mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showPermissionBanner = false
        if (isGranted) {
            repository.postNotification(
                title = "🔔 Push Notifications Enabled",
                message = "You will now receive urgent emergency blood requests and appointment reminders.",
                type = NotificationType.SYSTEM
            )
        }
    }

    val filteredList = remember(notifications, selectedFilter) {
        if (selectedFilter == null) notifications
        else notifications.filter { it.type == selectedFilter }
    }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Notifications",
                subtitle = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { repository.markAllNotificationsAsRead() }) {
                            Text(
                                text = "Mark all read",
                                color = BloodRedPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
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
            // Notification Permission Banner for Android 13+
            AnimatedVisibility(visible = showPermissionBanner) {
                Surface(
                    color = appColors.redLight,
                    modifier = Modifier.fillMaxWidth()
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
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enable Real-time Push Alerts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BloodRedPrimary
                                )
                                Text(
                                    text = "Get alerted when emergency blood is needed nearby",
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showPermissionBanner = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Allow", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Category Filter Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${notifications.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BloodRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(NotificationType.entries.filter { it != NotificationType.SYSTEM }) { type ->
                    val count = notifications.count { it.type == type }
                    FilterChip(
                        selected = selectedFilter == type,
                        onClick = { selectedFilter = type },
                        label = {
                            Text(
                                when (type) {
                                    NotificationType.EMERGENCY -> "Emergency ($count)"
                                    NotificationType.APPOINTMENT -> "Appointments ($count)"
                                    NotificationType.ELIGIBILITY -> "Eligibility ($count)"
                                    NotificationType.CERTIFICATE -> "Certificates ($count)"
                                    else -> "System"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BloodRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Quick trigger button to simulate realistic push alerts (Emergency / Appointment / Certificate)
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                color = appColors.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Simulate Real-Time Notification:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = appColors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = {
                                repository.postNotification(
                                    title = "🚨 URGENT: O- Blood Needed (Children's Ward)",
                                    message = "St. Jude Hospital has 2 patients in urgent need. You match this request!",
                                    type = NotificationType.EMERGENCY,
                                    targetScreen = "emergency"
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text("Emergency", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = {
                                repository.postNotification(
                                    title = "🎖️ New Certificate Available",
                                    message = "Your certificate for verified donation has been generated!",
                                    type = NotificationType.CERTIFICATE,
                                    targetScreen = "certificate"
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text("Certificate", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                EmptyStateView(
                    title = "No Notifications",
                    message = "You have no notifications in this category at the moment.",
                    icon = Icons.Default.NotificationsNone
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { notif ->
                        NotificationItemCard(
                            notification = notif,
                            onItemClick = {
                                repository.markNotificationAsRead(notif.id)
                                if (!notif.targetScreen.isNullOrEmpty()) {
                                    onNavigateToTarget(notif.targetScreen, notif.targetId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: AppNotification,
    onItemClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) appColors.redLight else appColors.cardBackground
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!notification.isRead) BloodRedPrimary.copy(alpha = 0.5f) else appColors.border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!notification.isRead) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when (notification.type) {
                            NotificationType.EMERGENCY -> appColors.redLight
                            NotificationType.APPOINTMENT -> appColors.yellowLight
                            NotificationType.ELIGIBILITY -> appColors.greenLight
                            NotificationType.CERTIFICATE -> appColors.yellowLight
                            NotificationType.SYSTEM -> appColors.surfaceVariant
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        NotificationType.EMERGENCY -> Icons.Default.Warning
                        NotificationType.APPOINTMENT -> Icons.Default.CalendarToday
                        NotificationType.ELIGIBILITY -> Icons.Default.Favorite
                        NotificationType.CERTIFICATE -> Icons.Default.CardGiftcard
                        NotificationType.SYSTEM -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (notification.type) {
                        NotificationType.EMERGENCY -> StatusUrgentRed
                        NotificationType.APPOINTMENT -> StatusWarningAmber
                        NotificationType.ELIGIBILITY -> StatusEligibleGreen
                        NotificationType.CERTIFICATE -> CertificateGold
                        NotificationType.SYSTEM -> appColors.textSecondary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = appColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(BloodRedPrimary, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = appColors.textSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.timestamp,
                        fontSize = 11.sp,
                        color = appColors.textMuted
                    )

                    if (!notification.targetScreen.isNullOrEmpty()) {
                        Text(
                            text = "View details →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BloodRedPrimary
                        )
                    }
                }
            }
        }
    }
}
