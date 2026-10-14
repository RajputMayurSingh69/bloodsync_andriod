package com.bloodsync.data.emergency

import android.content.Context
import android.util.Log
import com.bloodsync.data.model.EmergencyRequest
import com.bloodsync.data.model.EmergencyStatus
import com.bloodsync.service.EmergencySirenService
import com.bloodsync.util.SirenPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central State Holder and Alert Coordinator for the Blood Bank Portal.
 *
 * Responsibilities:
 * - Tracks unseen / unacknowledged emergency requests.
 * - Coordinates the Root Emergency Popup across all Blood Bank screens.
 * - Manages the Emergency Siren lifecycle (SirenPlayer in foreground, EmergencySirenService in background).
 * - Coordinates deep linking and target request highlighting (red glow animation).
 * - Aggregates multiple simultaneous requests into a single unified alert with a count.
 */
object EmergencyAlertManager {

    private const val TAG = "EmergencyAlertManager"
    private const val PREFS_NAME = "bloodsync_emergency_seen_prefs"
    private const val KEY_SEEN_IDS = "seen_request_ids"

    private val _isAlertActive = MutableStateFlow(false)
    val isAlertActive: StateFlow<Boolean> = _isAlertActive.asStateFlow()

    private val _currentAlertRequest = MutableStateFlow<EmergencyRequest?>(null)
    val currentAlertRequest: StateFlow<EmergencyRequest?> = _currentAlertRequest.asStateFlow()

    private val _unseenRequests = MutableStateFlow<List<EmergencyRequest>>(emptyList())
    val unseenRequests: StateFlow<List<EmergencyRequest>> = _unseenRequests.asStateFlow()

    private val _unseenCount = MutableStateFlow(0)
    val unseenCount: StateFlow<Int> = _unseenCount.asStateFlow()

    private val _highlightedRequestId = MutableStateFlow<String?>(null)
    val highlightedRequestId: StateFlow<String?> = _highlightedRequestId.asStateFlow()

    private val seenRequestIds = mutableSetOf<String>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getStringSet(KEY_SEEN_IDS, emptySet()) ?: emptySet()
            seenRequestIds.addAll(saved)
            isInitialized = true
            Log.d(TAG, "EmergencyAlertManager initialized with ${seenRequestIds.size} acknowledged IDs.")
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing seen IDs: ${e.message}")
        }
    }

    /**
     * Called when a new emergency blood request arrives (via Firestore listener or FCM message).
     */
    fun onNewEmergencyArrived(
        context: Context,
        request: EmergencyRequest,
        isAppInForeground: Boolean
    ) {
        init(context)

        // Ignore fulfilled, cancelled, or already acknowledged requests
        if (request.status != EmergencyStatus.BROADCASTING) return
        if (seenRequestIds.contains(request.id)) {
            Log.d(TAG, "Request ${request.id} already acknowledged. Skipping alert.")
            return
        }

        val currentList = _unseenRequests.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == request.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = request
        } else {
            currentList.add(0, request)
        }

        _unseenRequests.value = currentList
        _unseenCount.value = currentList.size
        _currentAlertRequest.value = request
        _isAlertActive.value = true

        Log.i(TAG, "🚨 Triggering emergency alert for request ${request.id} (Total Unseen: ${currentList.size})")

        // Trigger Siren:
        if (isAppInForeground) {
            SirenPlayer.playSiren(context)
        } else {
            EmergencySirenService.start(
                context = context,
                requestId = request.id,
                patientName = request.patientName,
                bloodGroup = request.bloodGroupNeeded,
                hospital = request.hospitalName,
                units = request.unitsRequired
            )
        }
    }

    /**
     * Batch check incoming list from Firestore to detect any brand-new requests.
     */
    fun onEmergencyRequestsBatchArrived(
        context: Context,
        requests: List<EmergencyRequest>,
        isAppInForeground: Boolean
    ) {
        init(context)

        val activeBroadcasts = requests.filter { it.status == EmergencyStatus.BROADCASTING }
        val newUnseen = activeBroadcasts.filter { !seenRequestIds.contains(it.id) }

        if (newUnseen.isEmpty()) {
            // No new unseen requests; clear unseen if all are acknowledged
            _unseenRequests.value = emptyList()
            _unseenCount.value = 0
            return
        }

        _unseenRequests.value = newUnseen
        _unseenCount.value = newUnseen.size

        // Only fire alert if the popup is not already showing or a newer request arrived
        val latest = newUnseen.firstOrNull()
        if (latest != null && (!_isAlertActive.value || _currentAlertRequest.value?.id != latest.id)) {
            _currentAlertRequest.value = latest
            _isAlertActive.value = true

            if (isAppInForeground) {
                SirenPlayer.playSiren(context)
            } else {
                EmergencySirenService.start(
                    context = context,
                    requestId = latest.id,
                    patientName = latest.patientName,
                    bloodGroup = latest.bloodGroupNeeded,
                    hospital = latest.hospitalName,
                    units = latest.unitsRequired
                )
            }
        }
    }

    /**
     * Called when the user clicks "View Emergency Request" either on the global popup
     * or on the system heads-up notification.
     *
     * Actions:
     * 1. Stops the siren immediately (both in-process SirenPlayer and background service).
     * 2. Dismisses the global popup dialog.
     * 3. Sets the highlighted request ID to trigger auto-scroll and pulsing red glow.
     * 4. Marks the request as seen.
     */
    fun acknowledgeAndNavigate(context: Context, requestId: String?) {
        Log.i(TAG, "User acknowledged emergency alert. Navigating to request: $requestId")

        // Stop sound immediately
        SirenPlayer.stopSiren(context)
        EmergencySirenService.stop(context)

        // Dismiss popup
        _isAlertActive.value = false

        // Highlight request on screen
        if (!requestId.isNullOrBlank()) {
            _highlightedRequestId.value = requestId
            markRequestSeen(context, requestId)
        }
    }

    /**
     * Mark a specific emergency request as acknowledged/seen.
     */
    fun markRequestSeen(context: Context, requestId: String) {
        init(context)
        seenRequestIds.add(requestId)
        persistSeenIds(context)

        val currentList = _unseenRequests.value.filter { it.id != requestId }
        _unseenRequests.value = currentList
        _unseenCount.value = currentList.size

        Log.d(TAG, "Request $requestId marked as seen. Remaining unseen: ${currentList.size}")
    }

    /**
     * Stop the red glow animation once the request card has been viewed or interacted with.
     */
    fun clearGlow(requestId: String? = null) {
        if (requestId == null || _highlightedRequestId.value == requestId) {
            _highlightedRequestId.value = null
        }
    }

    /**
     * Dismiss the popup without immediately opening the request (e.g. user dismissed dialog).
     */
    fun dismissAlert(context: Context) {
        SirenPlayer.stopSiren(context)
        EmergencySirenService.stop(context)
        _isAlertActive.value = false
    }

    private fun persistSeenIds(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_SEEN_IDS, seenRequestIds).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving seen IDs to prefs: ${e.message}")
        }
    }
}
