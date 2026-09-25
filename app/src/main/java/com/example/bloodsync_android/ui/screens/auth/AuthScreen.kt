package com.example.bloodsync_android.ui.screens.auth

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsync_android.data.model.UserProfile
import com.example.bloodsync_android.data.repository.BloodSyncRepository
import com.example.bloodsync_android.ui.components.BloodDropIcon
import com.example.bloodsync_android.ui.components.BloodGroupSelector
import com.example.bloodsync_android.ui.theme.*
import com.example.bloodsync_android.util.LanguageManager
import com.example.bloodsync_android.util.ValidationHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: BloodSyncRepository,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val appColors = BloodSyncTheme.colors
    val currentLanguage by repository.appLanguage
    val strings = remember(currentLanguage) { LanguageManager.getStrings(currentLanguage) }

    var isRegisterMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register fields
    var fullName by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf("O+") }
    var phoneInput by remember { mutableStateOf("") }
    var cityInput by remember { mutableStateOf("") }
    var volunteerEmergency by remember { mutableStateOf(true) }

    // State
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Google Sign-In Client & Activity Result Launcher
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
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
                repository.loginWithGoogleAccount(displayName, email)
                onLoginSuccess()
            } else {
                errorMessage = "Google sign-in was not completed."
            }
        } catch (e: Exception) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BloodDropIcon(size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.appName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BloodRedPrimary
                )
            }

            Text(
                text = "Fast, reliable real-time blood donor network",
                fontSize = 12.sp,
                color = appColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
            )

            // Segmented Tab for Login / Register (Pill Shape)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
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

                        BloodGroupSelector(
                            selectedGroup = selectedBloodGroup,
                            onGroupSelected = { selectedBloodGroup = it }
                        )

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

                                errorMessage = null
                                isLoading = true
                                scope.launch {
                                    delay(500)
                                    val newProfile = UserProfile(
                                        name = ValidationHelper.sanitizeText(fullName, 60),
                                        email = cleanEmail,
                                        phone = phoneInput.filter { it.isDigit() || it == '+' }.take(15),
                                        bloodGroup = selectedBloodGroup,
                                        city = ValidationHelper.sanitizeText(cityInput, 50),
                                        isEmergencyVolunteer = volunteerEmergency
                                    )
                                    // Registers directly in Firebase Firestore and posts notification
                                    repository.registerDonor(newProfile)
                                    repository.setLoggedIn(true)
                                    isLoading = false
                                    onLoginSuccess()
                                }
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
                                scope.launch {
                                    delay(400)
                                    repository.loginUser(
                                        name = cleanEmail.substringBefore("@").replace(".", " ").capitalize(),
                                        email = cleanEmail,
                                        phone = "",
                                        bloodGroup = "O+"
                                    )
                                    isLoading = false
                                    onLoginSuccess()
                                }
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
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
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
                            // Google Circular Badge
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }
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
}
