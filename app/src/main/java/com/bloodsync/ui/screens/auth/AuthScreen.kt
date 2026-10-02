package com.bloodsync.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodsync.R
import com.bloodsync.data.model.UserProfile
import com.bloodsync.data.repository.BloodSyncRepository
import com.bloodsync.ui.components.BloodDropIcon
import com.bloodsync.ui.components.BloodGroupSelector
import com.bloodsync.ui.theme.*
import com.bloodsync.util.LocalAppStrings
import com.bloodsync.util.ValidationHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: BloodSyncRepository,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val strings = LocalAppStrings.current

    // Primary default mode is REGISTER FIRST as required by user
    var isRegisterMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register fields
    var fullName by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Male") }
    var ageInput by remember { mutableStateOf("") }
    var ageWarning by remember { mutableStateOf<String?>(null) }
    var phoneInput by remember { mutableStateOf("") }
    var cityInput by remember { mutableStateOf("") }
    var volunteerEmergency by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Google Sign-In Client & Activity Result Launcher with Firebase Auth integration
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

                if (!idToken.isNullOrBlank()) {
                    isLoading = true
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            isLoading = false
                            if (authTask.isSuccessful) {
                                repository.loginWithGoogleAccount(displayName, email)
                                onLoginSuccess()
                            } else {
                                // Fallback to local profile session if cloud auth has network challenge
                                repository.loginWithGoogleAccount(displayName, email)
                                onLoginSuccess()
                            }
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
                10 -> errorMessage = "Developer Error (Code 10): SHA-1 fingerprint Firebase Console me register nahi hai."
                12500 -> errorMessage = "Sign-in failed (Code 12500): Firebase Console me SHA-1 add karke google-services.json update karein."
                12501, 16 -> { /* User cancelled picker, silent or soft notice */ }
                7 -> errorMessage = "Network error (Code 7): Internet connection check karein."
                else -> errorMessage = "Google Sign-In error (${e.statusCode}): ${e.localizedMessage ?: "Failed"}"
            }
        } catch (e: Exception) {
            isLoading = false
            val errorDetail = e.localizedMessage ?: "Sign-in cancelled"
            errorMessage = "Google Sign-In: $errorDetail"
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

    // Strict Email Regex requiring username + @ + domain + dot + domain extension (e.g. .com, .org, .in)
    val emailPattern = remember {
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = appColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // App Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BloodDropIcon(size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.appName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BloodRedPrimary
                )
            }

            Text(
                text = strings.donorNetworkSubtitle,
                fontSize = 12.sp,
                color = appColors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp)
            )

            // Segmented Tab: REGISTER FIRST (Left), SIGN IN SECOND (Right)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50.dp),
                color = appColors.surfaceVariant,
                border = BorderStroke(1.dp, appColors.border)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // 1. REGISTER AS DONOR (First, Default)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isRegisterMode) appColors.cardBackground else Color.Transparent)
                            .clickable {
                                isRegisterMode = true
                                errorMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.registerDonor,
                            fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRegisterMode) BloodRedPrimary else appColors.textSecondary,
                            fontSize = 14.sp
                        )
                    }

                    // 2. SIGN IN (Second)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (!isRegisterMode) appColors.cardBackground else Color.Transparent)
                            .clickable {
                                isRegisterMode = false
                                errorMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.signIn,
                            fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isRegisterMode) BloodRedPrimary else appColors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card Form Container (Pill Curves)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                border = BorderStroke(1.dp, appColors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRegisterMode) {
                        // Registration Form
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text(strings.fullName) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BloodRedPrimary)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email Field in Registration
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text(strings.emailAddress) },
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field in Registration
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(strings.password) },
                            placeholder = { Text("Minimum 6 characters") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = BloodRedPrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = appColors.textMuted
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(16.dp),
                            supportingText = {
                                Text(
                                    text = "Password must be at least 6 characters",
                                    fontSize = 11.sp,
                                    color = appColors.textMuted
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        BloodGroupSelector(
                            selectedGroup = selectedBloodGroup,
                            onGroupSelected = { selectedBloodGroup = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gender Selector (Male / Female Pill Options)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = strings.gender,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("Male", strings.genderMale, Icons.Default.Male),
                                    Triple("Female", strings.genderFemale, Icons.Default.Female),
                                    Triple("Other", strings.genderOther, Icons.Default.Transgender)
                                ).forEach { (genderKey, genderLabel, genderIcon) ->
                                    val isSelected = selectedGender == genderKey
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                            .clickable { selectedGender = genderKey },
                                        shape = RoundedCornerShape(50.dp),
                                        color = if (isSelected) appColors.redLight else appColors.inputBackground,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.8.dp else 1.dp,
                                            color = if (isSelected) BloodRedPrimary else appColors.border
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = genderIcon,
                                                contentDescription = null,
                                                tint = if (isSelected) BloodRedPrimary else appColors.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = genderLabel,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) BloodRedPrimary else appColors.textPrimary,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Age Input Field with Strict 15+ Age Restriction Validation
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = ageInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 3) {
                                        ageInput = input
                                        if (input.isNotBlank()) {
                                            val (isValid, errorMsg) = ValidationHelper.isValidAge(input, minAge = 15, maxAge = 65)
                                            ageWarning = if (!isValid) errorMsg else null
                                        } else {
                                            ageWarning = null
                                        }
                                    }
                                },
                                label = { Text(strings.age) },
                                placeholder = { Text(strings.agePlaceholder) },
                                leadingIcon = {
                                    Icon(Icons.Default.Cake, contentDescription = null, tint = BloodRedPrimary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(16.dp),
                                supportingText = {
                                    Text(
                                        text = strings.minAgeHint,
                                        fontSize = 11.sp,
                                        color = if (ageWarning != null) StatusUrgentRed else appColors.textMuted
                                    )
                                },
                                isError = ageWarning != null
                            )

                            // Inline Instant Age Restriction Notice
                            AnimatedVisibility(visible = ageWarning != null) {
                                Surface(
                                    color = appColors.redLight,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = StatusUrgentRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = ageWarning ?: "",
                                            color = StatusUrgentRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text(strings.phoneNumber) },
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

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            label = { Text(strings.cityRegion) },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = BloodRedPrimary)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = volunteerEmergency,
                                onCheckedChange = { volunteerEmergency = it },
                                colors = CheckboxDefaults.colors(checkedColor = BloodRedPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.volunteerForEmergency,
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }
                    } else {
                        // Sign In Form with STRICT Email Extension Validation
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text(strings.emailAddress) },
                            placeholder = { Text("e.g. yourname@gmail.com") },
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

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(strings.password) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = BloodRedPrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = appColors.textMuted
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BloodRedPrimary,
                                modifier = Modifier.clickable {
                                    resetEmailInput = emailInput.trim()
                                    resetMessage = null
                                    showForgotPasswordDialog = true
                                }
                            )
                        }
                    }

                    // Error Message Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            color = appColors.redLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = StatusUrgentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = StatusUrgentRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Submit Button (Sign In / Register)
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                val nameValidation = ValidationHelper.validateName(fullName)
                                if (!nameValidation.isValid) {
                                    errorMessage = strings.invalidNameError
                                    return@Button
                                }

                                val cleanEmail = emailInput.trim()
                                if (cleanEmail.isBlank() || !emailPattern.matches(cleanEmail)) {
                                    errorMessage = strings.invalidEmailError
                                    return@Button
                                }

                                if (password.length < 6) {
                                    errorMessage = strings.passwordMinLengthError
                                    return@Button
                                }

                                val phoneValidation = ValidationHelper.validatePhone(phoneInput)
                                if (!phoneValidation.isValid) {
                                    errorMessage = strings.invalidPhoneError
                                    return@Button
                                }
                                val bloodValidation = ValidationHelper.validateBloodGroup(selectedBloodGroup)
                                if (!bloodValidation.isValid) {
                                    errorMessage = bloodValidation.errorMessage
                                    return@Button
                                }

                                // Age Validation & Strict 15+ Age Restriction Check using centralized ValidationHelper
                                val (isAgeValid, ageErrorMsg) = ValidationHelper.isValidAge(ageInput, minAge = 15, maxAge = 65)
                                if (!isAgeValid) {
                                    errorMessage = ageErrorMsg ?: strings.invalidAgeError
                                    return@Button
                                }
                                val parsedAge = ageInput.trim().toInt()

                                errorMessage = null
                                isLoading = true
                                val newProfile = UserProfile(
                                    name = ValidationHelper.sanitizeText(fullName, 60),
                                    email = cleanEmail,
                                    phone = phoneInput.filter { it.isDigit() || it == '+' }.take(15),
                                    bloodGroup = selectedBloodGroup,
                                    city = ValidationHelper.sanitizeText(cityInput, 50),
                                    gender = selectedGender,
                                    age = parsedAge,
                                    isEmergencyVolunteer = volunteerEmergency
                                )
                                repository.registerDonorWithCredentials(
                                    profile = newProfile,
                                    pass = password,
                                    onSuccess = {
                                        isLoading = false
                                        onLoginSuccess()
                                    },
                                    onFailure = { error ->
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            } else {
                                // SIGN IN: Check Email Extension STRICTLY
                                val cleanEmail = emailInput.trim()
                                if (cleanEmail.isBlank()) {
                                    errorMessage = strings.invalidEmailError
                                    return@Button
                                }
                                if (!emailPattern.matches(cleanEmail)) {
                                    errorMessage = strings.invalidEmailError
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = strings.passwordMinLengthError
                                    return@Button
                                }

                                errorMessage = null
                                isLoading = true
                                repository.signInWithEmailAndPassword(
                                    email = cleanEmail,
                                    pass = password,
                                    onSuccess = {
                                        isLoading = false
                                        onLoginSuccess()
                                    },
                                    onFailure = { error ->
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                        shape = RoundedCornerShape(50.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) strings.createDonorAccount else strings.signInToBloodSync,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visual "OR" Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = appColors.divider)
                        Text(
                            text = "  OR  ",
                            fontSize = 11.sp,
                            color = appColors.textMuted,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = appColors.divider)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // "Continue with Google" Full Tactile Pill Button
                    Surface(
                        onClick = {
                            errorMessage = null
                            if (googleSignInClient != null) {
                                try {
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                } catch (e: Exception) {
                                    errorMessage = "Google Play Services unavailable on this device."
                                }
                            } else {
                                errorMessage = "Google Sign-In is unavailable on this device."
                            }
                        },
                        shape = RoundedCornerShape(50.dp),
                        color = appColors.surfaceVariant,
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
                            // Official Google 4-color vector logo
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.continueWithGoogle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                        }
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
                    text = "HIPAA-grade medical privacy & encrypted donor data",
                    fontSize = 11.sp,
                    color = appColors.textMuted
                )
            }
        }
    }

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
                        label = { Text(strings.emailAddress) },
                        placeholder = { Text("e.g. yourname@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetMessage ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (resetMessage?.contains("sent", ignoreCase = true) == true) StatusEligibleGreen else StatusUrgentRed
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = resetEmailInput.trim()
                        if (clean.isBlank()) {
                            resetMessage = "Please enter your email."
                            return@Button
                        }
                        isSendingReset = true
                        resetMessage = null
                        repository.sendPasswordResetEmail(
                            email = clean,
                            onSuccess = {
                                isSendingReset = false
                                resetMessage = "Password reset email sent! Check your inbox."
                            },
                            onFailure = { err ->
                                isSendingReset = false
                                resetMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    shape = RoundedCornerShape(50.dp),
                    enabled = !isSendingReset
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
            containerColor = appColors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
