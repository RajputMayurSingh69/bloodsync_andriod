package com.bloodsync.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bloodsync.MainActivity
import com.bloodsync.R
import com.bloodsync.data.notification.NotificationHelper
import com.bloodsync.util.SirenPlayer

/**
 * Short-lived Foreground Service designed to keep the emergency siren playing
 * and maintain a persistent high-priority notification when the app is in the
 * background or the device screen is locked.
 *
 * Automatically stops itself after 15 seconds (safety cap) or when the user
 * taps "View Emergency Request" or "Dismiss Siren".
 */
class EmergencySirenService : Service() {

    companion object {
        private const val TAG = "EmergencySirenService"
        const val ACTION_START_SIREN = "com.bloodsync.action.START_EMERGENCY_SIREN"
        const val ACTION_STOP_SIREN = "com.bloodsync.action.STOP_EMERGENCY_SIREN"
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_PATIENT_NAME = "extra_patient_name"
        const val EXTRA_BLOOD_GROUP = "extra_blood_group"
        const val EXTRA_HOSPITAL = "extra_hospital"
        const val EXTRA_UNITS = "extra_units"

        private const val NOTIFICATION_ID = 9911

        fun start(
            context: Context,
            requestId: String,
            patientName: String = "",
            bloodGroup: String = "",
            hospital: String = "",
            units: Int = 1
        ) {
            val intent = Intent(context, EmergencySirenService::class.java).apply {
                action = ACTION_START_SIREN
                putExtra(EXTRA_REQUEST_ID, requestId)
                putExtra(EXTRA_PATIENT_NAME, patientName)
                putExtra(EXTRA_BLOOD_GROUP, bloodGroup)
                putExtra(EXTRA_HOSPITAL, hospital)
                putExtra(EXTRA_UNITS, units)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start EmergencySirenService: ${e.message}", e)
                // Fallback to in-process playback
                SirenPlayer.playSiren(context)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, EmergencySirenService::class.java).apply {
                action = ACTION_STOP_SIREN
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop EmergencySirenService via intent: ${e.message}")
                SirenPlayer.stopSiren(context)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        Log.d(TAG, "Auto-stop runnable triggered (15s elapsed).")
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SIREN

        if (action == ACTION_STOP_SIREN) {
            Log.d(TAG, "Received ACTION_STOP_SIREN. Stopping service.")
            stopSirenAndFinish()
            return START_NOT_STICKY
        }

        val requestId = intent?.getStringExtra(EXTRA_REQUEST_ID) ?: ""
        val patientName = intent?.getStringExtra(EXTRA_PATIENT_NAME) ?: "Emergency Patient"
        val bloodGroup = intent?.getStringExtra(EXTRA_BLOOD_GROUP) ?: "O+"
        val hospital = intent?.getStringExtra(EXTRA_HOSPITAL) ?: "Hospital"
        val units = intent?.getIntExtra(EXTRA_UNITS, 1) ?: 1

        Log.i(TAG, "🚨 Starting Foreground Siren for request: $requestId ($bloodGroup, $patientName)")

        // Build ongoing alert notification
        val notification = buildForegroundNotification(requestId, patientName, bloodGroup, hospital, units)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not startForeground with MEDIA_PLAYBACK type: ${e.message}. Falling back.")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start siren sound loop via SirenPlayer singleton
        SirenPlayer.playSiren(applicationContext)

        // Reset safety 15-second auto-stop
        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, 15_000L)

        return START_NOT_STICKY
    }

    private fun buildForegroundNotification(
        requestId: String,
        patientName: String,
        bloodGroup: String,
        hospital: String,
        units: Int
    ): Notification {
        NotificationHelper.createNotificationChannels(applicationContext)

        // 1. Deep-link intent for full notification tap & primary action
        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = "ACTION_VIEW_EMERGENCY"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra("requestId", requestId)
            putExtra("bloodGroup", bloodGroup)
            putExtra("patient", patientName)
            putExtra("hospital", hospital)
        }
        val openPendingIntent = PendingIntent.getActivity(
            applicationContext,
            requestId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Dismiss action intent to silence siren without opening
        val dismissIntent = Intent(applicationContext, EmergencySirenService::class.java).apply {
            action = ACTION_STOP_SIREN
        }
        val dismissPendingIntent = PendingIntent.getService(
            applicationContext,
            992,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🚨 INBOUND EMERGENCY: $bloodGroup ($units Units)"
        val message = "$patientName at $hospital requires immediate blood dispatch!"

        return NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_EMERGENCY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\nTap 'View Emergency Request' to review and acknowledge."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(openPendingIntent, true)
            .addAction(
                R.mipmap.ic_launcher,
                "View Emergency Request",
                openPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Silence Siren",
                dismissPendingIntent
            )
            .build()
    }

    private fun stopSirenAndFinish() {
        handler.removeCallbacks(autoStopRunnable)
        SirenPlayer.stopSiren(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoStopRunnable)
        SirenPlayer.stopSiren(applicationContext)
        Log.d(TAG, "EmergencySirenService destroyed.")
    }
}
