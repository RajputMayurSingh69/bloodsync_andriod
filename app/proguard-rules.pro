# =======================================================================
# BloodSync Android Security & Obfuscation ProGuard Rules
# =======================================================================

# Keep Kotlin Compose runtime and compiler intrinsics
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep Firebase Firestore & Auth data models for reflection & serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.example.bloodsync_android.data.model.** {
    <fields>;
    <init>(...);
    public <methods>;
}
-keep class com.example.bloodsync_android.data.model.** { *; }

# Keep Firebase models and libraries
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Jetpack Security Crypto & Keystore
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Strip all verbose, debug, and info logging in release builds for hacker protection
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
