# 🩸 BloodSync Android — Blood Donor Finder App

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin%20100%25-orange.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/Version-v1.3.0-crimson.svg)](https://gofile.io/d/KIxoRdR6)

**BloodSync** is a production-grade, healthcare-oriented Android mobile application built with 100% Kotlin and Jetpack Compose (Material 3). It connects voluntary blood donors with individuals and hospitals in urgent need in real-time, while strictly enforcing global medical donation interval guidelines (such as the WHO 3-month whole blood recovery rule).

---

## 📱 App Highlights & Core Features

### 1. 🩸 Splash Screen with Physics Blood Drop
- Pure white background splash screen.
- Smooth physics-based teardrop gravity fall, squash-and-stretch impact bounce, crimson fluid pulse, and expanding landing ripple.
- Auto-navigates smoothly after ~2.2 seconds.

### 2. 🚨 24/7 Emergency Blood Request System
- **Urgent Broadcast Button**: Prominent pulsating radar Floating Action Button accessible from all main screens.
- **Immediate Broadcast Form**: Blood group selector (A+, A-, B+, B-, AB+, AB-, O+, O-), required unit counter, hospital name & trauma ward, emergency phone, and urgency rating.
- **Live Tracking Dashboard**: Real-time mock radar tracking nearby matching donors, ETA countdown, and one-tap direct donor dialing.

### 3. 🛡️ Enforced 3-Month (90-Day) Donation Gap Safety Rule
- **Medical Standard**: In accordance with WHO and Red Cross guidelines, donors must wait a minimum of 90 days between whole blood donations to protect iron stores (ferritin) and hemoglobin.
- **Triple-Lock Enforcement**:
  - **Health Tracker**: Interactive recovery countdown ring and real-time days-remaining indicator.
  - **Appointment Booking**: Dynamic date chips and native Android `DatePickerDialog` lock out dates before the 90-day window.
  - **Reschedule & Logging Flow**: Validates interval before confirming appointments or saving historical donation logs.

### 4. 📅 In-App Blood Bank Scheduling
- Select from certified regional blood banks.
- Real-time dynamic date selection with calendar picker.
- Flexible time slot chips (`09:00 AM - 11:00 AM`, `11:00 AM - 01:00 PM`, `02:00 PM - 04:00 PM`, etc.).
- Complete rescheduling dialog and appointment cancellation with reactive state updates.

### 5. 🎖️ Appreciation Certificates & Lifetime History
- Official recognition certificate with gold borders, embossed seal, and unique verification ID.
- Canvas bitmap rendering for direct image download and Android system share sheet integration.
- Lifetime donation metrics: Total liters donated, lives saved, and detailed donation receipts.

### 6. ⚙️ Settings & System Adaptation Engine
- **Theme Selection**:
  - **Phone Jaisa (System Default)**: Mirrors device dark/light setting.
  - **Light Mode**: Clean medical white with crimson accents.
  - **Dark Mode**: Sleek slate-dark theme designed for low-light comfort.
- **3-Button Navigation Auto-Adjustment**: Dynamically measures system navigation bars (`WindowInsets.navigationBars`), ensuring bottom tabs and action buttons never get blocked on phones with 3-button navigation.
- **Smart Adaptive Status Bar**: Transparent system bars with auto-contrasting dark/light icons so clock, battery %, and Wi-Fi are always crystal clear and never washed out.
- **Notification Preferences & Demo Data Reset**: Push notification switches and single-tap demo data restore.

---

## 📥 Download & Installation

- **Direct Download Link**: [Download BloodSync APK (v1.3.0)](https://gofile.io/d/KIxoRdR6)
- **Local APK Path**: `BloodSync.apk` (Project root) and `C:\Users\ADMIN\Desktop\BloodSync.apk`
- **Installation via ADB**:
  ```powershell
  adb install -r BloodSync.apk
  ```

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin (100%)
- **Framework**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture / Repository Pattern with Reactive Compose State
- **Storage**: Android `SharedPreferences` with JSON Serialization
- **System Insets**: WindowInsets Edge-to-Edge with `WindowCompat.getInsetsController`
- **Target SDK**: 37 (Android 15 Ready) | **Min SDK**: 24 (Android 7.0+)
