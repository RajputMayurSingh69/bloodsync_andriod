package com.example.bloodsync_android.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.bloodsync_android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        val dropOffsetY = remember { Animatable(-180f) }
        val dropScaleY = remember { Animatable(1.2f) }
        val dropScaleX = remember { Animatable(0.85f) }
        val fillProgress = remember { Animatable(0f) }
        val rippleScale = remember { Animatable(0.6f) }
        val rippleAlpha = remember { Animatable(0f) }
        val contentAlpha = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            // Step 1: Drop falls smoothly and bounces slightly
            launch {
                dropOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(650, easing = FastOutSlowInEasing)
                )
                // Impact squash & stretch
                launch {
                    dropScaleY.animateTo(0.82f, tween(100))
                    dropScaleY.animateTo(1.06f, tween(120))
                    dropScaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                launch {
                    dropScaleX.animateTo(1.22f, tween(100))
                    dropScaleX.animateTo(0.95f, tween(120))
                    dropScaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }

                // Ripple triggers upon landing
                launch {
                    rippleAlpha.snapTo(0.6f)
                    rippleAlpha.animateTo(0f, tween(700))
                }
                launch {
                    rippleScale.animateTo(1.7f, tween(700, easing = LinearOutSlowInEasing))
                }
            }

            // Step 2: Liquid fill & pulse
            delay(350)
            launch {
                fillProgress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
            }

            // Step 3: Text fade & rise
            delay(500)
            launch {
                contentAlpha.animateTo(1f, tween(600, easing = EaseOutCubic))
            }

            // Total splash duration: ~2.2 seconds, then auto-navigate
            delay(2200)
            onSplashComplete()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Blood Drop Animation Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                // Expanding landing ripple
                if (rippleAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(rippleScale.value)
                            .background(
                                color = BloodRedPrimary.copy(alpha = rippleAlpha.value),
                                shape = CircleShape
                            )
                    )
                }

                // Animated Blood Drop
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(y = dropOffsetY.value.dp)
                        .scale(scaleX = dropScaleX.value, scaleY = dropScaleY.value)
                ) {
                    val w = size.width
                    val h = size.height

                    // Construct teardrop path
                    val dropPath = Path().apply {
                        moveTo(w / 2f, 0f)
                        cubicTo(
                            w * 0.45f, h * 0.28f,
                            0f, h * 0.55f,
                            0f, h * 0.72f
                        )
                        cubicTo(
                            0f, h * 0.96f,
                            w * 0.22f, h,
                            w / 2f, h
                        )
                        cubicTo(
                            w * 0.78f, h,
                            w, h * 0.96f,
                            w, h * 0.72f
                        )
                        cubicTo(
                            w, h * 0.55f,
                            w * 0.55f, h * 0.28f,
                            w / 2f, 0f
                        )
                        close()
                    }

                    // Clip to the drop path
                    drawContext.canvas.save()
                    drawContext.canvas.clipPath(dropPath)

                    // Background unfilled droplet (soft tint)
                    drawRect(color = Color(0xFFFFEBEE))

                    // Rising liquid fill level
                    val fillHeight = h * fillProgress.value
                    val fillTopY = h - fillHeight

                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE53935), BloodRedDark),
                        startY = fillTopY,
                        endY = h
                    )

                    drawRect(
                        brush = gradientBrush,
                        topLeft = Offset(0f, fillTopY),
                        size = androidx.compose.ui.geometry.Size(w, fillHeight)
                    )

                    // Glossy shine highlight on upper-left
                    val shinePath = Path().apply {
                        moveTo(w * 0.30f, h * 0.32f)
                        cubicTo(
                            w * 0.18f, h * 0.48f,
                            w * 0.16f, h * 0.65f,
                            w * 0.22f, h * 0.72f
                        )
                    }
                    drawPath(
                        path = shinePath,
                        color = Color.White.copy(alpha = 0.5f),
                        style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
                    )

                    drawContext.canvas.restore()

                    // Crisp droplet outline
                    drawPath(
                        path = dropPath,
                        color = BloodRedDark.copy(alpha = 0.25f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Name & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha.value)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Blood",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = BloodRedPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Sync",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Connecting Life Savers in Real Time",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = appColors.textSecondary,
                    letterSpacing = 0.2.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Clinical grade subtle loader dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotAlpha by rememberInfiniteTransition(label = "dots").animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(BloodRedPrimary.copy(alpha = dotAlpha), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(BloodRedPrimary.copy(alpha = 1f - dotAlpha + 0.3f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(BloodRedPrimary.copy(alpha = dotAlpha), CircleShape)
                    )
                }
            }
        }
    }
}
