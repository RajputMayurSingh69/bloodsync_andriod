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

    // Method 1: Path to service account JSON
    if (FIREBASE_CONFIG.serviceAccountPath) {
      const resolvedPath = path.isAbsolute(FIREBASE_CONFIG.serviceAccountPath)
        ? FIREBASE_CONFIG.serviceAccountPath
        : path.resolve(process.cwd(), FIREBASE_CONFIG.serviceAccountPath);

      if (fs.existsSync(resolvedPath)) {
        const serviceAccount = JSON.parse(fs.readFileSync(resolvedPath, 'utf8'));
        credential = admin.credential.cert(serviceAccount);
      }
    }

    // Method 2: Explicit credentials from environment variables
    if (!credential && FIREBASE_CONFIG.clientEmail && FIREBASE_CONFIG.privateKey) {
      credential = admin.credential.cert({
        projectId: FIREBASE_CONFIG.projectId,
        clientEmail: FIREBASE_CONFIG.clientEmail,
        privateKey: FIREBASE_CONFIG.privateKey,
      });
    }

    // Method 3: Application Default Credentials (GCP / Firebase Functions / Local gcloud)
    if (!credential) {
      credential = admin.credential.applicationDefault();
    }

    admin.initializeApp({
      credential,
      projectId: FIREBASE_CONFIG.projectId,
    });

    isInitialized = true;
    console.log(`[Firebase] Firebase Admin SDK initialized for project: ${FIREBASE_CONFIG.projectId}`);
  } catch (error) {
    console.error(`[Firebase] Initialization Warning: ${error.message}`);
    // If running in development without credentials, provide dummy or local mock fallback
    if (!isInitialized && admin.apps.length === 0) {
      try {
        admin.initializeApp({
          projectId: FIREBASE_CONFIG.projectId || 'bloodsync-3b5cf',
        });
        isInitialized = true;
        console.warn('[Firebase] Initialized with default project fallback.');
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
