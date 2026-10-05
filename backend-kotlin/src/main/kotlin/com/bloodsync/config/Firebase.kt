package com.bloodsync.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64

/**
 * BloodSync Backend - Firebase Admin SDK Initialization
 * Converted from: backend/src/config/firebase.js
 */

private val logger = KotlinLogging.logger {}

object FirebaseAdmin {

    var db: Firestore? = null
        private set

    var auth: FirebaseAuth? = null
        private set

    private var initialized = false

    fun initialize() {
        if (initialized || FirebaseApp.getApps().isNotEmpty()) {
            db   = if (FirebaseApp.getApps().isNotEmpty()) FirestoreClient.getFirestore() else null
            auth = if (FirebaseApp.getApps().isNotEmpty()) FirebaseAuth.getInstance() else null
            return
        }

        try {
            var credentials: GoogleCredentials? = null

            // Method 1: JSON content / base64 in FIREBASE_SERVICE_ACCOUNT_JSON env var
            if (FIREBASE_CONFIG.serviceAccountJson.isNotBlank()) {
                try {
                    var jsonStr = FIREBASE_CONFIG.serviceAccountJson.trim()
                    // If base64 encoded (does not start with '{')
                    if (!jsonStr.startsWith("{")) {
                        jsonStr = String(Base64.getDecoder().decode(jsonStr))
                    }
                    credentials = GoogleCredentials.fromStream(
                        ByteArrayInputStream(jsonStr.toByteArray(Charsets.UTF_8))
                    )
                    logger.info { "[Firebase] Initialized with FIREBASE_SERVICE_ACCOUNT_JSON env variable." }
                } catch (e: Exception) {
                    logger.warn { "[Firebase] Failed to parse FIREBASE_SERVICE_ACCOUNT_JSON: ${e.message}" }
                }
            }

            // Method 2: Service account file path
            if (credentials == null && FIREBASE_CONFIG.serviceAccountPath.isNotBlank()) {
                val candidatePaths = listOf(
                    FIREBASE_CONFIG.serviceAccountPath,
                    "${System.getProperty("user.dir")}/${FIREBASE_CONFIG.serviceAccountPath}",
                    "${System.getProperty("user.dir")}/config/firebase-service-account.json",
                )
                for (path in candidatePaths) {
                    val file = File(path)
                    if (file.exists()) {
                        try {
                            credentials = GoogleCredentials.fromStream(file.inputStream())
                            logger.info { "[Firebase] Initialized with service account file: $path" }
                            break
                        } catch (e: Exception) {
                            logger.warn { "[Firebase] Could not read service account from $path: ${e.message}" }
                        }
                    }
                }
            }

            // Method 3: Individual env variable credentials
            if (credentials == null
                && FIREBASE_CONFIG.clientEmail.isNotBlank()
                && FIREBASE_CONFIG.privateKey.isNotBlank()
            ) {
                val saJson = """
                    {
                        "type": "service_account",
                        "project_id": "${FIREBASE_CONFIG.projectId}",
                        "client_email": "${FIREBASE_CONFIG.clientEmail}",
                        "private_key": "${FIREBASE_CONFIG.privateKey.replace("\n", "\\n")}"
                    }
                """.trimIndent()
                credentials = GoogleCredentials.fromStream(
                    ByteArrayInputStream(saJson.toByteArray(Charsets.UTF_8))
                )
                logger.info { "[Firebase] Initialized with FIREBASE_CLIENT_EMAIL and FIREBASE_PRIVATE_KEY." }
            }

            // Method 4: Application Default Credentials (GCP / Cloud Run)
            if (credentials == null) {
                try {
                    credentials = GoogleCredentials.getApplicationDefault()
                    logger.info { "[Firebase] Initialized with Application Default Credentials." }
                } catch (_: Exception) {
                    // Fallback
                }
            }

            val options = if (credentials != null) {
                FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(FIREBASE_CONFIG.projectId)
                    .build()
            } else {
                logger.warn { "[Firebase] No credentials found. Initializing in fallback mode without private service account." }
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.newBuilder().build())
                    .setProjectId(FIREBASE_CONFIG.projectId.ifBlank { "bloodsync-3b5cf" })
                    .build()
            }

            FirebaseApp.initializeApp(options)
            db   = FirestoreClient.getFirestore()
            auth = FirebaseAuth.getInstance()
            initialized = true
            logger.info { "[Firebase] Firebase Admin SDK active for project: ${FIREBASE_CONFIG.projectId}" }
        } catch (error: Exception) {
            logger.error { "[Firebase] Initialization Warning: ${error.message}" }
            try {
                if (FirebaseApp.getApps().isEmpty()) {
                    val fallbackOptions = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.newBuilder().build())
                        .setProjectId(FIREBASE_CONFIG.projectId.ifBlank { "bloodsync-3b5cf" })
                        .build()
                    FirebaseApp.initializeApp(fallbackOptions)
                }
                db   = FirestoreClient.getFirestore()
                auth = FirebaseAuth.getInstance()
                initialized = true
            } catch (inner: Exception) {
                logger.error { "[Firebase] Fatal initialization failure: ${inner.message}" }
            }
        }
    }
}
