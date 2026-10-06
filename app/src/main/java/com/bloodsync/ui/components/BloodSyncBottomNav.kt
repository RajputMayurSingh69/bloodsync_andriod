package com.bloodsync.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.ui.theme.*

enum class AppNavDestination(val icon: ImageVector, val bankIcon: ImageVector) {
    HOME(Icons.Default.Home, Icons.Default.Home),
    HISTORY(Icons.Default.DateRange, Icons.Default.Notifications),
    HEALTH(Icons.Default.Favorite, Icons.Default.List),
    APPOINTMENTS(Icons.Default.CalendarToday, Icons.Default.CalendarToday),
    PROFILE(Icons.Default.Person, Icons.Default.Person)
}

@Composable
fun BloodSyncBottomNav(
    currentDestination: AppNavDestination,
    onNavigate: (AppNavDestination) -> Unit,
    role: String = "user",
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors
    val strings = com.bloodsync.util.LocalAppStrings.current
    val isBank = role == "blood_bank"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = Color(0x15000000),
                    spotColor = Color(0x20000000)
                ),
            shape = RoundedCornerShape(36.dp),
            color = appColors.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppNavDestination.entries.forEach { destination ->
                    val isSelected = destination == currentDestination
                    val label = when (destination) {
                        AppNavDestination.HOME -> if (isBank) "Dashboard" else strings.navHome
                        AppNavDestination.HISTORY -> if (isBank) "SOS Requests" else strings.navHistory
                        AppNavDestination.HEALTH -> if (isBank) "Inventory" else strings.navSafety
                        AppNavDestination.APPOINTMENTS -> if (isBank) "Schedule" else strings.navBook
                        AppNavDestination.PROFILE -> strings.navProfile
                    }
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabScale"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) BloodRedPrimary else appColors.textMuted,
                        label = "iconTint"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(50.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(destination) }
                            )
                            .background(
                                color = if (isSelected) appColors.redLight else Color.Transparent,
                                shape = RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBank) destination.bankIcon else destination.icon,
                            contentDescription = label,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BloodRedPrimary else appColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
