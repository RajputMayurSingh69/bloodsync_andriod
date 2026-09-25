package com.example.bloodsync_android.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bloodsync_android.MainActivity
import com.example.bloodsync_android.data.model.NotificationType

object NotificationHelper {

    const val CHANNEL_EMERGENCY = "channel_emergency_alerts"
    const val CHANNEL_APPOINTMENTS = "channel_appointments"
    const val CHANNEL_HEALTH = "channel_health_eligibility"
    const val CHANNEL_CERTIFICATES = "channel_certificates"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Emergency Channel (High Importance with Heads-up vibration)
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency Blood Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts for emergency blood requests near you"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }

            // 2. Appointments Channel
            val appointmentChannel = NotificationChannel(
                CHANNEL_APPOINTMENTS,
                "Donation Appointments",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for scheduled donation appointments"
            }

            // 3. Health & Eligibility Channel
            val healthChannel = NotificationChannel(
                CHANNEL_HEALTH,
                "Eligibility & Health Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates on your donation eligibility and health milestones"
            }

            // 4. Certificates Channel
            val certificateChannel = NotificationChannel(
                CHANNEL_CERTIFICATES,
                "Appreciation Certificates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when your verified donation certificates are ready"
            }

            notificationManager.createNotificationChannels(
                listOf(emergencyChannel, appointmentChannel, healthChannel, certificateChannel)
            )
        }
    }

    fun sendSystemNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        type: NotificationType
    ) {
        // If Android 13+, check POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted; in-app notification center will still store it
                return
            }
        }

        val channelId = when (type) {
            NotificationType.EMERGENCY -> CHANNEL_EMERGENCY
            NotificationType.APPOINTMENT -> CHANNEL_APPOINTMENTS
            NotificationType.ELIGIBILITY -> CHANNEL_HEALTH
            NotificationType.CERTIFICATE -> CHANNEL_CERTIFICATES
            NotificationType.SYSTEM -> CHANNEL_HEALTH
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.bloodsync_android.R.mipmap.ic_launcher)
            .setContentTitle("BloodSync Alert")
            .setContentText("You have a new confidential update.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.bloodsync_android.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setPriority(
                if (type == NotificationType.EMERGENCY)
                    NotificationCompat.PRIORITY_HIGH
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled gracefully
        }
    }
}
