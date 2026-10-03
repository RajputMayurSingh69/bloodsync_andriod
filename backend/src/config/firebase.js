/**
 * BloodSync Backend - Firebase Admin SDK Initialization
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const { FIREBASE_CONFIG } = require('./environment');

let isInitialized = false;

function initializeFirebase() {
  if (isInitialized || admin.apps.length > 0) {
    return admin;
  }

  try {
    let credential;

    // Method 1: Direct JSON content in environment variable (FIREBASE_SERVICE_ACCOUNT_JSON)
    if (FIREBASE_CONFIG.serviceAccountJson) {
      try {
        let jsonStr = String(FIREBASE_CONFIG.serviceAccountJson).trim();
        // Check if base64 encoded
        if (!jsonStr.startsWith('{')) {
          jsonStr = Buffer.from(jsonStr, 'base64').toString('utf8');
        }
        const serviceAccount = JSON.parse(jsonStr);
        credential = admin.credential.cert(serviceAccount);
        console.log('[Firebase] Successfully initialized with FIREBASE_SERVICE_ACCOUNT_JSON environment variable.');
      } catch (jsonErr) {
        console.warn(`[Firebase] Failed to parse FIREBASE_SERVICE_ACCOUNT_JSON: ${jsonErr.message}`);
      }
    }

    // Method 2: Path to service account JSON
    if (!credential && FIREBASE_CONFIG.serviceAccountPath) {
      const candidatePaths = [
        FIREBASE_CONFIG.serviceAccountPath,
        path.isAbsolute(FIREBASE_CONFIG.serviceAccountPath) ? FIREBASE_CONFIG.serviceAccountPath : null,
        path.resolve(process.cwd(), FIREBASE_CONFIG.serviceAccountPath),
        path.resolve(__dirname, '../../config/firebase-service-account.json'),
        path.resolve(__dirname, './firebase-service-account.json'),
      ].filter(Boolean);

      for (const p of candidatePaths) {
        if (fs.existsSync(p)) {
          try {
            const serviceAccount = JSON.parse(fs.readFileSync(p, 'utf8'));
            credential = admin.credential.cert(serviceAccount);
            console.log(`[Firebase] Successfully initialized with service account file: ${p}`);
            break;
          } catch (fileErr) {
            console.warn(`[Firebase] Could not read service account from ${p}: ${fileErr.message}`);
          }
        }
      }
    }

    // Method 3: Explicit credentials from environment variables
    if (!credential && FIREBASE_CONFIG.clientEmail && FIREBASE_CONFIG.privateKey) {
      credential = admin.credential.cert({
        projectId: FIREBASE_CONFIG.projectId,
        clientEmail: FIREBASE_CONFIG.clientEmail,
        privateKey: FIREBASE_CONFIG.privateKey,
      });
      console.log('[Firebase] Successfully initialized with FIREBASE_CLIENT_EMAIL and FIREBASE_PRIVATE_KEY.');
    }

    // Method 4: Application Default Credentials (GCP / Cloud Run / Firebase Functions)
    if (!credential) {
      try {
        credential = admin.credential.applicationDefault();
      } catch (_adcErr) {
        // Fallback below
      }
    }

    if (credential) {
      admin.initializeApp({
        credential,
        projectId: FIREBASE_CONFIG.projectId,
      });
      isInitialized = true;
      console.log(`[Firebase] Firebase Admin SDK active for project: ${FIREBASE_CONFIG.projectId}`);
    } else {
      admin.initializeApp({
        projectId: FIREBASE_CONFIG.projectId || 'bloodsync-3b5cf',
      });
      isInitialized = true;
      console.warn('[Firebase] Initialized in default fallback mode without private service account credentials.');

      try {
        const webhookAlertService = require('../services/webhookAlertService');
        webhookAlertService.triggerAlert({
          severity: 'WARNING',
          errorCategory: 'FIREBASE_FALLBACK_MODE',
          title: 'Firebase Service Account Credentials Missing',
          message: 'Firebase Admin SDK initialized in fallback mode without service account credentials. Elevated admin operations may fail.',
          requestId: 'boot_firebase_fallback',
        });
      } catch (_hookErr) {}
    }
  } catch (error) {
    console.error(`[Firebase] Initialization Warning: ${error.message}`);
    try {
      const webhookAlertService = require('../services/webhookAlertService');
      webhookAlertService.triggerAlert({
        severity: 'CRITICAL',
        errorCategory: 'FIREBASE_CREDENTIALS_ERROR',
        title: 'Firebase Admin SDK Initialization Error',
        message: `Failed to initialize Firebase Admin SDK: ${error.message}. Check service account credentials.`,
        requestId: 'boot_firebase_init',
      });
    } catch (_hookErr) {}

    if (!isInitialized && admin.apps.length === 0) {
      try {
        admin.initializeApp({
          projectId: FIREBASE_CONFIG.projectId || 'bloodsync-3b5cf',
        });
        isInitialized = true;
      } catch (innerError) {
        console.error('[Firebase] Fatal initialization failure:', innerError.message);
      }
    }
  }

  return admin;
}

const firebaseApp = initializeFirebase();
const db = firebaseApp.firestore ? firebaseApp.firestore() : null;
const auth = firebaseApp.auth ? firebaseApp.auth() : null;

module.exports = {
  admin,
  db,
  auth,
  initializeFirebase,
};
