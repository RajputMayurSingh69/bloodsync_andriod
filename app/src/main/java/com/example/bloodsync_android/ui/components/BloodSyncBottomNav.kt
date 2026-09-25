package com.example.bloodsync_android.ui.components

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
import com.example.bloodsync_android.ui.theme.*

enum class AppNavDestination(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    HISTORY("History", Icons.Default.DateRange),
    HEALTH("Safety", Icons.Default.Favorite),
    APPOINTMENTS("Book", Icons.Default.CalendarToday),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun BloodSyncBottomNav(
    currentDestination: AppNavDestination,
    onNavigate: (AppNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
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
                    .padding(vertical = 6.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppNavDestination.entries.forEach { destination ->
                    val isSelected = destination == currentDestination
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
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = destination.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BloodRedPrimary else appColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
