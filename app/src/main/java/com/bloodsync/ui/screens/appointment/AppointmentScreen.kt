package com.bloodsync.ui.screens.appointment

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.data.model.Appointment
import com.bloodsync.data.model.AppointmentStatus
import com.bloodsync.data.model.BloodBank
import com.bloodsync.data.model.HealthRecord
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.*
import com.bloodsync.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class BookingDateOption(
    val dayLabel: String,         // e.g. "Today", "Tomorrow", "Fri", "Sat"
    val subLabel: String,         // e.g. "Sep 24", "Sep 25"
    val fullFormattedDate: String // e.g. "Sep 24, 2026"
)

fun getDynamicUpcomingDates(count: Int = 10, startOffsetDays: Int = 0): List<BookingDateOption> {
    val list = mutableListOf<BookingDateOption>()
    val cal = Calendar.getInstance().apply {
        if (startOffsetDays > 0) add(Calendar.DAY_OF_YEAR, startOffsetDays)
    }
    val dayFormat = SimpleDateFormat("EEE", Locale.US)
    val monthDayFormat = SimpleDateFormat("MMM dd", Locale.US)
    val fullFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    for (i in 0 until count) {
        val date = cal.time
        val dayLabel = when {
            startOffsetDays == 0 && i == 0 -> "Today"
            startOffsetDays == 0 && i == 1 -> "Tomorrow"
            else -> dayFormat.format(date)
        }
        val subLabel = monthDayFormat.format(date)
        val full = fullFormat.format(date)
        list.add(BookingDateOption(dayLabel, subLabel, full))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}

@Composable
fun AppointmentScreen(
    repository: BloodSyncRepository,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val appointments = repository.appointments
    val bloodBanks = repository.bloodBanks
    val healthRecord by repository.healthRecord
    val unreadNotifs by repository.unreadNotificationCount

    val userProfile by repository.userProfile
    val isBank = userProfile?.role == "blood_bank"

    // 3-Month Donation Gap Rule Calculations
    val eligibilityResult = remember(healthRecord) { healthRecord.calculateEligibility() }
    val nextEligibleDateMillis = remember(healthRecord) { healthRecord.getNextEligibleDateMillis() }
    val nextEligibleDateStr = remember(healthRecord) { healthRecord.getNextEligibleDateFormatted() }
    val isUnder3MonthCooldown = eligibilityResult.daysRemaining > 0

    var isBookingTab by remember { mutableStateOf(false) }

    // Dynamic upcoming dates starting from real current date
    val dynamicDates = remember { getDynamicUpcomingDates(10) }

    // Booking state
    var selectedBloodBank by remember { mutableStateOf<BloodBank?>(bloodBanks.firstOrNull()) }
    var selectedDate by remember(isUnder3MonthCooldown) {
        mutableStateOf(
            if (isUnder3MonthCooldown) nextEligibleDateStr
            else (dynamicDates.getOrNull(1)?.fullFormattedDate ?: dynamicDates.first().fullFormattedDate)
        )
    }
    var selectedTimeSlot by remember { mutableStateOf("10:30 AM") }
    var selectedDonationType by remember { mutableStateOf("Whole Blood (450ml)") }

    // Check if currently selected date satisfies 3-month gap rule
    val isDateAllowedBy3MonthRule = remember(selectedDate, healthRecord) {
        healthRecord.isAppointmentDateAllowed(selectedDate)
    }

    // Booking confirmation dialog state
    var confirmedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var rescheduleTargetAppointment by remember { mutableStateOf<Appointment?>(null) }

    val upcomingAppointments = appointments.filter { it.status == AppointmentStatus.UPCOMING }
    val pastAppointments = appointments.filter { it.status != AppointmentStatus.UPCOMING }

    Scaffold(
        topBar = {
            BloodSyncTopBar(
                title = "Appointments",
                subtitle = "Schedule & Manage Donations",
                showBackButton = true,
                onBackClick = onBackClick,
                unreadCount = unreadNotifs,
                onNotificationClick = onNotificationClick,
                actions = {
                    if (!isBank) {
                        IconButton(onClick = { isBookingTab = !isBookingTab }) {
                            Icon(
                                imageVector = if (isBookingTab) Icons.Rounded.CalendarMonth else Icons.Rounded.Add,
                                contentDescription = "Toggle View",
                                tint = BloodRedPrimary
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = appColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isBank) {
                // Segmented Pill Tab Switcher (50dp rounded)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = appColors.cardBackground,
                    border = BorderStroke(1.dp, appColors.border),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50.dp))
                                .then(
                                    if (!isBookingTab) {
                                        Modifier.background(
                                            Brush.horizontalGradient(
                                                listOf(BloodRedGradientStart, BloodRedGradientEnd)
                                            )
                                        )
                                    } else Modifier
                                )
                                .clickable { isBookingTab = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "My Slots (${upcomingAppointments.size})",
                                fontWeight = if (!isBookingTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isBookingTab) Color.White else appColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50.dp))
                                .then(
                                    if (isBookingTab) {
                                        Modifier.background(
                                            Brush.horizontalGradient(
                                                listOf(BloodRedGradientStart, BloodRedGradientEnd)
                                            )
                                        )
                                    } else Modifier
                                )
                                .clickable { isBookingTab = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ Book Slot",
                                fontWeight = if (isBookingTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (isBookingTab) Color.White else appColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (isBookingTab && !isBank) {
                // Booking Form
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 3-Month Donation Gap Cooldown Status Card
                    item {
                        BloodSyncCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (isUnder3MonthCooldown) StatusWarningAmberLight else appColors.greenLight,
                            borderColor = if (isUnder3MonthCooldown) StatusWarningAmber.copy(alpha = 0.4f) else StatusEligibleGreen.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (isUnder3MonthCooldown) StatusWarningAmber else StatusEligibleGreen,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isUnder3MonthCooldown) Icons.Rounded.HourglassEmpty else Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isUnder3MonthCooldown) "WHO 90-Day Gap Active" else "Eligible to Book Today",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isUnder3MonthCooldown) StatusWarningAmber else StatusEligibleGreen
                                    )
                                    Text(
                                        text = if (isUnder3MonthCooldown)
                                            "Earliest allowed slot is on or after $nextEligibleDateStr (${eligibilityResult.daysRemaining} days remaining)."
                                        else
                                            "You have satisfied the 3-month interval rule and can donate whole blood.",
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Step 1: Center / Blood Bank Picker
                    item {
                        SectionHeader(
                            title = "1. Select Blood Center",
                            subtitle = "Authorized donation clinics nearby"
                        )
                    }

                    items(bloodBanks, key = { it.id }) { bank ->
                        val isSelected = bank.id == selectedBloodBank?.id
                        BloodSyncCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBloodBank = bank },
                            backgroundColor = if (isSelected) appColors.redLight else appColors.cardBackground,
                            borderColor = if (isSelected) BloodRedPrimary else appColors.border,
                            elevation = if (isSelected) 3.dp else 1.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(
                                                if (isSelected) BloodRedPrimary else appColors.surfaceVariant,
                                                RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocalHospital,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else BloodRedPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = bank.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = appColors.textPrimary
                                        )
                                        Text(
                                            text = "${bank.address} • ${bank.distanceKm} km",
                                            fontSize = 12.sp,
                                            color = appColors.textSecondary
                                        )
                                        Text(
                                            text = "${bank.openHours} • ${bank.bloodStockStatus}",
                                            fontSize = 11.sp,
                                            color = StatusEligibleGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedBloodBank = bank },
                                    colors = RadioButtonDefaults.colors(selectedColor = BloodRedPrimary)
                                )
                            }
                        }
                    }

                    // Step 2: 10-Day Date Strip
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                title = "2. Select Date",
                                subtitle = "Pick from upcoming 10 days"
                            )

                            TextButton(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    val minAllowedMillis = if (isUnder3MonthCooldown)
                                        maxOf(System.currentTimeMillis(), nextEligibleDateMillis)
                                    else
                                        System.currentTimeMillis()

                                    calendar.timeInMillis = minAllowedMillis

                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val cal = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth)
                                            }
                                            selectedDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        datePicker.minDate = minAllowedMillis
                                    }.show()
                                }
                            ) {
                                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = BloodRedPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Calendar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BloodRedPrimary)
                            }
                        }

                        // Selected Date Indicator
                        StatusPillBadge(
                            text = "Chosen Date: $selectedDate",
                            backgroundColor = if (isDateAllowedBy3MonthRule) appColors.redLight else StatusWarningAmberLight,
                            contentColor = if (isDateAllowedBy3MonthRule) BloodRedPrimary else StatusWarningAmber,
                            showBeacon = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal Date Pills Strip
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(dynamicDates) { dateOption ->
                                val isSelected = selectedDate == dateOption.fullFormattedDate
                                val isOptionAllowed = healthRecord.isAppointmentDateAllowed(dateOption.fullFormattedDate)

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(enabled = isOptionAllowed) {
                                            selectedDate = dateOption.fullFormattedDate
                                        }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = when {
                                                isSelected -> BloodRedPrimary
                                                !isOptionAllowed -> appColors.border
                                                else -> appColors.border
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    color = when {
                                        isSelected -> appColors.redLight
                                        !isOptionAllowed -> appColors.surfaceVariant.copy(alpha = 0.5f)
                                        else -> appColors.cardBackground
                                    },
                                    shadowElevation = if (isSelected) 2.dp else 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = dateOption.dayLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> BloodRedPrimary
                                                !isOptionAllowed -> appColors.textMuted
                                                else -> appColors.textSecondary
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dateOption.subLabel,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isSelected -> BloodRedPrimary
                                                !isOptionAllowed -> appColors.textMuted
                                                else -> appColors.textPrimary
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Step 3: Time Slot Picker (Pill Chips)
                    item {
                        SectionHeader(
                            title = "3. Preferred Time Slot",
                            subtitle = "Available clinical appointment slots"
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val slots = listOf("10:30 AM", "12:00 PM", "02:30 PM", "04:00 PM")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            slots.forEach { slot ->
                                val isSelected = selectedTimeSlot == slot
                                BloodSyncChip(
                                    text = slot,
                                    selected = isSelected,
                                    onClick = { selectedTimeSlot = slot },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Step 4: Donation Type (Pill Chips)
                    item {
                        SectionHeader(
                            title = "4. Donation Type",
                            subtitle = "Select procedure"
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val types = listOf(
                            "Whole Blood 450ml",
                            "Platelets",
                            "Plasma"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            types.forEach { type ->
                                val isSelected = selectedDonationType.startsWith(type.take(5))
                                BloodSyncChip(
                                    text = type,
                                    selected = isSelected,
                                    onClick = { selectedDonationType = type },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Confirm Booking Pill Button
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        BloodSyncButton(
                            text = if (isDateAllowedBy3MonthRule) "Confirm Appointment Slot" else "Date Cooldown Required",
                            onClick = {
                                if (!isDateAllowedBy3MonthRule) {
                                    Toast.makeText(
                                        context,
                                        "WHO 3-Month Rule: Earliest eligible date is $nextEligibleDateStr",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@BloodSyncButton
                                }
                                selectedBloodBank?.let { bank ->
                                    val apt = repository.bookAppointment(
                                        bloodBank = bank,
                                        date = selectedDate,
                                        timeSlot = selectedTimeSlot,
                                        donationType = selectedDonationType
                                    )
                                    confirmedAppointment = apt
                                }
                            },
                            enabled = selectedBloodBank != null && isDateAllowedBy3MonthRule,
                            icon = Icons.Rounded.CheckCircle,
                            height = 54.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // My Appointments List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Upcoming Appointments",
                            subtitle = "Your scheduled blood donation visits"
                        )
                    }

                    if (upcomingAppointments.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No Upcoming Appointments",
                                message = if (isBank) "There are no upcoming appointments scheduled at your blood bank facility." else "You have no upcoming donation bookings. Book a slot today to save lives.",
                                icon = Icons.Rounded.EventAvailable,
                                actionText = if (isBank) null else "Book Donation Slot",
                                onActionClick = { if (!isBank) isBookingTab = true }
                            )
                        }
                    } else {
                        items(upcomingAppointments, key = { it.id }) { apt ->
                            AppointmentItemCard(
                                appointment = apt,
                                isUpcoming = true,
                                onRescheduleClick = { rescheduleTargetAppointment = apt },
                                onCancelClick = { repository.cancelAppointment(apt.id) }
                            )
                        }
                    }

                    if (pastAppointments.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(
                                title = "Past & Completed Visits",
                                subtitle = "Previous donation history"
                            )
                        }

                        items(pastAppointments, key = { it.id }) { apt ->
                            AppointmentItemCard(
                                appointment = apt,
                                isUpcoming = false,
                                onRescheduleClick = {},
                                onCancelClick = {}
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Success Dialog
    confirmedAppointment?.let { apt ->
        AlertDialog(
            onDismissRequest = {
                confirmedAppointment = null
                isBookingTab = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(appColors.greenLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = StatusEligibleGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Booking Confirmed!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = appColors.textPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your donation slot has been reserved. A notification reminder has been scheduled.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BloodSyncCard(
                        backgroundColor = appColors.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        elevation = 0.dp
                    ) {
                        Text(
                            text = "Reference: ${apt.referenceCode}",
                            fontWeight = FontWeight.Bold,
                            color = BloodRedPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Facility: ${apt.bloodBankName}", fontSize = 12.sp, color = appColors.textPrimary)
                        Text(text = "Time: ${apt.date} at ${apt.timeSlot}", fontSize = 12.sp, color = appColors.textPrimary)
                        Text(text = "Type: ${apt.donationType}", fontSize = 12.sp, color = appColors.textPrimary)
                    }
                }
            },
            confirmButton = {
                BloodSyncButton(
                    text = "View Appointments",
                    onClick = {
                        confirmedAppointment = null
                        isBookingTab = false
                    },
                    height = 46.dp,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = appColors.cardBackground
        )
    }

    // Interactive Reschedule Dialog
    rescheduleTargetAppointment?.let { apt ->
        RescheduleDialog(
            appointment = apt,
            dynamicDates = dynamicDates,
            healthRecord = healthRecord,
            onDismiss = { rescheduleTargetAppointment = null },
            onConfirmReschedule = { newDate, newSlot ->
                repository.rescheduleAppointment(apt.id, newDate, newSlot)
                rescheduleTargetAppointment = null
                Toast.makeText(context, "Rescheduled to $newDate at $newSlot", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AppointmentItemCard(
    appointment: Appointment,
    isUpcoming: Boolean,
    onRescheduleClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val appColors = BloodSyncTheme.colors

    BloodSyncCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isUpcoming) appColors.redLight else appColors.surfaceVariant,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = if (isUpcoming) BloodRedPrimary else appColors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = appointment.bloodBankName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = appColors.textPrimary
                    )
                    Text(
                        text = "Ref: ${appointment.referenceCode}",
                        fontSize = 11.sp,
                        color = appColors.textMuted
                    )
                }
            }

            StatusPillBadge(
                text = appointment.status.name,
                backgroundColor = when (appointment.status) {
                    AppointmentStatus.UPCOMING -> StatusWarningAmberLight
                    AppointmentStatus.COMPLETED -> appColors.greenLight
                    AppointmentStatus.CANCELLED -> appColors.redLight
                },
                contentColor = when (appointment.status) {
                    AppointmentStatus.UPCOMING -> StatusWarningAmber
                    AppointmentStatus.COMPLETED -> StatusEligibleGreen
                    AppointmentStatus.CANCELLED -> StatusUrgentRed
                },
                showBeacon = isUpcoming
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = appColors.border, thickness = 0.75.dp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Date & Time", fontSize = 11.sp, color = appColors.textSecondary)
                Text(
                    "${appointment.date} • ${appointment.timeSlot}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Donation Type", fontSize = 11.sp, color = appColors.textSecondary)
                Text(
                    appointment.donationType,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appColors.textPrimary
                )
            }
        }

        if (isUpcoming) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancelClick) {
                    Text("Cancel", color = StatusUrgentRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                BloodSyncOutlinedButton(
                    text = "Reschedule",
                    onClick = onRescheduleClick,
                    icon = Icons.Rounded.Schedule,
                    height = 42.dp,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }
        }
    }
}

@Composable
fun RescheduleDialog(
    appointment: Appointment,
    dynamicDates: List<BookingDateOption>,
    healthRecord: HealthRecord,
    onDismiss: () -> Unit,
    onConfirmReschedule: (newDate: String, newSlot: String) -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val nextEligibleDateMillis = remember(healthRecord) { healthRecord.getNextEligibleDateMillis() }
    val nextEligibleDateStr = remember(healthRecord) { healthRecord.getNextEligibleDateFormatted() }

    var selectedNewDate by remember {
        mutableStateOf(
            if (!healthRecord.isAppointmentDateAllowed(dynamicDates.first().fullFormattedDate))
                nextEligibleDateStr
            else
                (dynamicDates.getOrNull(2)?.fullFormattedDate ?: dynamicDates.first().fullFormattedDate)
        )
    }
    var selectedNewSlot by remember { mutableStateOf(appointment.timeSlot) }

    val isNewDateValid = remember(selectedNewDate, healthRecord) {
        healthRecord.isAppointmentDateAllowed(selectedNewDate)
    }

    val slots = listOf("10:30 AM", "12:00 PM", "02:30 PM", "04:00 PM")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Reschedule Appointment", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = appColors.textPrimary)
                Text(
                    text = appointment.bloodBankName,
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusPillBadge(
                    text = "New Date: $selectedNewDate",
                    backgroundColor = if (isNewDateValid) appColors.redLight else StatusWarningAmberLight,
                    contentColor = if (isNewDateValid) BloodRedPrimary else StatusWarningAmber
                )

                // Quick Date Pills
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(dynamicDates) { option ->
                        val isSelected = selectedNewDate == option.fullFormattedDate
                        val isAllowed = healthRecord.isAppointmentDateAllowed(option.fullFormattedDate)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = isAllowed) { selectedNewDate = option.fullFormattedDate }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) BloodRedPrimary else appColors.border,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            color = if (isSelected) appColors.redLight else appColors.cardBackground
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = option.dayLabel,
                                    fontSize = 10.sp,
                                    color = if (isSelected) BloodRedPrimary else appColors.textSecondary
                                )
                                Text(
                                    text = option.subLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BloodRedPrimary else appColors.textPrimary
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Choose New Slot:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slots.forEach { slot ->
                        BloodSyncChip(
                            text = slot,
                            selected = selectedNewSlot == slot,
                            onClick = { selectedNewSlot = slot },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            BloodSyncButton(
                text = "Confirm",
                onClick = { onConfirmReschedule(selectedNewDate, selectedNewSlot) },
                enabled = isNewDateValid,
                height = 46.dp,
                modifier = Modifier.widthIn(max = 140.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = appColors.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = appColors.cardBackground
    )
}
