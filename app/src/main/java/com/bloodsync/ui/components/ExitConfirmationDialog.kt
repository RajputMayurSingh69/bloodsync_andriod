package com.bloodsync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.bloodsync.ui.theme.BloodSyncTheme

/**
 * Beautiful rounded confirmation dialog displayed for app exit, logout, or cache clearing.
 * Styled with soft medical red palette and 50dp pill buttons.
 */
@Composable
fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit = {},
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null
) {
    val colors = BloodSyncTheme.colors
    val strings = com.bloodsync.util.LocalAppStrings.current

    val effectiveTitle = title ?: strings.exitAppTitle
    val effectiveMessage = message ?: strings.exitAppMessage
    val effectiveConfirmText = confirmText ?: strings.yesExit
    val effectiveDismissText = dismissText ?: strings.noStay
    val effectiveConfirmAction = onConfirm ?: onConfirmExit

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surface,
        modifier = Modifier.padding(16.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.primaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = effectiveTitle,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = effectiveTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = effectiveMessage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (effectiveDismissText.isNotBlank()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = effectiveDismissText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                    }
                }

                Button(
                    onClick = effectiveConfirmAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = effectiveConfirmText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        dismissButton = {}
    )
}
