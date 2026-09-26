package com.example.bloodsync_android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bloodsync_android.ui.theme.BloodRedPrimary
import com.example.bloodsync_android.ui.theme.BloodSyncTheme
import com.example.bloodsync_android.util.AppLanguage
import com.example.bloodsync_android.util.LanguageManager

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val appColors = BloodSyncTheme.colors
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    val strings = remember(selectedLanguage) { LanguageManager.getStrings(selectedLanguage) }

    Dialog(
        onDismissRequest = { /* Require explicit confirmation or choice */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(26.dp),
            color = appColors.cardBackground,
            border = BorderStroke(1.dp, appColors.border),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(appColors.redLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = BloodRedPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = strings.chooseYourLanguage,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = strings.selectLanguageSubtitle,
                    fontSize = 13.sp,
                    color = appColors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3 Language Options
                val languages = listOf(
                    Triple(AppLanguage.ENGLISH, "English", "English (Default)"),
                    Triple(AppLanguage.HINDI, "हिंदी", "Hindi (भारतीय भाषा)"),
                    Triple(AppLanguage.GUJARATI, "ગુજરાતી", "Gujarati (સ્થાનિક ભાષા)")
                )

                languages.forEach { (lang, nativeLabel, subLabel) ->
                    val isSelected = selectedLanguage == lang
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) BloodRedPrimary else appColors.border,
                        animationSpec = spring(),
                        label = "langBorder"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) appColors.redLight else appColors.surfaceVariant,
                        animationSpec = spring(),
                        label = "langBg"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                selectedLanguage = lang
                                onLanguageSelected(lang)
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = bgColor,
                        border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nativeLabel,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) BloodRedPrimary else appColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subLabel,
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary
                                )
                            }

                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isSelected) BloodRedPrimary else appColors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Continue / Confirm Button (50.dp Tactile Pill)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BloodRedPrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = strings.continueButton,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
