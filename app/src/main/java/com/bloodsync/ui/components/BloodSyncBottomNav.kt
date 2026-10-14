package com.bloodsync.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(0.03f),
                    spotColor = BloodRedPrimary.copy(alpha = 0.14f)
                ),
            shape = RoundedCornerShape(28.dp),
            color = appColors.bottomNavBackground,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = appColors.border
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppNavDestination.entries.forEach { destination ->
                    val isSelected = destination == currentDestination
                    val label = when (destination) {
                        AppNavDestination.HOME         -> if (isBank) "Dashboard" else strings.navHome
                        AppNavDestination.HISTORY      -> if (isBank) "SOS" else strings.navHistory
                        AppNavDestination.HEALTH       -> if (isBank) "Inventory" else strings.navSafety
                        AppNavDestination.APPOINTMENTS -> if (isBank) "Schedule" else strings.navBook
                        AppNavDestination.PROFILE      -> strings.navProfile
                    }

                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tabScale"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) appColors.primary else appColors.textMuted,
                        animationSpec = tween(200),
                        label = "iconTint"
                    )

                    val bgAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(200),
                        label = "bgAlpha"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(destination) }
                            )
                            .drawBehind {
                                if (bgAlpha > 0f) {
                                    drawRoundRect(
                                        color = appColors.primaryLight.copy(alpha = bgAlpha),
                                        cornerRadius = CornerRadius(40f, 40f)
                                    )
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isBank) destination.bankIcon else destination.icon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) appColors.primary else appColors.textMuted,
                            letterSpacing = 0.1.sp
                        )
                    }
                }
            }
        }
    }
}
