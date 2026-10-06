package com.bloodsync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A lightweight singleton that bridges the Android Activity layer (onNewIntent)
 * with the Compose layer (BloodSyncApp).
 *
 * When a user taps an emergency FCM notification while the app is already open,
 * MainActivity.onNewIntent() writes the incoming requestId here.
 * BloodSyncApp observes this as Compose State and triggers navigation.
 */
object BloodSyncDeepLinkState {
    /**
     * Pending emergency requestId from a tapped FCM notification.
     * Set by MainActivity.onNewIntent(); consumed and cleared by BloodSyncApp.
     */
    var pendingRequestId: String? by mutableStateOf(null)
}
