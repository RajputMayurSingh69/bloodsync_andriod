package com.bloodsync.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import com.google.cloud.firestore.Firestore
import io.ktor.server.application.*
import java.io.FileInputStream

/**
 * BloodSync Backend - Firebase Admin SDK Initializer (Kotlin)
 *
 * Mirrors firebase.js: initialises the Firebase Admin SDK using the service account
 * JSON file path from environment variables and exposes db (Firestore) and auth handles.
 */
object FirebaseConfig {
    var firestore: Firestore? = null
        private set
    var auth: FirebaseAuth? = null
        private set

    fun initialize() {
        if (FirebaseApp.getApps().isNotEmpty()) return // Already initialized

        try {
            val serviceAccountPath = AppConfig.firebaseServiceAccountPath
            val options = if (serviceAccountPath.isNotBlank()) {
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(FileInputStream(serviceAccountPath)))
                    .setDatabaseUrl(AppConfig.firebaseDatabaseUrl.ifBlank { null })
                    .build()
            } else {
                // Fallback: Application Default Credentials (e.g., GCP / Cloud Run)
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build()
            }

            FirebaseApp.initializeApp(options)
            firestore = FirestoreClient.getFirestore()
            auth = FirebaseAuth.getInstance()
            println("[Firebase] Admin SDK initialized successfully.")
        } catch (e: Exception) {
            println("[Firebase] WARNING: Could not initialize Admin SDK: ${e.message}")
            println("[Firebase] Firestore and FCM features will be unavailable.")
        }
    }
}

/** Ktor plugin to initialize Firebase on server startup */
fun Application.configureFirebase() {
    FirebaseConfig.initialize()
}
