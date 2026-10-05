package com.bloodsync.data.notification

import android.content.Context
import android.util.Log
import com.bloodsync.data.model.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Service to handle incoming Firebase Cloud Messaging (FCM) push notifications
 * and token refreshes for real-time Emergency SOS broadcast alerts.
 */
class BloodSyncFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "BloodSyncFCM"

    /**
     * Called whenever a new FCM registration token is generated or refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "FCM registration token refreshed: $token")

        // Persist token in secure local preferences
        saveTokenLocally(token)

        // Sync token to Cloud Firestore for the logged-in donor
        syncTokenToFirestore(token)
    }

    /**
     * Called when a remote FCM push notification is received while the app is in foreground or background.
     *
     * The data payload dispatched by the backend contains:
     *   type, requestId, bloodGroup, hospital, patient, units, urgency, emergencyLat, emergencyLon, radarRadiusKm
     *
     * All fields are forwarded as Intent extras so MainActivity can deep-link into
     * EmergencyLiveTrackingScreen for the correct emergency — NOT scoped to the
     * local logged-in user's session.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(tag, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        // Extract emergency identity — intentionally NOT filtered by local userId
        val requestId = data["requestId"] ?: ""
        Log.d(tag, "Emergency requestId from FCM payload: \"$requestId\"")

        val title = notification?.title
            ?: data["title"]
            ?: "🚨 Emergency Blood Alert!"

        val body = notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: "Urgent blood request broadcast in your area. Tap to view details."

        val typeStr = data["type"] ?: "EMERGENCY"
        val notificationType = try {
            NotificationType.valueOf(typeStr)
        } catch (_: Exception) {
            NotificationType.EMERGENCY
        }

        // Each unique emergency gets its own system notification slot via requestId hash
        val notificationId = if (requestId.isNotBlank()) requestId.hashCode() else System.currentTimeMillis().toInt()

        // Dispatch heads-up notification; pass the full FCM data map as extras so the
        // tap PendingIntent deep-links to the correct EmergencyLiveTrackingScreen
        NotificationHelper.sendSystemNotification(
            context = applicationContext,
            notificationId = notificationId,
            title = title,
            message = body,
            type = notificationType,
            extras = data
        )
    }

    private fun saveTokenLocally(token: String) {
        try {
            val prefs = applicationContext.getSharedPreferences("bloodsync_fcm_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("fcm_token", token).apply()
        } catch (e: Exception) {
            Log.w(tag, "Could not save FCM token locally: ${e.message}")
        }
    }

    private fun syncTokenToFirestore(token: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (!currentUserId.isNullOrBlank()) {
            val db = FirebaseFirestore.getInstance()
            val tokenData = hashMapOf(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
            )

            // Update users/{userId}
            db.collection("users").document(currentUserId)
                .set(tokenData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(tag, "FCM token synced to users/$currentUserId")
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Failed to sync FCM token to users: ${e.message}")
                }

            // Update donors/{userId}
            db.collection("donors").document(currentUserId)
                .set(tokenData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(tag, "FCM token synced to donors/$currentUserId")
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Failed to sync FCM token to donors: ${e.message}")
                }
        }
    }
}
