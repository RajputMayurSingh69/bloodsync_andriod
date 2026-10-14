package com.bloodsync.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bloodsync.R

/**
 * Singleton SirenPlayer responsible for playing loud, looping emergency sirens
 * when an Emergency Blood Request is dispatched to the Blood Bank Portal.
 *
 * Characteristics:
 * - Uses AudioAttributes with USAGE_ALARM to ensure it rings audibly even if the
 *   ringer is muted/vibrated (wherever Android permits alarm stream audio).
 * - Requests transient audio focus so other audio streams duck or pause.
 * - Looping = true, playing continuously until explicitly stopped.
 * - Safety Cap: Automatically stops after 15 seconds if not acknowledged.
 * - Strict singleton guarantee: Only ONE instance can ever play at a time.
 */
object SirenPlayer {

    private const val TAG = "SirenPlayer"
    private const val MAX_PLAY_DURATION_MS = 15_000L // 15-second safety cutoff

    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isPlaying = false

    private val safetyStopRunnable = Runnable {
        Log.d(TAG, "Safety cap triggered (15s elapsed). Stopping siren.")
        stopSirenInternal()
    }

    /**
     * Start playing the emergency siren with USAGE_ALARM in a loop.
     * Guaranteed thread-safe; won't double-start if already playing.
     */
    @Synchronized
    fun playSiren(context: Context) {
        if (isPlaying) {
            Log.d(TAG, "Siren is already playing. Resetting safety timer.")
            mainHandler.removeCallbacks(safetyStopRunnable)
            mainHandler.postDelayed(safetyStopRunnable, MAX_PLAY_DURATION_MS)
            return
        }

        try {
            val ctx = context.applicationContext
            audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

            // Request Audio Focus with ALARM stream attributes
            requestAudioFocus()

            // Prepare AudioAttributes for emergency alarms
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Create MediaPlayer with local res/raw/emergency_siren asset
            val player = MediaPlayer.create(ctx, R.raw.emergency_siren) ?: run {
                Log.e(TAG, "Failed to create MediaPlayer for R.raw.emergency_siren")
                return
            }

            player.setAudioAttributes(audioAttributes)
            player.isLooping = true
            player.setVolume(1.0f, 1.0f)

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error occurred: what=$what, extra=$extra")
                stopSirenInternal()
                true
            }

            player.start()
            mediaPlayer = player
            isPlaying = true
            Log.i(TAG, "🚨 Emergency siren started playing (USAGE_ALARM, looping).")

            // Arm safety cutoff timer
            mainHandler.removeCallbacks(safetyStopRunnable)
            mainHandler.postDelayed(safetyStopRunnable, MAX_PLAY_DURATION_MS)

        } catch (e: Exception) {
            Log.e(TAG, "Error while starting siren: ${e.message}", e)
            stopSirenInternal()
        }
    }

    /**
     * Stop the siren immediately and release all media and audio focus resources.
     */
    @Synchronized
    fun stopSiren(context: Context? = null) {
        Log.d(TAG, "Explicit siren stop requested.")
        mainHandler.removeCallbacks(safetyStopRunnable)
        stopSirenInternal()
    }

    /**
     * Check if the siren is currently playing.
     */
    fun isSirenPlaying(): Boolean = isPlaying

    private fun stopSirenInternal() {
        try {
            mainHandler.removeCallbacks(safetyStopRunnable)

            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
            isPlaying = false

            abandonAudioFocus()
            Log.d(TAG, "Siren stopped and resources released cleanly.")
        } catch (e: Exception) {
            Log.w(TAG, "Exception during siren teardown: ${e.message}")
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }

    private fun requestAudioFocus() {
        try {
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "Audio focus changed: $focusChange")
                    }
                    .build()

                focusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not request audio focus: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        try {
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { req ->
                    am.abandonAudioFocusRequest(req)
                }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not abandon audio focus: ${e.message}")
        }
    }
}
