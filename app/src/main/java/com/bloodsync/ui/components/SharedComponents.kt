package com.bloodsync.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.ui.theme.*

// =======================================================================
// BLOODSYNC PREMIUM REUSABLE UI DESIGN SYSTEM (LIGHT ONLY)
// 24dp Soft Cards • 50dp Pill Buttons • 16dp Soft Inputs • 120 FPS Springs
// =======================================================================

/**
 * Premium 24dp Card with soft pink border and subtle red-tinted elevation shadow.
 */
@Composable
fun BloodSyncCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
    border: BorderStroke? = null,
    elevation: Dp = 6.dp,
    shadowColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val appColors = BloodSyncTheme.colors
    val effectiveBg = if (backgroundColor != Color.Unspecified) backgroundColor else appColors.cardBackground
    val effectiveBorderColor = if (borderColor != Color.Unspecified) borderColor else appColors.border
    val effectiveShadowColor = if (shadowColor != Color.Unspecified) shadowColor else appColors.cardShadow

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val appliedBorder = border ?: BorderStroke(borderWidth, effectiveBorderColor)

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = effectiveShadowColor
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = appColors.primary.copy(alpha = 0.12f)),
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = effectiveBg,
        border = appliedBorder
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

/**
 * Fully rounded 50dp Pill Button with smooth red gradient and spring scale interaction.
 */
@Composable
fun BloodSyncButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
    height: Dp = 54.dp,
    gradient: Brush = BloodRedGradientBrush
) {
    val effectiveIcon = leadingIcon ?: icon
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btnScale"
    )

    val shape = RoundedCornerShape(50.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled && !isLoading) 8.dp else 0.dp,
                shape = shape,
                spotColor = BloodRedPrimary.copy(alpha = 0.35f),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(shape)
            .background(
                if (enabled && !isLoading) gradient
                else Brush.horizontalGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)))
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (effectiveIcon != null) {
                    Icon(
                        imageVector = effectiveIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

/**
 * Outlined Pill Button with white background and prominent red stroke.
 */
@Composable
fun BloodSyncOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    textColor: Color = BloodRedPrimary,
    borderColor: Color = BloodRedPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "outlinedBtnScale"
    )

    val appColors = BloodSyncTheme.colors
    val shape = RoundedCornerShape(50.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(appColors.surface)
            .border(BorderStroke(1.5.dp, if (enabled) borderColor else appColors.border), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = appColors.primary.copy(alpha = 0.12f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) textColor else MedicalTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) textColor else MedicalTextMuted
            )
        }
    }
}

/**
 * Pill-shaped Filter Chip with smooth spring state transitions.
 */
@Composable
fun BloodSyncChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipScale"
    )

    val appColors = BloodSyncTheme.colors
    val shape = RoundedCornerShape(50.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .shadow(4.dp, shape, spotColor = appColors.primary.copy(alpha = 0.30f))
                        .background(BloodRedGradientBrush)
                } else {
                    Modifier
                        .background(appColors.surface)
                        .border(BorderStroke(1.dp, appColors.border), shape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = appColors.primary.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) Color.White else appColors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else appColors.textSecondary
            )
        }
    }
}

/**
 * 16dp Soft Rounded Outlined TextField with gentle pink border & focused red glow.
 */
@Composable
fun BloodSyncTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    val appColors = BloodSyncTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, fontSize = 13.sp) },
            placeholder = { if (placeholder.isNotBlank()) Text(placeholder, color = appColors.textMuted, fontSize = 13.sp) },
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isError) StatusUrgentRed else appColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            readOnly = readOnly,
            enabled = enabled,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = appColors.surface,
                unfocusedContainerColor = appColors.inputBackground,
                disabledContainerColor = appColors.surfaceVariant,
                errorContainerColor = if (appColors.isDark) Color(0xFF2A1517) else Color(0xFFFFF5F5),
                focusedBorderColor = appColors.primary,
                unfocusedBorderColor = appColors.border,
                errorBorderColor = StatusUrgentRed,
                focusedLabelColor = appColors.primary,
                unfocusedLabelColor = appColors.textSecondary,
                cursorColor = appColors.primary,
                focusedTextColor = appColors.textPrimary,
                unfocusedTextColor = appColors.textPrimary
            )
        )

        AnimatedVisibility(visible = isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage ?: "",
                color = StatusUrgentRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Section Header with title, subtitle, and optional right action.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val appColors = BloodSyncTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                letterSpacing = (-0.2).sp
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = appColors.textMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (!actionText.isNullOrBlank() && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.primary
                )
            }
        }
    }
}

/**
 * Shimmer Loading Box with gentle pink-white gradient.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val appColors = BloodSyncTheme.colors
    val shimmerColors = listOf(
        appColors.shimmerBase,
        appColors.shimmerHighlight,
        appColors.shimmerBase
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * Smooth Animated Number Counter for KPIs (Donors Online, Units, Lives Saved).
 */
@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    suffix: String = "",
    color: Color = MedicalTextPrimary,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    Text(
        text = "${animatedValue.value.toInt()}$suffix",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    fontSize: Int,
    suffix: String = "",
    color: Color = MedicalTextPrimary,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    AnimatedCounter(
        targetValue = targetValue,
        modifier = modifier,
        fontSize = fontSize.sp,
        suffix = suffix,
        color = color,
        fontWeight = fontWeight
    )
}

/**
 * Illustrated Soft Empty State Card.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val appColors = BloodSyncTheme.colors
    BloodSyncCard(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(appColors.primaryLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = appColors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = appColors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            if (!actionText.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                BloodSyncButton(
                    text = actionText,
                    onClick = onActionClick,
                    height = 46.dp,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
        }
    }
}

/**
 * Soft Pill Status Badge with pulse live beacon dot.
 */
@Composable
fun StatusPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = StatusEligibleGreenLight,
    contentColor: Color = StatusEligibleGreen,
    textColor: Color = contentColor,
    showBeacon: Boolean = true
) {
    val effectiveTextColor = if (textColor != contentColor) textColor else contentColor
    val infiniteTransition = rememberInfiniteTransition(label = "beaconPulse")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = backgroundColor,
        border = BorderStroke(0.5.dp, effectiveTextColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (showBeacon) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { alpha = beaconAlpha }
                        .background(effectiveTextColor, CircleShape)
                )
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = effectiveTextColor
            )
        }
    }
}
