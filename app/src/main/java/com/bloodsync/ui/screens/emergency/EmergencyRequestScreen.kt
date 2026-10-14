package com.bloodsync.ui.screens.emergency

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.UrgencyLevel
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*
import com.bloodsync.util.LocationHelper
import com.bloodsync.util.ValidationHelper

/**
 * Redesigned Emergency Request Screen.
 * Soft Medical Red & White theme with 24dp cards, 1dp soft pink borders,
 * 8-chip blood group grid, units stepper, color-coded urgency selector,
 * GPS 10km radar card, and full-width glowing SOS broadcast button.
 */
@Composable
fun EmergencyRequestScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onRequestCreated: (requestId: String) -> Unit,
    onNotificationClick: () -> Unit
) {
    val context = LocalContext.current
    val unreadNotifs by repository.unreadNotificationCount

    // User fills details directly
    var patientName by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") }
    var unitsRequired by remember { mutableIntStateOf(1) }
    var urgencyLevel by remember { mutableStateOf<UrgencyLevel?>(null) }
    var hospitalName by remember { mutableStateOf("") }
    var hospitalAddress by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }

    // GPS 10km Radar Coordinates Capture State
    var capturedLatitude by remember { mutableStateOf<Double?>(null) }
    var capturedLongitude by remember { mutableStateOf<Double?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var locationStatusMessage by remember { mutableStateOf<String?>("GPS Coordinates ready for Radar") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            isLocating = true
            LocationHelper.getCurrentLocation(
                context = context,
                onLocation = { lat, lon ->
                    capturedLatitude = lat
                    capturedLongitude = lon
                    isLocating = false
                    locationStatusMessage = "GPS Fixed: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
                },
                onError = { err ->
                    isLocating = false
                    locationStatusMessage = "Location unavailable ($err)"
                }
            )
        } else {
            isLocating = false
            locationStatusMessage = "Location permission denied"
        }
    }

    // Auto-detect GPS coordinates on screen entry if permission granted
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            isLocating = true
            LocationHelper.getCurrentLocation(
                context = context,
                onLocation = { lat, lon ->
                    capturedLatitude = lat
                    capturedLongitude = lon
                    isLocating = false
                    locationStatusMessage = "GPS Fixed: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
                },
                onError = {
                    isLocating = false
                }
            )
        }
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val appColors = BloodSyncTheme.colors
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Emergency Request",
                subtitle = "Fast SOS Broadcast",
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
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Live SOS Beacon Header Banner ─────────────────────────────────
            val beaconTransition = rememberInfiniteTransition(label = "beacon")
            val beaconAlpha by beaconTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(750, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "beaconAlpha"
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(BloodRedGradientStart, BloodRedGradientEnd)
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Emergency,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .graphicsLayer { alpha = beaconAlpha }
                                        .background(Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE SOS BROADCAST",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    letterSpacing = 0.6.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Instantly notifies matching registered donors nearby.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // ── Card 1: Blood Group (8-chip grid) & Units Stepper ─────────────
            BloodSyncCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                BloodGroupSelector(
                    selectedGroup = selectedGroup,
                    onGroupSelected = {
                        selectedGroup = it
                        if (validationError?.contains("blood group", ignoreCase = true) == true) {
                            validationError = null
                        }
                    },
                    title = "Blood Group Needed *"
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = appColors.border, thickness = 0.75.dp)
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Units Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = appColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Whole blood bags (450 ml each)",
                            fontSize = 12.sp,
                            color = appColors.textMuted
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = unitsRequired > 1) {
                                    if (unitsRequired > 1) unitsRequired--
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (unitsRequired > 1) appColors.redLight else appColors.surfaceVariant,
                            border = BorderStroke(1.dp, if (unitsRequired > 1) BloodRedPrimary.copy(alpha = 0.25f) else appColors.border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Remove,
                                    contentDescription = "Decrease",
                                    tint = if (unitsRequired > 1) BloodRedPrimary else appColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .height(40.dp)
                                .background(appColors.surfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, appColors.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$unitsRequired",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = BloodRedPrimary
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = unitsRequired < 10) {
                                    if (unitsRequired < 10) unitsRequired++
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (unitsRequired < 10) appColors.redLight else appColors.surfaceVariant,
                            border = BorderStroke(1.dp, if (unitsRequired < 10) BloodRedPrimary.copy(alpha = 0.25f) else appColors.border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Increase",
                                    tint = if (unitsRequired < 10) BloodRedPrimary else appColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Card 2: Urgency Level Selector ────────────────────────────────
            BloodSyncCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Urgency Level *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = appColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Helps donors prioritize response timeline",
                    fontSize = 12.sp,
                    color = appColors.textMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UrgencyLevel.entries.forEach { level ->
                        val isSelected = urgencyLevel == level
                        val activeColor = when (level) {
                            UrgencyLevel.IMMEDIATE -> BloodRedPrimary
                            UrgencyLevel.URGENT -> StatusWarningAmber
                            UrgencyLevel.WITHIN_24_HOURS -> StatusEligibleGreen
                        }
                        val activeBg = when (level) {
                            UrgencyLevel.IMMEDIATE -> appColors.redLight
                            UrgencyLevel.URGENT -> StatusWarningAmberLight
                            UrgencyLevel.WITHIN_24_HOURS -> appColors.greenLight
                        }

                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.03f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "urgencyScale"
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .scale(scale)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    urgencyLevel = level
                                    if (validationError?.contains("urgency", ignoreCase = true) == true) {
                                        validationError = null
                                    }
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) activeColor else appColors.border,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            color = if (isSelected) activeBg else appColors.cardBackground,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (level) {
                                        UrgencyLevel.IMMEDIATE -> "Critical"
                                        UrgencyLevel.URGENT -> "Urgent"
                                        UrgencyLevel.WITHIN_24_HOURS -> "Standard"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) activeColor else appColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = when (level) {
                                        UrgencyLevel.IMMEDIATE -> "< 1 hr"
                                        UrgencyLevel.URGENT -> "< 3 hrs"
                                        UrgencyLevel.WITHIN_24_HOURS -> "< 24 hrs"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) activeColor else appColors.textMuted
                                )
                            }
                        }
                    }
                }
            }

            // ── Card 3: Patient & Hospital Details ─────────────────────────────
            BloodSyncCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hospital & Patient Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = appColors.textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                BloodSyncTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = "Patient Name (Optional)",
                    placeholder = "e.g. John Doe / Emergency Patient",
                    leadingIcon = Icons.Rounded.Person
                )

                Spacer(modifier = Modifier.height(12.dp))

                BloodSyncTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = "Attendant / Emergency Contact Phone *",
                    placeholder = "e.g. +91 9876543210",
                    leadingIcon = Icons.Rounded.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(12.dp))

                BloodSyncTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = "Hospital / Clinic Name",
                    placeholder = "e.g. City General Hospital",
                    leadingIcon = Icons.Rounded.LocalHospital
                )

                Spacer(modifier = Modifier.height(12.dp))

                BloodSyncTextField(
                    value = hospitalAddress,
                    onValueChange = { hospitalAddress = it },
                    label = "Address / Ward / Room (Optional)",
                    placeholder = "e.g. Ward 4, 3rd Floor, Trauma ICU",
                    leadingIcon = Icons.Rounded.LocationOn
                )
            }

            // ── Card 4: GPS 10km Radar Proximity Card ─────────────────────────
            val radarInfinite = rememberInfiniteTransition(label = "radarPulse")
            val radarScale by radarInfinite.animateFloat(
                initialValue = 1f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radarScale"
            )
            val radarAlpha by radarInfinite.animateFloat(
                initialValue = 0.45f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radarAlpha"
            )

            BloodSyncCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!LocationHelper.hasLocationPermission(context)) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            isLocating = true
                            LocationHelper.getCurrentLocation(
                                context = context,
                                onLocation = { lat, lon ->
                                    capturedLatitude = lat
                                    capturedLongitude = lon
                                    isLocating = false
                                    locationStatusMessage = "GPS Fixed: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
                                },
                                onError = { err ->
                                    isLocating = false
                                    locationStatusMessage = "Location unavailable ($err)"
                                }
                            )
                        }
                    },
                backgroundColor = if (capturedLatitude != null) appColors.greenLight else appColors.cardBackground,
                borderColor = if (capturedLatitude != null) StatusEligibleGreen.copy(alpha = 0.4f) else appColors.border
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLocating) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .graphicsLayer {
                                        scaleX = radarScale
                                        scaleY = radarScale
                                        alpha = radarAlpha
                                    }
                                    .background(BloodRedPrimary, CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (capturedLatitude != null) StatusEligibleGreen else BloodRedPrimary
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (capturedLatitude != null) Icons.Rounded.GpsFixed else Icons.Rounded.LocationSearching,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "24/7 Active Radar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (capturedLatitude != null) StatusEligibleGreen else appColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = locationStatusMessage ?: "Tap to detect GPS coordinates",
                            fontSize = 12.sp,
                            color = if (capturedLatitude != null) StatusEligibleGreen else appColors.textMuted
                        )
                    }

                    if (capturedLatitude == null && !isLocating) {
                        Surface(
                            color = appColors.redLight,
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.clickable {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Text(
                                text = "Detect",
                                color = BloodRedPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── Inline Validation Error Alert ─────────────────────────────────
            if (validationError != null) {
                Surface(
                    color = appColors.redLight,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = BloodRedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = validationError ?: "",
                            color = BloodRedPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Full-Width Glowing SOS Broadcast Button ───────────────────────
            BloodSyncButton(
                text = "BROADCAST EMERGENCY SOS",
                onClick = {
                    if (selectedGroup.isBlank()) {
                        validationError = "Please select the blood group needed."
                        return@BloodSyncButton
                    }
                    if (urgencyLevel == null) {
                        validationError = "Please select an urgency level."
                        return@BloodSyncButton
                    }

                    val phoneValidation = ValidationHelper.validatePhone(contactPhone)
                    if (!phoneValidation.isValid) {
                        validationError = phoneValidation.errorMessage
                        return@BloodSyncButton
                    }
                    val unitsValidation = ValidationHelper.validateUnits(unitsRequired)
                    if (!unitsValidation.isValid) {
                        validationError = unitsValidation.errorMessage
                        return@BloodSyncButton
                    }

                    validationError = null
                    isSubmitting = true

                    val cleanPatientName = if (patientName.isNotBlank()) patientName.trim() else "Emergency Patient"
                    val cleanHospitalName = if (hospitalName.isNotBlank()) hospitalName.trim() else "Medical Center"
                    val cleanAddress = ValidationHelper.sanitizeText(hospitalAddress, 150)
                    val cleanPhone = contactPhone.filter { it.isDigit() || it == '+' }.take(15)

                    val newRequest = repository.createEmergencyRequest(
                        patientName = cleanPatientName,
                        bloodGroup = selectedGroup,
                        units = unitsRequired,
                        hospitalName = cleanHospitalName,
                        hospitalAddress = cleanAddress,
                        contactPhone = cleanPhone,
                        urgencyLevel = urgencyLevel!!,
                        notes = "",
                        latitude = capturedLatitude,
                        longitude = capturedLongitude
                    )

                    isSubmitting = false
                    onRequestCreated(newRequest.id)
                },
                isLoading = isSubmitting,
                icon = Icons.Rounded.Send,
                height = 56.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
