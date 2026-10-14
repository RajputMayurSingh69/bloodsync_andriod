package com.bloodsync.ui.screens.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import java.util.Calendar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.R
import com.bloodsync.data.model.UserProfile
import com.bloodsync.data.network.BankLoginPayload
import com.bloodsync.data.network.BankRegisterPayload
import com.bloodsync.data.network.BloodSyncNetworkClient
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.BloodDropIcon
import com.bloodsync.ui.components.BloodGroupSelector
import com.bloodsync.ui.theme.*
import com.bloodsync.util.LocalAppStrings
import com.bloodsync.util.LocationHelper
import com.bloodsync.util.ValidationHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

/**
 * Top-Level Portal Role Enum
 * PORTAL 1: User / Donor Sign In & Registration
 * PORTAL 2: Blood Bank Organization Registration & Sign In
 */
enum class AuthPortalRole {
    USER,
    BLOOD_BANK
}

@Composable
fun AuthScreen(
    repository: BloodSyncRepository,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    // Active Portal Role Selection (Default: User Portal)
    var selectedPortal by remember { mutableStateOf(AuthPortalRole.USER) }

    // =========================================================================
    // Portal 1: User / Donor State
    // =========================================================================
    var isUserRegisterMode by remember { mutableStateOf(false) } // Default: User Sign In
    var userEmailInput by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }
    var userPasswordVisible by remember { mutableStateOf(false) }

    // Donor Registration fields
    var fullName by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Male") }
    var dobInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var dobWarning by remember { mutableStateOf<String?>(null) }
    var phoneInput by remember { mutableStateOf("") }
    var cityInput by remember { mutableStateOf("") }
    var volunteerEmergency by remember { mutableStateOf(true) }

    // Donor GPS Coordinates for 10km Radar Proximity
    var donorLatitude by remember { mutableStateOf<Double?>(null) }
    var donorLongitude by remember { mutableStateOf<Double?>(null) }

    // =========================================================================
    // Portal 2: Blood Bank State
    // =========================================================================
    var isBankRegisterMode by remember { mutableStateOf(true) } // Default: Register Blood Bank
    var bankNameInput by remember { mutableStateOf("") }
    var bankLicenseInput by remember { mutableStateOf("") }
    var bankEmailInput by remember { mutableStateOf("") }
    var bankPhoneInput by remember { mutableStateOf("") }
    var bankCityInput by remember { mutableStateOf("") }
    var bankAddressInput by remember { mutableStateOf("") }
    var bankPasswordInput by remember { mutableStateOf("") }
    var bankPasswordVisible by remember { mutableStateOf(false) }

    // Blood Bank Login fields
    var bankLoginEmail by remember { mutableStateOf("") }
    var bankLoginPassword by remember { mutableStateOf("") }
    var bankLoginPasswordVisible by remember { mutableStateOf(false) }

    // UI Feedback state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }

    // Location detection for donors
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            LocationHelper.getCurrentLocation(
                context = context,
                onLocation = { lat, lon ->
                    donorLatitude = lat
                    donorLongitude = lon
                }
            )
        }
    }

    // Google Sign-In Client (STRICT REQUIREMENT: ONLY FOR USER PORTAL)
    val googleSignInClient = remember {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1044746314963-01csrn69c2eut7dnhqcrr67oml08244n.apps.googleusercontent.com")
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso)
        } catch (_: Throwable) {
            null
        }
    
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val displayName = account.displayName ?: account.givenName ?: "Google User"
                val email = account.email ?: ""
                val idToken = account.idToken

                if (!ValidationHelper.isValidGmail(email)) {
                    errorMessage = "Invalid email format! Only @gmail.com accounts are allowed."
                    return@rememberLauncherForActivityResult
                }

                if (!idToken.isNullOrBlank()) {
                    isLoading = true
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            isLoading = false
                            repository.loginWithGoogleAccount(displayName, email)
                            onLoginSuccess()
                        }
                } else {
                    repository.loginWithGoogleAccount(displayName, email)
                    onLoginSuccess()
                }
            } else {
                errorMessage = "Google sign-in was not completed."
            }
        } catch (e: ApiException) {
            isLoading = false
            when (e.statusCode) {
                10 -> errorMessage = "Developer Error (Code 10): SHA-1 fingerprint Firebase Console me register karein."
                12500 -> errorMessage = "Sign-in failed (Code 12500): Firebase Console update karein."
                12501, 16 -> { /* Cancelled silently */ }
                7 -> errorMessage = "Network error (Code 7): Internet connection check karein."
                else -> errorMessage = "Google Sign-In error (${e.statusCode}): ${e.localizedMessage ?: "Failed"}"
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Google Sign-In: ${e.localizedMessage ?: "Cancelled"}"
        }
    }

    // Date of Birth Calendar Picker Dialog
    val calendar = remember { Calendar.getInstance() }
    val initialYear = remember { calendar.get(Calendar.YEAR) - 20 }
    val initialMonth = remember { calendar.get(Calendar.MONTH) }
    val initialDay = remember { calendar.get(Calendar.DAY_OF_MONTH) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDay = dayOfMonth.toString().padStart(2, '0')
                val formattedMonth = (month + 1).toString().padStart(2, '0')
                dobInput = "$formattedDay/$formattedMonth/$year"

                // Auto-calculate exact age from selected DOB
                val today = Calendar.getInstance()
                var calculatedAge = today.get(Calendar.YEAR) - year
                val currentMonth = today.get(Calendar.MONTH)
                val currentDay = today.get(Calendar.DAY_OF_MONTH)
                if (currentMonth < month || (currentMonth == month && currentDay < dayOfMonth)) {
                    calculatedAge--
                }
                if (calculatedAge in 0..120) {
                    ageInput = calculatedAge.toString()
                    val (isValid, errorMsg) = ValidationHelper.isValidAge(ageInput, minAge = 15, maxAge = 65)
                    dobWarning = if (!isValid) errorMsg else null
                } else {
                    dobWarning = "Please enter a valid date of birth."
                }
            },
            initialYear,
            initialMonth,
            initialDay
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = appColors.textPrimary,
        unfocusedTextColor = appColors.textPrimary,
        focusedContainerColor = appColors.inputBackground,
        unfocusedContainerColor = appColors.inputBackground,
        focusedBorderColor = BloodRedPrimary,
        unfocusedBorderColor = appColors.border,
        focusedLabelColor = BloodRedPrimary,
        unfocusedLabelColor = appColors.textMuted,
        cursorColor = BloodRedPrimary
    )

    val emailPattern = remember {
        "^[A-Za-z0-9._%+-]+@gmail\\.com$".toRegex(RegexOption.IGNORE_CASE)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = appColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =================================================================
            // 1. BRANDED SHOWCASE BANNER (Gradient Red #EF5350 -> #D32F2F)
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(0.04f),
                        spotColor = BloodRedPrimary.copy(0.20f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(BloodRedSecondary, BloodRedDark)
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BloodDropIcon(size = 28.dp, tint = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "BloodSync",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-0.4).sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = if (selectedPortal == AuthPortalRole.USER) "USER PORTAL" else "BLOOD BANK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (selectedPortal == AuthPortalRole.USER)
                                "Every drop creates a second chance."
                            else
                                "Empowering blood banks, saving lives together.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (selectedPortal == AuthPortalRole.USER)
                                "Access your BloodSync workspace and help keep donors, hospitals, and blood banks connected."
                            else
                                "Manage inventory, respond to urgent emergency blood requests, and coordinate with verified donors.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.92f),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedPortal == AuthPortalRole.USER)
                                    "24/7 Connected care for every urgent moment."
                                else
                                    "Verified institution access & regulatory compliance.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // 2. PORTAL SELECTOR: [ 👤 USER PORTAL ] vs [ 🏥 BLOOD BANK PORTAL ]
            // =================================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50.dp),
                color = appColors.surfaceVariant,
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // TAB 1: User Portal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (selectedPortal == AuthPortalRole.USER) BloodRedPrimary else Color.Transparent
                            )
                            .clickable {
                                selectedPortal = AuthPortalRole.USER
                                errorMessage = null
                                successMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (selectedPortal == AuthPortalRole.USER) Color.White else appColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "User / Donor",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPortal == AuthPortalRole.USER) Color.White else appColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // TAB 2: Blood Bank Portal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (selectedPortal == AuthPortalRole.BLOOD_BANK) BloodRedPrimary else Color.Transparent
                            )
                            .clickable {
                                selectedPortal = AuthPortalRole.BLOOD_BANK
                                errorMessage = null
                                successMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = if (selectedPortal == AuthPortalRole.BLOOD_BANK) Color.White else appColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Blood Bank",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPortal == AuthPortalRole.BLOOD_BANK) Color.White else appColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // 3. MAIN FORM CARD CONTAINER
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(0.02f),
                        spotColor = BloodRedPrimary.copy(alpha = 0.10f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedPortal == AuthPortalRole.USER) {
                        // =====================================================
                        // PORTAL 1: USER / DONOR PORTAL
                        // =====================================================
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFC9382B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!isUserRegisterMode) "User Sign In" else "Donor Registration",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                        }

                        Text(
                            text = if (!isUserRegisterMode) "Sign in to your user account" else "Create your verified lifesaver account",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 16.dp)
                        )

                        // Sub-toggle: Sign In vs Create Account
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(50.dp),
                            color = appColors.surfaceVariant,
                            border = BorderStroke(1.dp, appColors.border)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (!isUserRegisterMode) (if (appColors.isDark) Color(0xFF383844) else Color.White) else Color.Transparent)
                                        .clickable {
                                             isUserRegisterMode = false
                                             errorMessage = null
                                         },
                                     contentAlignment = Alignment.Center
                                 ) {
                                     Text(
                                         text = "Sign In",
                                         fontWeight = if (!isUserRegisterMode) FontWeight.Bold else FontWeight.Medium,
                                         color = if (!isUserRegisterMode) (if (appColors.isDark) Color.White else BloodRedPrimary) else appColors.textSecondary,
                                         fontSize = 13.sp
                                     )
                                 }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isUserRegisterMode) (if (appColors.isDark) Color(0xFF383844) else Color.White) else Color.Transparent)
                                        .clickable {
                                             isUserRegisterMode = true
                                             errorMessage = null
                                         },
                                     contentAlignment = Alignment.Center
                                 ) {
                                     Text(
                                         text = "Create Account",
                                         fontWeight = if (isUserRegisterMode) FontWeight.Bold else FontWeight.Medium,
                                         color = if (isUserRegisterMode) (if (appColors.isDark) Color.White else BloodRedPrimary) else appColors.textSecondary,
                                         fontSize = 13.sp
                                     )
                                 }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (!isUserRegisterMode) {
                            // User Sign In Form
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "EMAIL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = userEmailInput,
                                    onValueChange = { userEmailInput = it },
                                    placeholder = { Text("user@gmail.com") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "PASSWORD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = userPassword,
                                    onValueChange = { userPassword = it },
                                    placeholder = { Text("••••••••") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { userPasswordVisible = !userPasswordVisible }) {
                                            Icon(
                                                imageVector = if (userPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = appColors.textMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (userPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BloodRedPrimary,
                                        modifier = Modifier.clickable {
                                            resetEmailInput = userEmailInput.trim()
                                            resetMessage = null
                                            showForgotPasswordDialog = true
                                        }
                                    )
                                }
                            }
                        } else {
                            // Donor Registration Form
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("FULL NAME") },
                                placeholder = { Text("e.g. John Doe") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = BloodRedPrimary)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = userEmailInput,
                                onValueChange = { userEmailInput = it },
                                label = { Text("EMAIL ADDRESS") },
                                placeholder = { Text("e.g. name@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = BloodRedPrimary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = userPassword,
                                onValueChange = { userPassword = it },
                                label = { Text("PASSWORD (MIN 6 CHARS)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = BloodRedPrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { userPasswordVisible = !userPasswordVisible }) {
                                        Icon(
                                            imageVector = if (userPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = appColors.textMuted
                                        )
                                    }
                                },
                                visualTransformation = if (userPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            BloodGroupSelector(
                                selectedGroup = selectedBloodGroup,
                                onGroupSelected = { selectedBloodGroup = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Gender Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Male", "Female", "Other").forEach { gender ->
                                    val isSel = selectedGender == gender
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                            .clickable { selectedGender = gender },
                                        shape = RoundedCornerShape(50.dp),
                                        color = if (isSel) appColors.redLight else appColors.inputBackground,
                                        border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) BloodRedPrimary else appColors.border)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = gender,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) BloodRedPrimary else appColors.textPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Date of Birth (DOB) Input Section
                            OutlinedTextField(
                                value = dobInput,
                                onValueChange = { input ->
                                    if (input.length <= 10 && input.all { it.isDigit() || it == '/' || it == '-' || it == '.' }) {
                                        dobInput = input
                                        val parts = input.split('/', '-', '.')
                                        if (parts.size == 3 && parts[0].length in 1..2 && parts[1].length in 1..2 && parts[2].length == 4) {
                                            val d = parts[0].toIntOrNull()
                                            val m = parts[1].toIntOrNull()
                                            val y = parts[2].toIntOrNull()
                                            if (d != null && m != null && y != null && m in 1..12 && d in 1..31) {
                                                val today = Calendar.getInstance()
                                                var calculatedAge = today.get(Calendar.YEAR) - y
                                                val currentMonth = today.get(Calendar.MONTH) + 1
                                                val currentDay = today.get(Calendar.DAY_OF_MONTH)
                                                if (currentMonth < m || (currentMonth == m && currentDay < d)) {
                                                    calculatedAge--
                                                }
                                                if (calculatedAge in 0..120) {
                                                    ageInput = calculatedAge.toString()
                                                    val (isValid, errorMsg) = ValidationHelper.isValidAge(ageInput, minAge = 15, maxAge = 65)
                                                    dobWarning = if (!isValid) errorMsg else null
                                                } else {
                                                    dobWarning = "Please enter a valid date of birth."
                                                }
                                            }
                                        } else if (input.isBlank()) {
                                            ageInput = ""
                                            dobWarning = null
                                        }
                                    }
                                },
                                label = { Text("DATE OF BIRTH (DOB)") },
                                placeholder = { Text("DD/MM/YYYY (e.g. 15/08/2000)") },
                                leadingIcon = {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = BloodRedPrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date of Birth", tint = BloodRedPrimary)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(16.dp),
                                isError = dobWarning != null
                            )

                            if (dobWarning != null) {
                                Text(
                                    text = dobWarning ?: "",
                                    color = StatusUrgentRed,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("PHONE NUMBER") },
                                placeholder = { Text("+91 98765 43210") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = BloodRedPrimary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = cityInput,
                                onValueChange = { cityInput = it },
                                label = { Text("CITY / DISTRICT") },
                                placeholder = { Text("e.g. Mumbai") },
                                leadingIcon = {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = BloodRedPrimary)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = volunteerEmergency,
                                    onCheckedChange = { volunteerEmergency = it },
                                    colors = CheckboxDefaults.colors(checkedColor = BloodRedPrimary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Volunteer for urgent blood requests in my area",
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }

                        // Error Banner
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = appColors.redLight,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = BloodRedPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = errorMessage ?: "", color = BloodRedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        if (successMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = appColors.greenLight,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = successMessage ?: "", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // User Portal Primary Action Button (52dp Pill)
                        Button(
                            onClick = {
                                errorMessage = null
                                successMessage = null
                                if (!isUserRegisterMode) {
                                    // User Sign In logic
                                    val cleanEmail = userEmailInput.trim()
                                    if (cleanEmail.isBlank() || !emailPattern.matches(cleanEmail)) {
                                        errorMessage = "Invalid email format! Only @gmail.com is allowed (e.g. name@gmail.com)."
                                        return@Button
                                    }
                                    if (userPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    isLoading = true
                                    repository.signInWithEmailAndPassword(
                                        email = cleanEmail,
                                        pass = userPassword,
                                        onSuccess = {
                                            isLoading = false
                                            onLoginSuccess()
                                        },
                                        onFailure = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                } else {
                                    // Donor Register logic
                                    val cleanEmail = userEmailInput.trim()
                                    if (fullName.trim().length < 2) {
                                        errorMessage = "Please enter your full name."
                                        return@Button
                                    }
                                    if (cleanEmail.isBlank() || !emailPattern.matches(cleanEmail)) {
                                        errorMessage = "Invalid email format! Only @gmail.com is allowed (e.g. name@gmail.com)."
                                        return@Button
                                    }
                                    if (userPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    if (selectedBloodGroup.isBlank()) {
                                        errorMessage = "Please select your blood group."
                                        return@Button
                                    }
                                    val phoneVal = ValidationHelper.validatePhone(phoneInput)
                                    if (!phoneVal.isValid) {
                                        errorMessage = "Please enter a valid 10-digit phone number."
                                        return@Button
                                    }
                                    if (dobInput.isBlank()) {
                                        errorMessage = "Please enter or select your Date of Birth (DOB)."
                                        return@Button
                                    }
                                    val (isAgeOk, ageMsg) = ValidationHelper.isValidAge(ageInput, 15, 65)
                                    if (!isAgeOk) {
                                        errorMessage = ageMsg ?: "Donor must be between 15 and 65 years old based on Date of Birth."
                                        return@Button
                                    }

                                    isLoading = true
                                    val newProfile = UserProfile(
                                        name = ValidationHelper.sanitizeText(fullName, 60),
                                        email = cleanEmail,
                                        phone = phoneInput.filter { it.isDigit() || it == '+' }.take(15),
                                        bloodGroup = selectedBloodGroup,
                                        city = ValidationHelper.sanitizeText(cityInput, 50),
                                        gender = selectedGender,
                                        dob = dobInput.trim(),
                                        age = ageInput.trim().toIntOrNull() ?: 20,
                                        isEmergencyVolunteer = volunteerEmergency,
                                        latitude = donorLatitude,
                                        longitude = donorLongitude
                                    )
                                    repository.registerDonorWithCredentials(
                                        profile = newProfile,
                                        pass = userPassword,
                                        onVerificationSent = { msg ->
                                            isLoading = false
                                            successMessage = msg
                                            isUserRegisterMode = false
                                        },
                                        onSuccess = {
                                            isLoading = false
                                            onLoginSuccess()
                                        },
                                        onFailure = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BloodRedPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (!isUserRegisterMode) "Sign In" else "Register as Donor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // STRICT REQUIREMENT: "Continue with Google" ONLY IN USER PORTAL
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = appColors.divider)
                            Text(text = "  OR  ", fontSize = 11.sp, color = appColors.textMuted, fontWeight = FontWeight.Bold)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = appColors.divider)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            onClick = {
                                errorMessage = null
                                if (googleSignInClient != null) {
                                    try {
                                        googleSignInClient.signOut().addOnCompleteListener {
                                            try {
                                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                            } catch (e: Exception) {
                                                errorMessage = "Google Play Services error: ${e.localizedMessage ?: "Unavailable"}"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        try {
                                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                        } catch (e2: Exception) {
                                            errorMessage = "Google Play Services error: ${e2.localizedMessage ?: "Unavailable"}"
                                        }
                                    }
                                } else {
                                    errorMessage = "Google Sign-In is unavailable on this device."
                                }
                            },
                            shape = RoundedCornerShape(50.dp),
                            color = if (appColors.isDark) appColors.surfaceVariant else Color.White,
                            border = BorderStroke(1.dp, appColors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom toggle link
                        Text(
                            text = if (!isUserRegisterMode)
                                "Are you new here? Create a donor account"
                            else
                                "Already have an account? Sign In",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BloodRedPrimary,
                            modifier = Modifier
                                .clickable {
                                    isUserRegisterMode = !isUserRegisterMode
                                    errorMessage = null
                                }
                                .padding(4.dp)
                        )

                    } else {
                        // =====================================================
                        // PORTAL 2: BLOOD BANK PORTAL (Strictly NO Google OAuth!)
                        // =====================================================
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = Color(0xFFC9382B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBankRegisterMode) "Blood Bank Registration" else "Blood Bank Sign In",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                        }

                        Text(
                            text = if (isBankRegisterMode)
                                "Register your certified blood bank organization"
                            else
                                "Sign in to blood bank management workspace",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 16.dp)
                        )

                        // Sub-toggle inside Blood Bank Portal: Register vs Sign In
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(50.dp),
                            color = appColors.surfaceVariant,
                            border = BorderStroke(1.dp, appColors.border)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isBankRegisterMode) (if (appColors.isDark) Color(0xFF383844) else Color.White) else Color.Transparent)
                                        .clickable {
                                            isBankRegisterMode = true
                                            errorMessage = null
                                            successMessage = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Register Bank",
                                        fontWeight = if (isBankRegisterMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isBankRegisterMode) (if (appColors.isDark) Color.White else BloodRedPrimary) else appColors.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (!isBankRegisterMode) (if (appColors.isDark) Color(0xFF383844) else Color.White) else Color.Transparent)
                                        .clickable {
                                            isBankRegisterMode = false
                                            errorMessage = null
                                            successMessage = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Sign In",
                                        fontWeight = if (!isBankRegisterMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isBankRegisterMode) (if (appColors.isDark) Color.White else BloodRedPrimary) else appColors.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isBankRegisterMode) {
                            // 1. BLOOD BANK NAME
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "BLOOD BANK NAME",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankNameInput,
                                    onValueChange = { bankNameInput = it },
                                    placeholder = { Text("e.g. City Red Cross Blood Bank") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. LICENSE / REGISTRATION NUMBER
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "LICENSE / REGISTRATION NUMBER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankLicenseInput,
                                    onValueChange = { bankLicenseInput = it },
                                    placeholder = { Text("e.g. BB-DL-2024-9876") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. OFFICIAL EMAIL ADDRESS
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "OFFICIAL EMAIL ADDRESS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankEmailInput,
                                    onValueChange = { bankEmailInput = it },
                                    placeholder = { Text("bankname@gmail.com") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. CONTACT PHONE NUMBER
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "CONTACT PHONE NUMBER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankPhoneInput,
                                    onValueChange = { bankPhoneInput = it },
                                    placeholder = { Text("+91 98765 43210") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 5. CITY / DISTRICT
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "CITY / DISTRICT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankCityInput,
                                    onValueChange = { bankCityInput = it },
                                    placeholder = { Text("e.g. New Delhi") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 6. FULL OPERATING ADDRESS
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "FULL OPERATING ADDRESS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankAddressInput,
                                    onValueChange = { bankAddressInput = it },
                                    placeholder = { Text("Sector 14, Health Boulevard, Hospital Campus") },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    singleLine = false,
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 7. CREATE SECURE PASSWORD
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "CREATE SECURE PASSWORD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankPasswordInput,
                                    onValueChange = { bankPasswordInput = it },
                                    placeholder = { Text("Minimum 6 characters") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { bankPasswordVisible = !bankPasswordVisible }) {
                                            Icon(
                                                imageVector = if (bankPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = appColors.textMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (bankPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        } else {
                            // Blood Bank Sign In
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "OFFICIAL EMAIL ADDRESS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankLoginEmail,
                                    onValueChange = { bankLoginEmail = it },
                                    placeholder = { Text("bankname@gmail.com") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "PASSWORD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textMuted,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = bankLoginPassword,
                                    onValueChange = { bankLoginPassword = it },
                                    placeholder = { Text("••••••••") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = BloodRedPrimary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { bankLoginPasswordVisible = !bankLoginPasswordVisible }) {
                                            Icon(
                                                imageVector = if (bankLoginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = appColors.textMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (bankLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }

                        // Feedback Messages
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = appColors.redLight,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BloodRedPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = BloodRedPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = errorMessage ?: "", color = BloodRedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        if (successMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = appColors.greenLight,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, StatusEligibleGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = successMessage ?: "", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Blood Bank Action Button (STRICT: NO GOOGLE AUTH BUTTON ANYWHERE HERE!)
                        Button(
                            onClick = {
                                errorMessage = null
                                successMessage = null

                                if (isBankRegisterMode) {
                                    // Validation
                                    if (bankNameInput.trim().length < 3) {
                                        errorMessage = "Blood Bank name must be at least 3 characters."
                                        return@Button
                                    }
                                    if (bankLicenseInput.trim().length < 3) {
                                        errorMessage = "License number must be at least 3 characters."
                                        return@Button
                                    }
                                    val email = bankEmailInput.trim()
                                    if (email.isBlank() || !emailPattern.matches(email)) {
                                        errorMessage = "Invalid email format! Only @gmail.com is allowed (e.g. name@gmail.com)."
                                        return@Button
                                    }
                                    val phoneVal = ValidationHelper.validatePhone(bankPhoneInput)
                                    if (!phoneVal.isValid) {
                                        errorMessage = "Valid contact phone number (min 10 digits) is required."
                                        return@Button
                                    }
                                    if (bankCityInput.trim().length < 2) {
                                        errorMessage = "City / District is required."
                                        return@Button
                                    }
                                    if (bankAddressInput.trim().length < 6) {
                                        errorMessage = "Full operating address is required."
                                        return@Button
                                    }
                                    if (bankPasswordInput.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }

                                    isLoading = true
                                    repository.registerBloodBankWithCredentials(
                                        bankName = bankNameInput.trim(),
                                        licenseNumber = bankLicenseInput.trim(),
                                        email = email,
                                        phone = bankPhoneInput.trim(),
                                        city = bankCityInput.trim(),
                                        address = bankAddressInput.trim(),
                                        pass = bankPasswordInput,
                                        onVerificationSent = { msg ->
                                            isLoading = false
                                            successMessage = msg
                                            isBankRegisterMode = false
                                        },
                                        onSuccess = {
                                            isLoading = false
                                            onLoginSuccess()
                                        },
                                        onFailure = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                } else {
                                    // Blood Bank Sign In
                                    val email = bankLoginEmail.trim()
                                    if (email.isBlank() || !emailPattern.matches(email)) {
                                        errorMessage = "Invalid email format! Only @gmail.com is allowed (e.g. name@gmail.com)."
                                        return@Button
                                    }
                                    if (bankLoginPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }

                                    isLoading = true
                                    repository.signInBloodBankWithEmailAndPassword(
                                        email = email,
                                        pass = bankLoginPassword,
                                        onSuccess = {
                                            isLoading = false
                                            onLoginSuccess()
                                        },
                                        onFailure = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BloodRedPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isBankRegisterMode) "Register Blood Bank" else "Sign In as Blood Bank",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Toggle inside Blood Bank
                        Text(
                            text = if (isBankRegisterMode)
                                "Already registered? Sign In as Blood Bank"
                            else
                                "New Blood Bank? Register organization",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BloodRedPrimary,
                            modifier = Modifier
                                .clickable {
                                    isBankRegisterMode = !isBankRegisterMode
                                    errorMessage = null
                                    successMessage = null
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))


            // Healthcare privacy note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = appColors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HIPAA-grade medical privacy & certified institution encryption",
                    fontSize = 11.sp,
                    color = appColors.textMuted
                )
            }
        }
    }

    // Password Reset Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSendingReset) showForgotPasswordDialog = false
            },
            title = {
                Text(text = "Reset Password", fontWeight = FontWeight.Bold, color = appColors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered email address. We will send a secure password reset link to your inbox.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("EMAIL ADDRESS") },
                        placeholder = { Text("e.g. yourname@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (resetMessage?.contains("sent", ignoreCase = true) == true) appColors.greenLight else appColors.redLight,
                            border = BorderStroke(1.dp, if (resetMessage?.contains("sent", ignoreCase = true) == true) StatusEligibleGreen.copy(alpha = 0.4f) else BloodRedPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = resetMessage ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (resetMessage?.contains("sent", ignoreCase = true) == true) StatusEligibleGreen else BloodRedPrimary
                                )
                                if (resetMessage?.contains("sent", ignoreCase = true) == true) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "📁 Email nahi dikh raha? Gmail ke SPAM / JUNK ya PROMOTIONS folder ko zaroor check karein.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (appColors.isDark) StatusEligibleGreen else Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                                    addCategory(Intent.CATEGORY_APP_EMAIL)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Open Email App"))
                                            } catch (_: Exception) {
                                                try {
                                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com/"))
                                                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    context.startActivity(browserIntent)
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusEligibleGreen),
                                        shape = RoundedCornerShape(50.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Email / Spam Folder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Tip: Make sure this is the exact email registered in BloodSync. Reset links are sent via Firebase noreply.",
                            fontSize = 11.sp,
                            color = appColors.textMuted
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = resetEmailInput.trim().lowercase()
                        if (clean.isBlank()) {
                            resetMessage = "Please enter your email."
                            return@Button
                        }
                        if (!emailPattern.matches(clean)) {
                            resetMessage = "Invalid email format! Only @gmail.com is allowed (e.g. name@gmail.com)."
                            return@Button
                        }
                        isSendingReset = true
                        resetMessage = null
                        repository.sendPasswordResetEmail(
                            email = clean,
                            onSuccess = {
                                isSendingReset = false
                                resetMessage = "Password reset link sent to $clean! Please check your Inbox and Spam/Junk folder."
                            },
                            onFailure = { err ->
                                isSendingReset = false
                                resetMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(50.dp),
                    enabled = !isSendingReset,
                    modifier = Modifier.height(44.dp)
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send Reset Link", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close", color = appColors.textSecondary)
                }
            },
            containerColor = appColors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
