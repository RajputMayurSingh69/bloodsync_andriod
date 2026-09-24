package com.example.bloodsync_android.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
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
import com.example.bloodsync_android.util.ValidationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: BloodSyncRepository,
    onLoginSuccess: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var emailOrPhone by remember { mutableStateOf("") }
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

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = BloodSyncTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BloodDropIcon(size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BloodSync",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BloodRedPrimary
                )
            }

            Text(
                text = "Fast, reliable real-time blood donor network",
                fontSize = 13.sp,
                color = MedicalTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Segmented Tab for Login / Register
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                color = MedicalSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isRegisterMode) MedicalWhite else Color.Transparent)
                            .clickable {
                                isRegisterMode = false
                                errorMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isRegisterMode) BloodRedPrimary else MedicalTextSecondary,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRegisterMode) MedicalWhite else Color.Transparent)
                            .clickable {
                                isRegisterMode = true
                                errorMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Register Donor",
                            fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRegisterMode) BloodRedPrimary else MedicalTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card Form Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBorder),
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
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MedicalTextMuted)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BloodRedPrimary,
                                focusedLabelColor = BloodRedPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        BloodGroupSelector(
                            selectedGroup = selectedBloodGroup,
                            onGroupSelected = { selectedBloodGroup = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Mobile Phone Number") },
                            placeholder = { Text("+1 (555) 000-0000") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MedicalTextMuted)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BloodRedPrimary,
                                focusedLabelColor = BloodRedPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            label = { Text("City / Region") },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MedicalTextMuted)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BloodRedPrimary,
                                focusedLabelColor = BloodRedPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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
                                text = "Volunteer for 24/7 emergency blood requests nearby",
                                fontSize = 12.sp,
                                color = MedicalTextSecondary
                            )
                        }
                    } else {
                        // Sign In Form
                        OutlinedTextField(
                            value = emailOrPhone,
                            onValueChange = { emailOrPhone = it },
                            label = { Text("Email or Phone") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = MedicalTextMuted)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BloodRedPrimary,
                                focusedLabelColor = BloodRedPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MedicalTextMuted)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MedicalTextMuted
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BloodRedPrimary,
                                focusedLabelColor = BloodRedPrimary
                            )
                        )
                    }

                    // Error Message
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            color = StatusUrgentRedLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = StatusUrgentRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Action Button
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                val nameValidation = ValidationHelper.validateName(fullName)
                                if (!nameValidation.isValid) {
                                    errorMessage = nameValidation.errorMessage
                                    return@Button
                                }
                                val phoneValidation = ValidationHelper.validatePhone(phoneInput)
                                if (!phoneValidation.isValid) {
                                    errorMessage = phoneValidation.errorMessage
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
                                    delay(600)
                                    val newProfile = UserProfile(
                                        name = ValidationHelper.sanitizeText(fullName, 60),
                                        phone = phoneInput.filter { it.isDigit() || it == '+' }.take(15),
                                        bloodGroup = selectedBloodGroup,
                                        city = ValidationHelper.sanitizeText(cityInput, 50),
                                        isEmergencyVolunteer = volunteerEmergency
                                    )
                                    repository.updateUserProfile(newProfile)
                                    repository.setLoggedIn(true)
                                    isLoading = false
                                    onLoginSuccess()
                                }
                            } else {
                                if (emailOrPhone.isBlank()) {
                                    errorMessage = "Please enter email or phone number."
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters for security."
                                    return@Button
                                }
                                errorMessage = null
                                isLoading = true
                                scope.launch {
                                    delay(500)
                                    repository.setLoggedIn(true)
                                    isLoading = false
                                    onLoginSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MedicalWhite,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "Create Donor Account" else "Sign In to BloodSync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MedicalWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Demo Login Button for convenient testing
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                delay(300)
                                repository.setLoggedIn(true)
                                isLoading = false
                                onLoginSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRedPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BloodRedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Instant Demo Login (Verified Donor)",
                            color = BloodRedPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
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
                    tint = MedicalTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HIPAA-grade medical privacy & encrypted donor data",
                    fontSize = 11.sp,
                    color = MedicalTextMuted
                )
            }
        }
    }
}
