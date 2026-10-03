package com.bloodsync.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.math.*

/**
 * Utility for GPS coordinate acquisition using FusedLocationProviderClient
 * and Haversine distance proximity calculation for 10km radar.
 */
object LocationHelper {

    /**
     * Checks if either fine or coarse location permission is granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    /**
     * Acquires current high-accuracy GPS coordinates using FusedLocationProviderClient.
     * Falls back to lastKnownLocation if an immediate active fix is not available.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onLocation: (latitude: Double, longitude: Double) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        if (!hasLocationPermission(context)) {
            onError?.invoke("Location permission not granted")
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        onLocation(loc.latitude, loc.longitude)
                    } else {
                        // Fallback to last known location
                        fusedClient.lastLocation
                            .addOnSuccessListener { lastLoc: Location? ->
                                if (lastLoc != null) {
                                    onLocation(lastLoc.latitude, lastLoc.longitude)
                                } else {
                                    onError?.invoke("Unable to acquire location fix")
                                }
                            }
                            .addOnFailureListener { e ->
                                onError?.invoke(e.localizedMessage ?: "Failed to get last known location")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Fallback to last known location on current location failure
                    fusedClient.lastLocation
                        .addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                onLocation(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                onError?.invoke(e.localizedMessage ?: "Location request failed")
                            }
                        }
                        .addOnFailureListener {
                            onError?.invoke(e.localizedMessage ?: "Location request failed")
                        }
                }
        } catch (e: Exception) {
            onError?.invoke(e.localizedMessage ?: "Error initializing location provider")
        }
    }

    /**
     * Calculates the great-circle distance between two geographic coordinates using the Haversine formula.
     * @return Distance in kilometers
     */
    fun calculateHaversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371.0 // Earth's mean radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Checks if two coordinate pairs are within a given radius (e.g. 10.0 km).
     */
    fun isWithinRadius(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
        radiusKm: Double = 10.0
    ): Boolean {
        return calculateHaversineDistanceKm(lat1, lon1, lat2, lon2) <= radiusKm
    }
}
