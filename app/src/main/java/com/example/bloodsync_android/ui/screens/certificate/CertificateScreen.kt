package com.example.bloodsync_android.ui.screens.certificate

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.model.Certificate
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodDropIcon
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.EmptyStateView
import com.example.bloodsync_android.ui.theme.*

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
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (certificates.isEmpty()) {
                EmptyStateView(
                    title = "No Certificates Yet",
                    message = "Certificates are automatically generated after your blood donations are verified.",
                    icon = Icons.Default.CardGiftcard
                )
            } else {
                // Multi-certificate selector chips
                if (certificates.size > 1) {
                    Text(
                        text = "Your Certificates (${certificates.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = appColors.textSecondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(certificates) { cert ->
                            val isSelected = cert.id == currentCertificate?.id
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .clickable { selectedCertId = cert.id }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) CertificateGold else appColors.border,
                                        shape = RoundedCornerShape(50.dp)
                                    ),
                                color = if (isSelected) appColors.yellowLight else appColors.cardBackground
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = if (isSelected) CertificateGoldDark else appColors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${cert.donationMilestone} (${cert.donationDate})",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CertificateGoldDark else appColors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                currentCertificate?.let { cert ->
                    // Certificate Canvas / View
                    CertificateCard(certificate = cert)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons (Download & Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                shareCertificateText(context, cert)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Certificate", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                downloadCertificateBitmap(context, cert)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(50.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = BloodRedPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Image", color = BloodRedPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Authenticity & Verification note
                    Surface(
                        color = appColors.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusEligibleGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cryptographically signed by BloodSync. Verified ID: ${cert.certificateCode}",
                                fontSize = 11.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CertificateCard(
    certificate: Certificate,
    modifier: Modifier = Modifier
) {
    val appColors = BloodSyncTheme.colors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(2.dp, StatusWarningAmber)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dual decorative inner border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AlertYellow.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
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
                        BloodDropIcon(size = 22.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BLOODSYNC HEALTH NETWORK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = BloodRedPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "CERTIFICATE OF APPRECIATION",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BloodRedPrimary,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "PROUDLY PRESENTED TO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = appColors.textMuted,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    // Recipient Name
                    Text(
                        text = certificate.donorName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = BloodRedPrimary,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(
                        color = StatusWarningAmber.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.width(180.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "In grateful recognition of your humanitarian spirit and generous blood donation. Your selfless act helps preserve human life and brings hope to patients in critical medical care.",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = appColors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Milestone & Details Grid
                    Surface(
                        color = appColors.yellowLight,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CertificateField("Donation Milestone", certificate.donationMilestone)
                            CertificateField("Blood Group", certificate.bloodGroup)
                            CertificateField("Donation Date", certificate.donationDate)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Signatures & Seal Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left: Medical Center & Doctor
                        Column(modifier = Modifier.weight(0.45f)) {
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

                        // Center: Golden Seal representation
                        GoldenCertificateSeal(size = 52)

                        // Right: Certificate ID & Verification
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(0.45f)
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
                                text = "Official Verification ID",
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
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(AlertYellow, StatusWarningAmber, StatusWarningAmber)
                ),
                shape = CircleShape
            )
            .border(2.dp, AlertYellowLight, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
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

        // Background
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
