package com.example.bloodsync_android.ui.components

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
import com.example.bloodsync_android.ui.theme.BloodRedPrimary
import com.example.bloodsync_android.ui.theme.BloodSyncTheme

/**
 * Beautiful exit confirmation dialog displayed when the user presses the system Back button
 * from the app's root screen.
 */
@Composable
fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = BloodSyncTheme.colors
    val strings = com.example.bloodsync_android.util.LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.cardBackground,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BloodRedPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Exit App",
                    tint = BloodRedPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = strings.exitAppTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = strings.exitAppMessage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BloodRedPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = strings.yesExit,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.textPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = strings.noStay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = colors.textPrimary
                )
            }
        }
    )
}
