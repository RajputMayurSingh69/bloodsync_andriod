package com.bloodsync.ui.screens.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.AppNotification
import com.bloodsync.data.model.NotificationType
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*

@Composable
fun NotificationCenterScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onNavigateToTarget: (targetScreen: String, targetId: String?) -> Unit
) {
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
                subtitle = if (unreadCount > 0) "$unreadCount unread alerts" else "All caught up",
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
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = appColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Android 13+ Notification Permission Banner
            AnimatedVisibility(visible = showPermissionBanner) {
                Surface(
                    color = BloodRedLight,
                    border = BorderStroke(0.5.dp, BloodRedPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BloodRedPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MedicalWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Enable Real-time Push Alerts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BloodRedPrimary
                                )
                                Text(
                                    text = "Receive instant alarms when urgent blood is needed nearby",
                                    fontSize = 11.sp,
                                    color = TextPrimaryDark
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
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Allow", fontSize = 12.sp, color = MedicalWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Category Filter Pills Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAllSelected = selectedFilter == null
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { selectedFilter = null },
                    shape = RoundedCornerShape(50.dp),
                    color = if (isAllSelected) appColors.primary else appColors.surface,
                    border = BorderStroke(1.dp, if (isAllSelected) appColors.primary else appColors.border)
                ) {
                    Text(
                        text = "All (${notifications.size})",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllSelected) Color.White else appColors.textSecondary
                    )
                }

                NotificationType.entries.filter { it != NotificationType.SYSTEM }.forEach { type ->
                    val isSelected = selectedFilter == type
                    val count = notifications.count { it.type == type }
                    val label = when (type) {
                        NotificationType.EMERGENCY -> "Emergency SOS"
                        NotificationType.APPOINTMENT -> "Appointments"
                        NotificationType.ELIGIBILITY -> "Eligibility"
                        NotificationType.CERTIFICATE -> "Certificates"
                        else -> "System"
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { selectedFilter = type },
                        shape = RoundedCornerShape(50.dp),
                        color = if (isSelected) appColors.primary else appColors.surface,
                        border = BorderStroke(1.dp, if (isSelected) appColors.primary else appColors.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else appColors.textSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.25f) else appColors.primaryLight
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color.White else appColors.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Notification Items or Empty State
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "No Notifications",
                        message = "You have no notifications in this category at the moment. All live alerts will appear here in real-time.",
                        icon = Icons.Default.NotificationsNone
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
private fun NotificationItemCard(
    notification: AppNotification,
    onItemClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    BloodSyncCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        backgroundColor = if (!notification.isRead) appColors.primaryLight.copy(alpha = 0.45f) else appColors.cardBackground,
        borderColor = if (!notification.isRead) appColors.primary.copy(alpha = 0.45f) else appColors.border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        when (notification.type) {
                            NotificationType.EMERGENCY -> BloodRedLight
                            NotificationType.APPOINTMENT -> StatusWarningAmberLight
                            NotificationType.ELIGIBILITY -> StatusEligibleGreenLight
                            NotificationType.CERTIFICATE -> StatusWarningAmberLight
                            NotificationType.SYSTEM -> WarmLightSurface
                        },
                        RoundedCornerShape(12.dp)
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
                        NotificationType.EMERGENCY -> BloodRedPrimary
                        NotificationType.APPOINTMENT -> StatusWarningAmber
                        NotificationType.ELIGIBILITY -> StatusEligibleGreen
                        NotificationType.CERTIFICATE -> CertificateGold
                        NotificationType.SYSTEM -> TextSecondaryGrey
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (!notification.isRead) FontWeight.ExtraBold else FontWeight.Bold,
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
