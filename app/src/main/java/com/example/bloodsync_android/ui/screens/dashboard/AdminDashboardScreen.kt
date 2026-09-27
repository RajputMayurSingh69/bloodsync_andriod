package com.example.bloodsync_android.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════
// LOCAL DATA MODELS
// ═══════════════════════════════════════════════════════════════════════

private data class BloodInventoryItem(
    val group: String, val units: Int, val maxUnits: Int
) {
    val isLow: Boolean get() = (units.toFloat() / maxUnits) <= 0.35f
    val fraction: Float get() = (units.toFloat() / maxUnits).coerceIn(0f, 1f)
}

private data class DonorEntry(
    val id: String, val name: String, val bloodGroup: String,
    val city: String, val phone: String, val date: String, val status: DonorStatus
)

private enum class DonorStatus(val label: String, val bgColor: Color, val textColor: Color) {
    AVAILABLE("● Available", Color(0xFFDCFCE7), Color(0xFF15803D)),
    PENDING  ("◌ Pending",   Color(0xFFFEF9C3), Color(0xFFA16207)),
    DISPATCHED("▶ Dispatched",Color(0xFFFFEBEE), Color(0xFFD32F2F)),
    COOLDOWN ("⏸ Cooldown",  Color(0xFFF3F4F6), Color(0xFF6B7280))
}

private data class FeedEvent(val icon: String, val text: String, val timeAgo: String, val accent: Color)

// ═══════════════════════════════════════════════════════════════════════
// STATIC DEMO DATA
// ═══════════════════════════════════════════════════════════════════════

private val INVENTORY_DATA = listOf(
    BloodInventoryItem("A+",  287, 400), BloodInventoryItem("A-",  48,  100),
    BloodInventoryItem("B+",  193, 300), BloodInventoryItem("B-",  29,  80),
    BloodInventoryItem("AB+", 112, 150), BloodInventoryItem("AB-", 8,   60),
    BloodInventoryItem("O+",  334, 450), BloodInventoryItem("O-",  53,  120),
)

private val DONOR_DATA = listOf(
    DonorEntry("D-1042","Aisha Patel",  "A+", "Mumbai",    "+91 98765 43210","27 Sep",DonorStatus.AVAILABLE),
    DonorEntry("D-1041","Rajesh Kumar", "O-", "Delhi",     "+91 87654 32109","26 Sep",DonorStatus.AVAILABLE),
    DonorEntry("D-1040","Meera Nair",   "B+", "Kochi",     "+91 76543 21098","26 Sep",DonorStatus.COOLDOWN),
    DonorEntry("D-1039","Arjun Singh",  "AB+","Bangalore", "+91 65432 10987","25 Sep",DonorStatus.AVAILABLE),
    DonorEntry("D-1038","Priya Sharma", "A-", "Pune",      "+91 54321 09876","24 Sep",DonorStatus.PENDING),
    DonorEntry("D-1037","Vikram Rao",   "O+", "Chennai",   "+91 43210 98765","23 Sep",DonorStatus.DISPATCHED),
    DonorEntry("D-1036","Sneha Gupta",  "B-", "Hyderabad", "+91 32109 87654","22 Sep",DonorStatus.COOLDOWN),
    DonorEntry("D-1035","Karan Mehta",  "AB-","Ahmedabad", "+91 21098 76543","21 Sep",DonorStatus.AVAILABLE),
)

private val FEED_DATA = listOf(
    FeedEvent("🩸","Rahul S. (B+) just registered as a donor",  "Just now",  Color(0xFF16A34A)),
    FeedEvent("🚨","SOS: City Hospital needs 3 units O−",        "2 min ago", Color(0xFFD32F2F)),
    FeedEvent("✅","Aisha P. verified — ready to donate",        "5 min ago", Color(0xFF16A34A)),
    FeedEvent("📦","Stock updated: +50 units A+ from camp",      "12 min ago",Color(0xFF2563EB)),
    FeedEvent("⚠️","AB− stock below threshold (8 units)",        "18 min ago",Color(0xFFD97706)),
    FeedEvent("🏥","MedCity Hospital joined as partner",         "34 min ago",Color(0xFF2563EB)),
)

private fun bgGroupColors(group: String): Pair<Color, Color> = when (group) {
    "A+"  -> Pair(Color(0xFFFFE0E0), Color(0xFFB91C1C))
    "A-"  -> Pair(Color(0xFFFFCDD2), Color(0xFF991B1B))
    "B+"  -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9))
    "B-"  -> Pair(Color(0xFFDDD6FE), Color(0xFF5B21B6))
    "AB+" -> Pair(Color(0xFFFFF7ED), Color(0xFFC2410C))
    "AB-" -> Pair(Color(0xFFFED7AA), Color(0xFF9A3412))
    "O+"  -> Pair(Color(0xFFECFDF5), Color(0xFF065F46))
    "O-"  -> Pair(Color(0xFFA7F3D0), Color(0xFF064E3B))
    else  -> Pair(Color(0xFFF3F4F6), Color(0xFF374151))
}

private fun currentTimeString(): String =
    SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.ENGLISH).format(Date())

// ═══════════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun AdminDashboardScreen(
    repository: BloodSyncRepository,
    onNavigateToEmergency: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val appColors      = BloodSyncTheme.colors
    val donors         = repository.donors
    val emergencies    = repository.emergencyRequests.filter { it.status.name != "CANCELLED" }
    val unreadNotifs   by repository.unreadNotificationCount

    val totalDonors    = donors.size.coerceAtLeast(2847)
    val urgentCount    = emergencies.size.coerceAtLeast(7)
    val totalUnits     = INVENTORY_DATA.sumOf { it.units }

    var timeString by remember { mutableStateOf(currentTimeString()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            timeString = currentTimeString()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(appColors.background)
    ) {
        DashboardTopBar(
            timeString       = timeString,
            unreadCount      = unreadNotifs,
            onNotifClick     = onNavigateToNotifications,
            onEmergencyClick = onNavigateToEmergency,
            appColors        = appColors
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // KPI Cards row 1
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard("Total Donors", "$totalDonors", "+24 this week", Color(0xFF16A34A),
                        Icons.Default.People, Color(0xFFFFEBEE), BloodRedPrimary, Modifier.weight(1f))
                    KpiCard("Units in Stock", "$totalUnits", "⚠ AB− critical", Color(0xFFD97706),
                        Icons.Default.Favorite, Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                }
            }

            // KPI Cards row 2
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard("Urgent SOS", "$urgentCount", "3 need dispatch", BloodRedPrimary,
                        Icons.Default.Warning, Color(0xFFFFEBEE), BloodRedPrimary, Modifier.weight(1f), isUrgent = true)
                    KpiCard("Pending Reviews", "38", "Last 2h ago", Color(0xFF757575),
                        Icons.Default.AssignmentTurnedIn, Color(0xFFFFFDE7), Color(0xFFF57F17), Modifier.weight(1f))
                }
            }

            // Blood Inventory
            item { InventoryMatrixCard(appColors) }

            // Live Feed
            item { LiveFeedCard(appColors) }

            // Active SOS banner if any
            if (emergencies.isNotEmpty()) {
                item {
                    ActiveSosBanner(
                        hospitalName = emergencies.first().hospitalName,
                        bloodGroup   = emergencies.first().bloodGroupNeeded,
                        units        = emergencies.first().unitsRequired,
                        onDispatch   = onNavigateToEmergency,
                        appColors    = appColors
                    )
                }
            }

            // Donor Activity
            item { DonorActivityCard(appColors) }

            // Summary
            item { TodaySummaryCard(appColors) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DashboardTopBar(
    timeString: String, unreadCount: Int,
    onNotifClick: () -> Unit, onEmergencyClick: () -> Unit,
    appColors: BloodSyncColors
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = appColors.topBarBackground,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(BloodRedPrimary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Dashboard, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Admin Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    Text(timeString, fontSize = 10.sp, color = appColors.textMuted)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box {
                    IconButton(onClick = onNotifClick) {
                        Icon(Icons.Default.Notifications, "Notifications", tint = appColors.textSecondary)
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(8.dp)
                                .background(BloodRedPrimary, CircleShape)
                                .border(1.5.dp, appColors.surface, CircleShape)
                        )
                    }
                }
                Button(
                    onClick = onEmergencyClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SOS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// KPI CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun KpiCard(
    label: String, value: String, trend: String, trendColor: Color,
    icon: ImageVector, iconBg: Color, iconTint: Color,
    modifier: Modifier = Modifier, isUrgent: Boolean = false
) {
    val appColors = BloodSyncTheme.colors
    val inf = rememberInfiniteTransition(label = "kpi")
    val pulseAlpha by inf.animateFloat(
        1f, 0.35f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), "kpiPulse"
    )

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if (isUrgent) BloodRedPrimary.copy(alpha = 0.3f) else appColors.border
        ),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).background(iconBg, RoundedCornerShape(12.dp)),
                    Alignment.Center
                ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
                if (isUrgent) Box(Modifier.size(8.dp).background(BloodRedPrimary.copy(alpha = pulseAlpha), CircleShape))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black,
                color = if (isUrgent) BloodRedPrimary else appColors.textPrimary)
            Text(label, fontSize = 12.sp, color = appColors.textMuted)
            Spacer(Modifier.height(4.dp))
            Text(trend, fontSize = 10.sp, color = trendColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// INVENTORY MATRIX
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun InventoryMatrixCard(appColors: BloodSyncColors) {
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        RoundedCornerShape(20.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Blood Inventory", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    Text("All 8 blood groups — live stock", fontSize = 11.sp, color = appColors.textMuted)
                }
                Box(Modifier.background(BloodRedLight, RoundedCornerShape(50.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("+ Stock", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BloodRedPrimary)
                }
            }
            Spacer(Modifier.height(14.dp))
            INVENTORY_DATA.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { InventoryTile(it, appColors, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun InventoryTile(item: BloodInventoryItem, appColors: BloodSyncColors, modifier: Modifier = Modifier) {
    val (bgColor, txtColor) = bgGroupColors(item.group)
    val barColor = when { item.isLow -> BloodRedPrimary; item.fraction > 0.65f -> Color(0xFF2E7D32); else -> Color(0xFFF57F17) }
    val animFraction by animateFloatAsState(item.fraction, tween(1000, easing = FastOutSlowInEasing), label = "bar")
    val tileBg = if (item.isLow) Color(0xFFFFF1F1) else appColors.surfaceVariant

    Surface(
        modifier.clip(RoundedCornerShape(14.dp)),
        RoundedCornerShape(14.dp),
        color = tileBg,
        border = androidx.compose.foundation.BorderStroke(1.dp,
            if (item.isLow) BloodRedPrimary.copy(alpha = 0.25f) else appColors.border)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).background(bgColor, CircleShape)
                        .border(1.5.dp, txtColor.copy(alpha = 0.3f), CircleShape),
                    Alignment.Center
                ) {
                    Text(item.group, fontSize = if (item.group.length > 2) 9.sp else 11.sp,
                        fontWeight = FontWeight.Black, color = txtColor)
                }
                if (item.isLow) {
                    Box(Modifier.background(BloodRedLight, RoundedCornerShape(50.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("LOW", fontSize = 8.sp, fontWeight = FontWeight.Black, color = BloodRedPrimary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${item.units}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = appColors.textPrimary)
            Text("/ ${item.maxUnits} max", fontSize = 10.sp, color = appColors.textMuted)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50.dp)).background(appColors.border)) {
                Box(Modifier.fillMaxWidth(animFraction).fillMaxHeight().clip(RoundedCornerShape(50.dp)).background(barColor))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// LIVE FEED CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LiveFeedCard(appColors: BloodSyncColors) {
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        RoundedCornerShape(20.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            val inf = rememberInfiniteTransition(label = "live")
            val dotAlpha by inf.animateFloat(1f, 0.3f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "dot")
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Color(0xFF16A34A).copy(alpha = dotAlpha), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Live Activity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                }
                Text("Auto-updating", fontSize = 10.sp, color = appColors.textMuted)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            FEED_DATA.forEach { event ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(30.dp).clip(RoundedCornerShape(50.dp)).background(event.accent))
                    Spacer(Modifier.width(10.dp))
                    Text(event.icon, fontSize = 16.sp, modifier = Modifier.width(26.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(event.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = appColors.textPrimary, maxLines = 2)
                        Text(event.timeAgo, fontSize = 10.sp, color = appColors.textMuted)
                    }
                }
                HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ACTIVE SOS BANNER
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ActiveSosBanner(
    hospitalName: String, bloodGroup: String, units: Int,
    onDispatch: () -> Unit, appColors: BloodSyncColors
) {
    val inf = rememberInfiniteTransition(label = "sos")
    val dotAlpha by inf.animateFloat(1f, 0.2f, infiniteRepeatable(tween(700), RepeatMode.Reverse), "sosDot")
    Card(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onDispatch),
        RoundedCornerShape(20.dp),
        CardDefaults.cardColors(containerColor = BloodRedPrimary),
        CardDefaults.cardElevation(4.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.size(100.dp).offset(x = 30.dp, y = (-30).dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape).align(Alignment.TopEnd))
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Color(0xFFFACC15).copy(alpha = dotAlpha), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("🚨 LIVE SOS ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Black,
                        color = Color(0xFFFFCDD2), letterSpacing = 0.5.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(hospitalName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Needs $units unit(s) of $bloodGroup — urgent dispatch required",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDispatch,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) { Text("Dispatch Now →", color = BloodRedPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    OutlinedButton(
                        onClick = {},
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.height(42.dp)
                    ) { Text("Details", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DONOR ACTIVITY CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DonorActivityCard(appColors: BloodSyncColors) {
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        RoundedCornerShape(20.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Donor Activity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    Text("Recent registrations & status", fontSize = 11.sp, color = appColors.textMuted)
                }
                Box(Modifier.background(BloodRedLight, RoundedCornerShape(50.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${DONOR_DATA.size} Records", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BloodRedPrimary)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            DONOR_DATA.forEach { donor ->
                val (bgColor, txtColor) = bgGroupColors(donor.bloodGroup)
                val initials = donor.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                Row(
                    Modifier.fillMaxWidth().clickable {}.padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).background(appColors.surfaceVariant, CircleShape)
                            .border(1.dp, appColors.border, CircleShape),
                        Alignment.Center
                    ) { Text(initials, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(donor.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = appColors.textPrimary, maxLines = 1)
                        Text("${donor.city} • ${donor.date}", fontSize = 10.sp, color = appColors.textMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(32.dp).background(bgColor, CircleShape)
                            .border(1.dp, txtColor.copy(alpha = 0.3f), CircleShape),
                        Alignment.Center
                    ) {
                        Text(donor.bloodGroup, fontSize = if (donor.bloodGroup.length > 2) 7.sp else 9.sp,
                            fontWeight = FontWeight.Black, color = txtColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.background(donor.status.bgColor, RoundedCornerShape(50.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(donor.status.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = donor.status.textColor)
                    }
                }
                HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// TODAY'S SUMMARY CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun TodaySummaryCard(appColors: BloodSyncColors) {
    val animProg by animateFloatAsState(0.68f, tween(1200, easing = FastOutSlowInEasing), label = "goal")
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        RoundedCornerShape(20.dp),
        color = appColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Today's Summary", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
            Spacer(Modifier.height(12.dp))
            listOf(
                Triple("Donations Completed","18", Color(0xFF16A34A)),
                Triple("New Registrations","7",   BloodRedPrimary),
                Triple("SOS Dispatched","4",       Color(0xFFF57F17)),
                Triple("Active Camps","2",          Color(0xFF7C3AED)),
            ).forEach { (label, value, color) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 13.sp, color = appColors.textSecondary)
                    }
                    Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = appColors.textPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = appColors.divider, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Monthly Goal", fontSize = 12.sp, color = appColors.textMuted)
                Text("68%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = BloodRedPrimary)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50.dp)).background(appColors.border)) {
                Box(Modifier.fillMaxWidth(animProg).fillMaxHeight().clip(RoundedCornerShape(50.dp)).background(BloodRedPrimary))
            }
            Spacer(Modifier.height(4.dp))
            Text("340 / 500 donations this month", fontSize = 10.sp, color = appColors.textMuted)
        }
    }
}
