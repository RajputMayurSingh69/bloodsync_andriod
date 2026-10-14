package com.bloodsync.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium BloodSync Splash Screen
 * Blood drop physics animation with liquid fill effect.
 * Seamless edge-to-edge on dark & light modes.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val appColors = BloodSyncTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background),
        contentAlignment = Alignment.Center
    ) {
        // Animation state
        val dropOffsetY = remember { Animatable(-200f) }
        val dropScaleY  = remember { Animatable(1.2f) }
        val dropScaleX  = remember { Animatable(0.85f) }
        val fillProgress= remember { Animatable(0f) }
        val rippleScale = remember { Animatable(0.6f) }
        val rippleAlpha = remember { Animatable(0f) }
        val contentAlpha= remember { Animatable(0f) }
        val logoTranslateY = remember { Animatable(16f) }

        LaunchedEffect(Unit) {
            // Step 1: Drop falls with elastic bounce
            launch {
                dropOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(680, easing = FastOutSlowInEasing)
                )
                // Impact squash & stretch
                launch {
                    dropScaleY.animateTo(0.78f, tween(90))
                    dropScaleY.animateTo(1.10f, tween(110))
                    dropScaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                launch {
                    dropScaleX.animateTo(1.28f, tween(90))
                    dropScaleX.animateTo(0.92f, tween(110))
                    dropScaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                // Landing ripple
                launch {
                    rippleAlpha.snapTo(0.55f)
                    rippleAlpha.animateTo(0f, tween(750, easing = LinearOutSlowInEasing))
                }
                launch {
                    rippleScale.animateTo(1.8f, tween(750, easing = LinearOutSlowInEasing))
                }
            }

            // Step 2: Liquid fill rises
            delay(320)
            launch {
                fillProgress.animateTo(1f, tween(820, easing = FastOutSlowInEasing))
            }

            // Step 3: Title & tagline appear with upward slide
            delay(480)
            launch {
                contentAlpha.animateTo(1f, tween(680, easing = EaseOutCubic))
            }
            launch {
                logoTranslateY.animateTo(0f, tween(680, easing = EaseOutCubic))
            }

            // Auto-navigate after 2.4 seconds
            delay(2400)
            onSplashComplete()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Soft Red Glow & Blood Drop Animation ────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Soft ambient radial glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(BloodRedLight.copy(alpha = 0.8f), Color.Transparent),
                                radius = 180f
                            ),
                            shape = CircleShape
                        )
                )

                // Landing ripple ring
                if (rippleAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(rippleScale.value)
                            .background(
                                color = BloodRedPrimary.copy(alpha = rippleAlpha.value * 0.4f),
                                shape = CircleShape
                            )
                    )
                }

                // The drop itself
                Canvas(
                    modifier = Modifier
                        .size(86.dp)
                        .offset(y = dropOffsetY.value.dp)
                        .scale(scaleX = dropScaleX.value, scaleY = dropScaleY.value)
                ) {
                    val w = size.width
                    val h = size.height

                    val dropPath = Path().apply {
                        moveTo(w / 2f, 0f)
                        cubicTo(w * 0.45f, h * 0.27f, 0f, h * 0.54f, 0f, h * 0.72f)
                        cubicTo(0f, h * 0.96f, w * 0.22f, h, w / 2f, h)
                        cubicTo(w * 0.78f, h, w, h * 0.96f, w, h * 0.72f)
                        cubicTo(w, h * 0.54f, w * 0.55f, h * 0.27f, w / 2f, 0f)
                        close()
                    }

                    drawContext.canvas.save()
                    drawContext.canvas.clipPath(dropPath)

                    // Background shell (soft tint)
                    drawRect(color = BloodRedLight)

                    // Liquid fill rising from bottom
                    val fillH = h * fillProgress.value
                    val fillTopY = h - fillH
                    val gradient = Brush.verticalGradient(
                        colors = listOf(BloodRedSecondary, BloodRedDark),
                        startY = fillTopY,
                        endY = h
                    )
                    if (fillH > 0f) {
                        drawRect(
                            brush = gradient,
                            topLeft = Offset(0f, fillTopY),
                            size = androidx.compose.ui.geometry.Size(w, fillH)
                        )
                    }

                    // Glossy highlight stroke
                    val shinePath = Path().apply {
                        moveTo(w * 0.30f, h * 0.30f)
                        cubicTo(w * 0.17f, h * 0.47f, w * 0.15f, h * 0.65f, w * 0.21f, h * 0.74f)
                    }
                    drawPath(
                        path = shinePath,
                        color = Color.White.copy(alpha = if (fillProgress.value > 0.3f) 0.55f else 0.25f),
                        style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
                    )

                    drawContext.canvas.restore()

                    // Drop outline
                    drawPath(
                        path = dropPath,
                        color = BloodRedDark.copy(alpha = 0.20f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── App Name & Tagline ───────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .offset(y = logoTranslateY.value.dp)
            ) {
                // Logo text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Blood",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = BloodRedPrimary,
                        letterSpacing = (-0.8).sp
                    )
                    Text(
                        text = "Sync",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        letterSpacing = (-0.8).sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Every drop creates a second chance.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = appColors.textSecondary,
                    letterSpacing = 0.1.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Animated loading dots (staggered)
                AnimatedLoadingDots()
            }
        }
    }
}

@Composable
private fun AnimatedLoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "loadDots")

    @Composable
    fun dot(delayMs: Int): Float {
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delayMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotAlpha$delayMs"
        )
        return alpha
    }

    val a1 = dot(0)
    val a2 = dot(200)
    val a3 = dot(400)

    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(a1, a2, a3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(BloodRedPrimary, CircleShape)
            )
        }
    }
}
