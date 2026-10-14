package com.bloodsync.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.ui.theme.*

// =======================================================================
// EMERGENCY FLOATING BUTTON — Dual-ripple pulse animation (GPU-only)
// Smooth 24/7 SOS visibility with eye-catching but not distracting pulse.
// =======================================================================
@Composable
fun EmergencyFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")

    // Radar wave expanding from 56dp to 92dp (scale 1f to 1.64f), alpha 0.45f to 0f
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.64f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    // Secondary ripple with 500ms delay
    val radarScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.64f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale2"
    )
    val radarAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha2"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Outer radar pulse 1
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = radarScale
                    scaleY = radarScale
                    alpha = radarAlpha
                }
                .background(
                    color = BloodRedPrimary,
                    shape = CircleShape
                )
        )

        // Outer radar pulse 2
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = radarScale2
                    scaleY = radarScale2
                    alpha = radarAlpha2
                }
                .background(
                    color = BloodRedPrimary,
                    shape = CircleShape
                )
        )

        // Main SOS FAB
        Surface(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = BloodRedPrimary.copy(alpha = 0.25f),
                    spotColor = BloodRedPrimary.copy(alpha = 0.40f)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BloodRedSecondary, BloodRedDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BloodDropIcon(
                        size = 18.dp,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "SOS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}

// =======================================================================
// EMERGENCY BANNER CARD — Soft medical emergency prompt
// =======================================================================
@Composable
fun EmergencyBannerCard(
    onTriggerEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onTriggerEmergency),
        color = appColors.primaryLight,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
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
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(BloodRedSecondary, BloodRedDark)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "24/7 Emergency Request",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = appColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Broadcast urgent blood need instantly",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            Surface(
                color = BloodRedPrimary,
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = "Request SOS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
