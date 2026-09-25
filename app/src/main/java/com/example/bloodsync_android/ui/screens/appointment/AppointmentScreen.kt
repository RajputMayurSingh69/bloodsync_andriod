package com.example.bloodsync_android.ui.screens.appointment

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.model.Appointment
import com.example.bloodsync_android.data.model.AppointmentStatus
import com.example.bloodsync_android.data.model.BloodBank
import com.example.bloodsync_android.data.model.HealthRecord
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodSyncTopBar
import com.example.bloodsync_android.ui.components.EmptyStateView
import com.example.bloodsync_android.ui.components.StatusBadge
import com.example.bloodsync_android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
    val appointments = repository.appointments
    val bloodBanks = repository.bloodBanks
    val healthRecord by repository.healthRecord
    val unreadNotifs by repository.unreadNotificationCount

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

    // Direct filter on SnapshotStateList (no stale remember caching)
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
                    IconButton(onClick = { isBookingTab = !isBookingTab }) {
                        Icon(
                            imageVector = if (isBookingTab) Icons.Default.DateRange else Icons.Default.Add,
                            contentDescription = "Toggle View",
                            tint = BloodRedPrimary
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = BloodSyncTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Switcher (My Appointments vs Book Slot) - Pill Shaped
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(50.dp),
                color = MedicalSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (!isBookingTab) MedicalWhite else Color.Transparent)
                            .clickable { isBookingTab = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "My Appointments (${upcomingAppointments.size})",
                            fontWeight = if (!isBookingTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isBookingTab) BloodRedPrimary else MedicalTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isBookingTab) MedicalWhite else Color.Transparent)
                            .clickable { isBookingTab = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Book New Slot",
                            fontWeight = if (isBookingTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (isBookingTab) BloodRedPrimary else MedicalTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (isBookingTab) {
                // Booking Form
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 3-Month Donation Gap Rule Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnder3MonthCooldown) StatusWarningAmberLight else StatusEligibleGreenLight
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isUnder3MonthCooldown) StatusWarningAmber else StatusEligibleGreen
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isUnder3MonthCooldown) StatusWarningAmber else StatusEligibleGreen,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isUnder3MonthCooldown) Icons.Default.HourglassEmpty else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MedicalWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3-Month Blood Donation Rule",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MedicalTextPrimary
                                    )
                                    Text(
                                        text = if (isUnder3MonthCooldown)
                                            "Medical Rule: A donor can only donate whole blood once every 3 months (90 days). Your next eligible date is $nextEligibleDateStr (${eligibilityResult.daysRemaining} days left)."
                                        else
                                            "You have satisfied the 3-month interval rule and are fully eligible to donate whole blood.",
                                        fontSize = 12.sp,
                                        color = MedicalTextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Step 1: Blood Bank / Hospital Selection
                    item {
                        Text(
                            text = "1. Choose Blood Bank / Hospital",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MedicalTextPrimary
                        )
                    }

                    items(bloodBanks, key = { it.id }) { bank ->
                        val isSelected = bank.id == selectedBloodBank?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { selectedBloodBank = bank },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BloodRedLight else MedicalWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) BloodRedPrimary else MedicalBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (isSelected) BloodRedPrimary else MedicalSurfaceVariant,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalHospital,
                                            contentDescription = null,
                                            tint = if (isSelected) MedicalWhite else BloodRedPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = bank.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MedicalTextPrimary
                                        )
                                        Text(
                                            text = "${bank.address} • ${bank.distanceKm} km away",
                                            fontSize = 12.sp,
                                            color = MedicalTextSecondary
                                        )
                                        Text(
                                            text = "Hours: ${bank.openHours} • ${bank.bloodStockStatus}",
                                            fontSize = 11.sp,
                                            color = StatusEligibleGreen
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

                    // Step 2: Date Picker (Dynamically computed + 3-month rule validation)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Select Appointment Date",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MedicalTextPrimary
                            )

                            // Pick custom date from calendar button (with 3-month minDate)
                            TextButton(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    // If user is under 3-month cooldown, minimum allowed date is nextEligibleDateMillis
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
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Calendar Picker", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BloodRedPrimary)
                            }
                        }

                        // Display active selected date - Pill Shaped
                        Surface(
                            color = if (isDateAllowedBy3MonthRule) BloodRedLight else StatusUrgentRedLight,
                            shape = RoundedCornerShape(50.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDateAllowedBy3MonthRule) BloodRedPrimary else StatusUrgentRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isDateAllowedBy3MonthRule) Icons.Default.Event else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDateAllowedBy3MonthRule) BloodRedPrimary else StatusUrgentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chosen Date: ",
                                    fontSize = 12.sp,
                                    color = MedicalTextSecondary
                                )
                                Text(
                                    text = selectedDate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDateAllowedBy3MonthRule) BloodRedPrimary else StatusUrgentRed
                                )
                            }
                        }

                        // If selected date violates 3-month rule, show warning banner + quick fix
                        if (!isDateAllowedBy3MonthRule) {
                            Surface(
                                color = StatusUrgentRedLight,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "⚠️ 3-Month Rule Violation:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusUrgentRed
                                    )
                                    Text(
                                        text = "You cannot donate before your 3-month recovery period. Please pick $nextEligibleDateStr or later.",
                                        fontSize = 11.sp,
                                        color = StatusUrgentRed
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { selectedDate = nextEligibleDateStr },
                                        colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                                        shape = RoundedCornerShape(50.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Select Earliest Date: $nextEligibleDateStr", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Horizontal upcoming date cards - Pill style
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(dynamicDates) { dateOption ->
                                val isSelected = selectedDate == dateOption.fullFormattedDate
                                val isOptionAllowed = healthRecord.isAppointmentDateAllowed(dateOption.fullFormattedDate)

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedDate = dateOption.fullFormattedDate }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) BloodRedPrimary else (if (!isOptionAllowed) StatusUrgentRedLight else MedicalBorder),
                                            shape = RoundedCornerShape(20.dp)
                                        ),
                                    color = if (isSelected) BloodRedLight else (if (!isOptionAllowed) StatusUrgentRedLight else MedicalWhite)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = dateOption.dayLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) BloodRedPrimary else (if (!isOptionAllowed) StatusUrgentRed else MedicalTextSecondary)
                                        )
                                        Text(
                                            text = dateOption.subLabel,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BloodRedPrimary else (if (!isOptionAllowed) StatusUrgentRed else MedicalTextPrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Step 3: Time Slot Picker
                    item {
                        Text(
                            text = "3. Select Preferred Time Slot",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MedicalTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val morningSlots = listOf("09:00 AM", "10:30 AM", "11:45 AM")
                        val afternoonSlots = listOf("01:30 PM", "03:00 PM", "04:30 PM")

                        Text("Morning Slots", fontSize = 12.sp, color = MedicalTextMuted)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            morningSlots.forEach { slot ->
                                TimeSlotChip(
                                    slot = slot,
                                    isSelected = selectedTimeSlot == slot,
                                    onSelect = { selectedTimeSlot = slot },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Afternoon Slots", fontSize = 12.sp, color = MedicalTextMuted)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            afternoonSlots.forEach { slot ->
                                TimeSlotChip(
                                    slot = slot,
                                    isSelected = selectedTimeSlot == slot,
                                    onSelect = { selectedTimeSlot = slot },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Step 4: Donation Type
                    item {
                        Text(
                            text = "4. Donation Type",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MedicalTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val types = listOf(
                            "Whole Blood (450ml)",
                            "Platelets (Apheresis)",
                            "Plasma Donation"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            types.forEach { type ->
                                val isSelected = selectedDonationType == type
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .clickable { selectedDonationType = type }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) BloodRedPrimary else MedicalBorder,
                                            shape = RoundedCornerShape(50.dp)
                                        ),
                                    color = if (isSelected) BloodRedLight else MedicalWhite
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BloodRedPrimary else MedicalTextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Submit Booking Action (Enforced 3-Month Interval) - Pill Button
                    item {
                        Button(
                            onClick = {
                                if (!isDateAllowedBy3MonthRule) {
                                    Toast.makeText(
                                        context,
                                        "3-Month Rule: Next eligible donation is on or after $nextEligibleDateStr",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Button
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDateAllowedBy3MonthRule) BloodRedPrimary else MedicalTextMuted
                            ),
                            shape = RoundedCornerShape(50.dp),
                            enabled = selectedBloodBank != null && isDateAllowedBy3MonthRule
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDateAllowedBy3MonthRule) "Confirm Donation Booking" else "Ineligible (3-Month Gap Required)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                // My Appointments List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Upcoming Scheduled Appointments",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MedicalTextPrimary
                        )
                    }

                    if (upcomingAppointments.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No Upcoming Appointments",
                                message = "You have no upcoming donation bookings.",
                                icon = Icons.Default.EventAvailable,
                                actionText = "Book Donation Slot",
                                onActionClick = { isBookingTab = true }
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Past & Cancelled Appointments",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MedicalTextPrimary
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
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusEligibleGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Booking Confirmed!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your donation slot has been reserved. A push notification reminder has been scheduled.",
                        fontSize = 13.sp,
                        color = MedicalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MedicalSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Booking Ref: ${apt.referenceCode}",
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary
                            )
                            Text(text = "Facility: ${apt.bloodBankName}", fontSize = 12.sp)
                            Text(text = "Date & Time: ${apt.date} at ${apt.timeSlot}", fontSize = 12.sp)
                            Text(text = "Type: ${apt.donationType}", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmedAppointment = null
                        isBookingTab = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text("View My Appointments")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = MedicalWhite
        )
    }

    // Interactive Reschedule Dialog with 3-Month Minimum Date Picker
    rescheduleTargetAppointment?.let { apt ->
        RescheduleDialog(
            appointment = apt,
            dynamicDates = dynamicDates,
            healthRecord = healthRecord,
            onDismiss = { rescheduleTargetAppointment = null },
            onConfirmReschedule = { newDate, newSlot ->
                repository.rescheduleAppointment(apt.id, newDate, newSlot)
                rescheduleTargetAppointment = null
                Toast.makeText(context, "Appointment rescheduled to $newDate at $newSlot", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun TimeSlotChip(
    slot: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BloodRedPrimary else MedicalBorder,
                shape = RoundedCornerShape(50.dp)
            ),
        color = if (isSelected) BloodRedLight else MedicalWhite
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = slot,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) BloodRedPrimary else MedicalTextPrimary
            )
        }
    }
}

@Composable
fun AppointmentItemCard(
    appointment: Appointment,
    isUpcoming: Boolean,
    onRescheduleClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(38.dp)
                            .background(
                                if (isUpcoming) BloodRedLight else MedicalSurfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (isUpcoming) BloodRedPrimary else MedicalTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = appointment.bloodBankName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MedicalTextPrimary
                        )
                        Text(
                            text = "Ref: ${appointment.referenceCode}",
                            fontSize = 11.sp,
                            color = MedicalTextMuted
                        )
                    }
                }

                StatusBadge(
                    text = appointment.status.name,
                    textColor = when (appointment.status) {
                        AppointmentStatus.UPCOMING -> StatusWarningAmber
                        AppointmentStatus.COMPLETED -> StatusEligibleGreen
                        AppointmentStatus.CANCELLED -> StatusUrgentRed
                    },
                    backgroundColor = when (appointment.status) {
                        AppointmentStatus.UPCOMING -> StatusWarningAmberLight
                        AppointmentStatus.COMPLETED -> StatusEligibleGreenLight
                        AppointmentStatus.CANCELLED -> StatusUrgentRedLight
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MedicalDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Date & Time", fontSize = 11.sp, color = MedicalTextSecondary)
                    Text(
                        "${appointment.date} • ${appointment.timeSlot}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedicalTextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Donation Type", fontSize = 11.sp, color = MedicalTextSecondary)
                    Text(
                        appointment.donationType,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MedicalTextPrimary
                    )
                }
            }

            if (isUpcoming) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancelClick) {
                        Text("Cancel", color = StatusUrgentRed, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRescheduleClick,
                        shape = RoundedCornerShape(50.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = BloodRedPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reschedule", color = BloodRedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
    val nextEligibleDateMillis = remember(healthRecord) { healthRecord.getNextEligibleDateMillis() }
    val nextEligibleDateStr = remember(healthRecord) { healthRecord.getNextEligibleDateFormatted() }

    // Initialize with valid future date
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

    val morningSlots = listOf("09:00 AM", "10:30 AM", "11:45 AM")
    val afternoonSlots = listOf("01:30 PM", "03:00 PM", "04:30 PM")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Reschedule Appointment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    text = appointment.bloodBankName,
                    fontSize = 12.sp,
                    color = MedicalTextSecondary
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
                // Current appointment badge
                Surface(
                    color = MedicalSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Currently Booked For:",
                            fontSize = 10.sp,
                            color = MedicalTextSecondary
                        )
                        Text(
                            text = "${appointment.date} at ${appointment.timeSlot}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MedicalTextPrimary
                        )
                    }
                }

                // New Date Selection Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose New Date:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedicalTextPrimary
                    )

                    TextButton(
                        onClick = {
                            val minAllowedMillis = maxOf(System.currentTimeMillis(), nextEligibleDateMillis)
                            val calendar = Calendar.getInstance().apply { timeInMillis = minAllowedMillis }

                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }
                                    selectedNewDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                datePicker.minDate = minAllowedMillis
                            }.show()
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Calendar", fontSize = 11.sp, color = BloodRedPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Selected New Date indicator - Pill
                Surface(
                    color = if (isNewDateValid) BloodRedLight else StatusUrgentRedLight,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isNewDateValid) Icons.Default.Event else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isNewDateValid) BloodRedPrimary else StatusUrgentRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "New Date: $selectedNewDate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNewDateValid) BloodRedPrimary else StatusUrgentRed
                        )
                    }
                }

                if (!isNewDateValid) {
                    Text(
                        text = "⚠️ Must wait 3 months between donations (Eligible on or after $nextEligibleDateStr)",
                        fontSize = 11.sp,
                        color = StatusUrgentRed
                    )
                }

                // Quick date pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(dynamicDates) { option ->
                        val isSelected = selectedNewDate == option.fullFormattedDate
                        val isAllowed = healthRecord.isAppointmentDateAllowed(option.fullFormattedDate)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedNewDate = option.fullFormattedDate }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) BloodRedPrimary else (if (!isAllowed) StatusUrgentRedLight else MedicalBorder),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            color = if (isSelected) BloodRedLight else (if (!isAllowed) StatusUrgentRedLight else MedicalWhite)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = option.dayLabel,
                                    fontSize = 10.sp,
                                    color = if (isSelected) BloodRedPrimary else (if (!isAllowed) StatusUrgentRed else MedicalTextSecondary)
                                )
                                Text(
                                    text = option.subLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BloodRedPrimary else (if (!isAllowed) StatusUrgentRed else MedicalTextPrimary)
                                )
                            }
                        }
                    }
                }

                // New Time Slot Selection
                Text(
                    text = "Choose New Time Slot:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalTextPrimary
                )

                Text("Morning", fontSize = 11.sp, color = MedicalTextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    morningSlots.forEach { slot ->
                        TimeSlotChip(
                            slot = slot,
                            isSelected = selectedNewSlot == slot,
                            onSelect = { selectedNewSlot = slot },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Afternoon", fontSize = 11.sp, color = MedicalTextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    afternoonSlots.forEach { slot ->
                        TimeSlotChip(
                            slot = slot,
                            isSelected = selectedNewSlot == slot,
                            onSelect = { selectedNewSlot = slot },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReschedule(selectedNewDate, selectedNewSlot) },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                shape = RoundedCornerShape(50.dp),
                enabled = isNewDateValid
            ) {
                Text(if (isNewDateValid) "Confirm Reschedule" else "Date Ineligible")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MedicalTextSecondary)
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = MedicalWhite
    )
}
