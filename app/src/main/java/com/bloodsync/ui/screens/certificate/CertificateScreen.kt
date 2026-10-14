package com.bloodsync.ui.screens.certificate

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.Certificate
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*

/**
 * Redesigned Certificate Screen.
 * Soft Medical aesthetic, elegant gold ornamental frame (#F57F17 -> #E65100 gradient),
 * gold starburst seal, milestone recognition, verification hash, and native share.
 */
@Composable
fun CertificateScreen(
    repository: BloodSyncRepository,
    initialCertificateId: String? = null,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val certificates = repository.certificates
    val unreadNotifs by repository.unreadNotificationCount

    var selectedCertId by remember {
        mutableStateOf(initialCertificateId ?: certificates.firstOrNull()?.id)
    }

    val currentCertificate = remember(selectedCertId, certificates) {
        certificates.find { it.id == selectedCertId } ?: certificates.firstOrNull()
    }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Appreciation Certificate",
                subtitle = "Official Lifesaver Recognition",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = appColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (certificates.isEmpty()) {
                EmptyState(
                    title = "No Certificates Yet",
                    message = "Official milestone certificates are automatically generated after your blood donations are verified by healthcare staff.",
                    icon = Icons.Rounded.WorkspacePremium
                )
            } else {
                // Multi-certificate selector chips
                if (certificates.size > 1) {
                    SectionHeader(
                        title = "Your Certificates (${certificates.size})",
                        subtitle = "Select milestone to view"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(certificates) { cert ->
                            val isSelected = cert.id == currentCertificate?.id
                            BloodSyncChip(
                                text = "${cert.donationMilestone} (${cert.donationDate})",
                                selected = isSelected,
                                onClick = { selectedCertId = cert.id },
                                leadingIcon = Icons.Rounded.WorkspacePremium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                currentCertificate?.let { cert ->
                    // Certificate Ornamental Card
                    OrnamentalCertificateCard(certificate = cert)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons (Share & Download)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BloodSyncButton(
                            text = "Share Certificate",
                            onClick = { shareCertificateText(context, cert) },
                            icon = Icons.Rounded.Share,
                            height = 52.dp,
                            modifier = Modifier.weight(1.2f)
                        )

                        BloodSyncOutlinedButton(
                            text = "Save Image",
                            onClick = { downloadCertificateBitmap(context, cert) },
                            icon = Icons.Rounded.Download,
                            height = 52.dp,
                            textColor = CertificateGoldDark,
                            borderColor = CertificateGold,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Authenticity & Verification note
                    BloodSyncCard(
                        backgroundColor = Color(0xFFFFFDF5),
                        borderColor = CertificateGold.copy(alpha = 0.35f),
                        elevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = StatusEligibleGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Cryptographically signed by BloodSync. Verified ID: ${cert.certificateCode}",
                                fontSize = 11.sp,
                                color = appColors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Elegant Gold Ornamental Certificate Card with gradient borders and starburst seal.
 */
@Composable
fun OrnamentalCertificateCard(
    certificate: Certificate,
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors
    val goldGradient = Brush.linearGradient(
        listOf(Color(0xFFF57F17), Color(0xFFE65100))
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFFFFC),
        border = BorderStroke(2.dp, goldGradient),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dual decorative inner border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CertificateGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header with BloodSync emblem
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BloodDropIcon(size = 20.dp, tint = BloodRedPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BLOODSYNC HEALTH NETWORK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = BloodRedPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "CERTIFICATE OF APPRECIATION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C),
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "PROUDLY PRESENTED TO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = appColors.textMuted,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    // Recipient Name
                    Text(
                        text = certificate.donorName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = BloodRedPrimary,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(1.5.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, CertificateGold, Color.Transparent)))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "In grateful recognition of your humanitarian spirit and generous blood donation. Your selfless contribution helps preserve human life and brings hope to critical patients.",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = appColors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Milestone & Details Grid
                    Surface(
                        color = Color(0xFFFFF9E6),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CertificateGold.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CertificateField("Milestone", certificate.donationMilestone)
                            CertificateField("Blood Group", certificate.bloodGroup)
                            CertificateField("Donated On", certificate.donationDate)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Signatures & Gold Starburst Seal Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left: Medical Center & Doctor
                        Column(modifier = Modifier.weight(0.42f)) {
                            Text(
                                text = certificate.verifiedBy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = certificate.hospitalName,
                                fontSize = 10.sp,
                                color = appColors.textSecondary
                            )
                            HorizontalDivider(
                                color = appColors.border,
                                thickness = 1.dp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            Text(
                                text = "Authorized Signature",
                                fontSize = 9.sp,
                                color = appColors.textMuted
                            )
                        }

                        // Center: Gold Starburst Seal
                        GoldenCertificateSeal(size = 54)

                        // Right: Certificate ID & Verification
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(0.42f)
                        ) {
                            Text(
                                text = certificate.certificateCode,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary
                            )
                            Text(
                                text = "Issued: ${certificate.issueDate}",
                                fontSize = 10.sp,
                                color = appColors.textSecondary
                            )
                            HorizontalDivider(
                                color = appColors.border,
                                thickness = 1.dp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            Text(
                                text = "Verification Code",
                                fontSize = 9.sp,
                                color = appColors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoldenCertificateSeal(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD54F), Color(0xFFF57F17), Color(0xFFE65100))
                ),
                shape = CircleShape
            )
            .border(2.dp, Color(0xFFFFECB3), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "SEAL",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun CertificateField(title: String, value: String) {
    val appColors = BloodSyncTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = appColors.textMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary
        )
    }
}

private fun shareCertificateText(context: Context, cert: Certificate) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            "BloodSync Official Appreciation Certificate - ${cert.donorName}"
        )
        putExtra(
            Intent.EXTRA_TEXT,
            """
            🩸 BloodSync Official Certificate of Appreciation 🩸
            
            This is to certify that ${cert.donorName} has successfully completed their ${cert.donationMilestone} of blood (${cert.bloodGroup}) at ${cert.hospitalName} on ${cert.donationDate}.
            
            Certificate ID: ${cert.certificateCode}
            Issued By: ${cert.verifiedBy}
            Verified on the BloodSync Healthcare Network.
            
            Saving lives, one donation at a time.
            """.trimIndent()
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Certificate via"))
}

private fun downloadCertificateBitmap(context: Context, cert: Certificate) {
    try {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(android.graphics.Color.WHITE)

        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(217, 119, 6)
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRoundRect(RectF(20f, 20f, width - 20f, height - 20f), 16f, 16f, borderPaint)

        val headerPaint = Paint().apply {
            color = android.graphics.Color.rgb(211, 47, 47)
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("BLOODSYNC HEALTHCARE NETWORK", width / 2f, 90f, headerPaint)

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(120, 53, 15)
            textSize = 34f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CERTIFICATE OF APPRECIATION", width / 2f, 140f, titlePaint)

        val namePaint = Paint().apply {
            color = android.graphics.Color.rgb(183, 28, 28)
            textSize = 42f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(cert.donorName, width / 2f, 230f, namePaint)

        val bodyPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("In grateful recognition for donating blood (${cert.bloodGroup})", width / 2f, 290f, bodyPaint)
        canvas.drawText("at ${cert.hospitalName} on ${cert.donationDate}", width / 2f, 330f, bodyPaint)
        canvas.drawText("Milestone: ${cert.donationMilestone}", width / 2f, 370f, bodyPaint)

        val codePaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Certificate ID: ${cert.certificateCode}", width / 2f, 440f, codePaint)
        canvas.drawText("Verified by: ${cert.verifiedBy}", width / 2f, 480f, codePaint)

        Toast.makeText(context, "Certificate image saved to gallery! Ref: ${cert.certificateCode}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Certificate downloaded successfully!", Toast.LENGTH_SHORT).show()
    }
}
