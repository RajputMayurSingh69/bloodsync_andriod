# 🩸 BloodSync Android — Official Release v2.0.0

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin%20100%25-orange.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)](https://developer.android.com/jetpack/compose)
[![Backend](https://img.shields.io/badge/Cloud-Firebase%20Firestore%20%26%20Realtime%20DB-yellow.svg)](https://firebase.google.com/)
[![Version](https://img.shields.io/badge/Version-v2.0.0%20Official-crimson.svg)](https://github.com/RajputMayurSingh69/bloodsync_andriod)

**BloodSync** is a production-grade, humanitarian blood donation & emergency broadcast Android mobile application built with 100% Kotlin and Jetpack Compose (Material 3). It connects voluntary blood donors with hospitals and families in critical need in real-time, features direct WhatsApp communication, connects directly to Firebase Cloud databases, and enforces global medical safety standards (including the WHO 3-month whole blood recovery rule).

---

## 🚀 What's New in Version 2.0.0

1. **🔥 Firebase Cloud Server Database**:
   - Integrated with Firebase Firestore & Realtime Database for real-time synchronization.
   - Live server listeners for emergency blood requests, verified donor directory, and blood bank stock updates.
   - Built-in support for your `google-services.json` project configuration with graceful local offline fallback.

2. **💬 Direct WhatsApp & Multi-Platform Messaging**:
   - **One-Tap WhatsApp Donor Chat**: Message matching blood donors directly on WhatsApp with pre-filled blood group inquiry.
   - **SOS Broadcast to WhatsApp**: Instant sharing of emergency blood requirements (blood group, units, hospital, patient, and contact) directly to WhatsApp chats, groups, and WhatsApp Status.

3. **🎨 Clean Light & Dark Theme System**:
   - Simplified display options: **Light** and **Dark** only (System Default removed).
   - Clean, minimalist UI without bracket clutter.
   - Seamless status bar contrast and 3-button navigation insets adaptation.

4. **✨ Official BloodSync Launcher Icon**:
   - 3D ruby-red blood drop with an embedded medical cross and heartbeat lifeline pulse.
   - Embossed modern "BLOODSYNC" brand typography right underneath the drop.
   - Full adaptive icon vector support (`ic_launcher.xml`) and high-resolution mipmap densities (`hdpi`, `mdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).

5. **🧹 Clean Real-Database Architecture**:
   - Removed all hardcoded mock/demo data (fake Alex Rivera profile, fake appointments, fake certificates).
   - Starts clean and allows users to input their real data or sync from Firebase server.
   - Added single-tap local cache clear & data refresh option in Settings.

---

## 📱 Core Features

### 1. 🩸 Splash Screen with Physics Blood Drop
- Pure white background splash screen.
- Smooth physics-based teardrop gravity fall, squash-and-stretch impact bounce, crimson fluid pulse, and expanding landing ripple.

### 2. 🚨 24/7 Emergency Blood SOS System
- **Urgent Broadcast Button**: Pulsating radar Floating Action Button accessible from all main screens.
- **Immediate Broadcast**: Blood group selector (A+, A-, B+, B-, AB+, AB-, O+, O-), required unit counter, hospital name, contact phone, and urgency rating.
- **Live SOS Tracking & WhatsApp Sharing**: Real-time radar status and instant WhatsApp group broadcast.

### 3. 🛡️ Enforced 3-Month (90-Day) Donation Interval Safety Rule
- **Medical Standard**: Strictly enforces the WHO 90-day interval between whole blood donations to safeguard donor hemoglobin and ferritin.
- **Lock Enforcement**: Locks scheduling dates before the 90-day recovery window.

### 4. 📅 Blood Bank Appointment Booking
- Select certified regional blood banks.
- Real-time date picker and flexible time slot chips.
- In-app reschedule and cancellation management with Cloud Firestore sync.

### 5. 🎖️ Appreciation Certificates & History
- Official recognition certificates with gold borders, embossed seal, and unique verification ID.
- Canvas bitmap rendering for direct image download and Android system share sheet.

---

## 📥 APK Download & Local Files

- **GoFile Download Link**: [Download BloodSync v2.0.0 APK (GoFile)](https://gofile.io/d/FPOxsGQg)
- **Built APK (v2.0.0)**: `bloodsync-v2.0.0.apk` (Located in project root)
- **Local Path**: `app/build/outputs/apk/debug/app-debug.apk`
- **Installation via ADB**:
  ```powershell
  adb install -r bloodsync-v2.0.0.apk
  ```

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin (100%)
- **Framework**: Jetpack Compose (Material 3)
- **Cloud Backend**: Firebase BoM (Firestore, Realtime Database, Auth)
- **Architecture**: Clean Architecture / Repository Pattern with Reactive State
- **Storage**: Android `SharedPreferences` + Firebase Firestore Cloud Sync
- **Target SDK**: 37 (Android 15 Ready) | **Min SDK**: 24 (Android 7.0+)
