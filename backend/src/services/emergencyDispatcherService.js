/**
 * BloodSync Backend - Automated Emergency SOS Push Notification Dispatcher
 * 
 * Listens to new emergency blood requests in Cloud Firestore and dispatches
 * high-priority FCM remote push notifications with sound to matching blood group donors.
 */

const { admin, db } = require('../config/firebase');
const loggerBotService = require('./loggerBotService');
const { REQUEST_STATUS } = require('../config/constants');

// ABO/Rh Compatibility Chart: Key is Needed Blood Group, Value is array of eligible donor groups
const DONOR_COMPATIBILITY = {
  'O-': ['O-'],
  'O+': ['O+', 'O-'],
  'A-': ['A-', 'O-'],
  'A+': ['A+', 'A-', 'O+', 'O-'],
  'B-': ['B-', 'O-'],
  'B+': ['B+', 'B-', 'O+', 'O-'],
  'AB-': ['AB-', 'A-', 'B-', 'O-'],
  'AB+': ['AB+', 'AB-', 'A+', 'A-', 'B+', 'B-', 'O+', 'O-'],
};

// Set of processed request IDs to prevent duplicate push broadcasts
const processedEmergencyIds = new Set();
let isDispatcherRunning = false;

/**
 * Calculates the great-circle distance between two geographic coordinates using the Haversine formula.
 * @param {number} lat1 Latitude of point 1 in degrees
 * @param {number} lon1 Longitude of point 1 in degrees
 * @param {number} lat2 Latitude of point 2 in degrees
 * @param {number} lon2 Longitude of point 2 in degrees
 * @returns {number} Distance in kilometers
 */
function calculateHaversineDistanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth's mean radius in kilometers
  const toRad = (angle) => (Number(angle) * Math.PI) / 180;

  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

class EmergencyDispatcherService {

  /**
   * Returns list of eligible donor blood groups for the requested blood type.
   */
  getEligibleDonorGroups(neededGroup) {
    const clean = String(neededGroup).trim().toUpperCase();
    return DONOR_COMPATIBILITY[clean] || [clean];
  }

  /**
   * Dispatches push notifications to compatible donors for a specific emergency request within 10km radar.
   * @param {Object} emergency - Emergency request document data
   * @returns {Promise<{ success: boolean, notifiedCount: number, error?: string }>}
   */
  async onEmergencyCreated(emergency) {
    if (!emergency || !emergency.id) {
      return { success: false, notifiedCount: 0, error: 'Invalid emergency payload' };
    }

    const requestId = emergency.id;
    const bloodGroupNeeded = emergency.bloodGroupNeeded || 'O+';
    const patientName = emergency.patientName || 'Emergency Patient';
    const hospitalName = emergency.hospitalName || 'Hospital';
    const unitsRequired = emergency.unitsRequired || 1;
    const urgency = emergency.urgencyLevel || 'IMMEDIATE';
    const requesterId = emergency.userId || '';

    // Parse emergency GPS coordinates for 10km Radar Proximity Filtering
    const emgLat = typeof emergency.latitude === 'number' ? emergency.latitude : parseFloat(emergency.latitude);
    const emgLon = typeof emergency.longitude === 'number' ? emergency.longitude : parseFloat(emergency.longitude);
    const hasEmergencyCoordinates = !isNaN(emgLat) && !isNaN(emgLon);
    const MAX_RADAR_RADIUS_KM = 10.0;

    console.log(`[EmergencyDispatcher] Processing SOS ${requestId} for ${unitsRequired} units of ${bloodGroupNeeded} at ${hospitalName}${hasEmergencyCoordinates ? ` (GPS: ${emgLat.toFixed(4)}, ${emgLon.toFixed(4)})` : ''}`);

    if (!db) {
      console.warn('[EmergencyDispatcher] Firestore DB not available. Cannot fetch donor tokens.');
      return { success: false, notifiedCount: 0, error: 'Firestore uninitialized' };
    }

    const messaging = admin.messaging ? admin.messaging() : null;
    if (!messaging) {
      console.warn('[EmergencyDispatcher] Firebase Messaging is not available (check Admin SDK credentials).');
      return { success: false, notifiedCount: 0, error: 'FCM uninitialized' };
    }

    try {
      const eligibleGroups = this.getEligibleDonorGroups(bloodGroupNeeded);

      // Map to store unique FCM token -> donor metadata (distance, bloodGroup, name)
      const tokenMap = new Map();

      // Proximity evaluator helper
      const evaluateProximity = (data) => {
        if (!hasEmergencyCoordinates) {
          return { isEligible: true, distanceKm: null };
        }
        const donorLat = typeof data.latitude === 'number' ? data.latitude : parseFloat(data.latitude);
        const donorLon = typeof data.longitude === 'number' ? data.longitude : parseFloat(data.longitude);

        if (!isNaN(donorLat) && !isNaN(donorLon)) {
          const distKm = calculateHaversineDistanceKm(emgLat, emgLon, donorLat, donorLon);
          return {
            isEligible: distKm <= MAX_RADAR_RADIUS_KM,
            distanceKm: distKm,
          };
        }

        // If donor has not yet set GPS coordinates, fallback to true so legacy registered donors are not excluded
        return { isEligible: true, distanceKm: null };
      };

      // 1. Query users collection for compatible donors
      const usersSnap = await db.collection('users')
        .where('isAvailableDonor', '==', true)
        .get();

      usersSnap.forEach((doc) => {
        const data = doc.data();
        const donorId = doc.id;
        const donorGroup = data.bloodGroup;
        const token = data.fcmToken;

        if (token && eligibleGroups.includes(donorGroup) && donorId !== requesterId) {
          const proximity = evaluateProximity(data);
          if (proximity.isEligible) {
            tokenMap.set(token, {
              donorId,
              donorGroup,
              distanceKm: proximity.distanceKm,
            });
          }
        }
      });

      // 2. Query donors collection for registered community donors (available only)
      const donorsSnap = await db.collection('donors')
        .where('isAvailableDonor', '==', true)
        .get();
      donorsSnap.forEach((doc) => {
        const data = doc.data();
        const donorId = doc.id;
        const donorGroup = data.bloodGroup;
        const token = data.fcmToken;

        if (token && eligibleGroups.includes(donorGroup) && donorId !== requesterId) {
          const proximity = evaluateProximity(data);
          if (proximity.isEligible) {
            tokenMap.set(token, {
              donorId,
              donorGroup,
              distanceKm: proximity.distanceKm,
            });
          }
        }
      });

      const tokenList = Array.from(tokenMap.keys());

      if (tokenList.length === 0) {
        console.log(`[EmergencyDispatcher] No matching donors with active FCM tokens found within 10km radar for ${bloodGroupNeeded}.`);
        return { success: true, notifiedCount: 0 };
      }

      console.log(`[EmergencyDispatcher] Found ${tokenList.length} matching donors within 10km radar. Dispatching push alert...`);

      // Construct high-priority push message payload with sound & vibration
      const messagePayload = {
        tokens: tokenList,
        notification: {
          title: `🚨 CRITICAL: ${bloodGroupNeeded} Blood Urgently Needed!`,
          body: hasEmergencyCoordinates
            ? `${patientName} needs ${unitsRequired} unit(s) at ${hospitalName} (Within 10km Radar). Tap to respond now!`
            : `${patientName} needs ${unitsRequired} unit(s) at ${hospitalName}. Tap to respond now!`,
        },
        data: {
          type: 'EMERGENCY',
          requestId: requestId,
          bloodGroup: bloodGroupNeeded,
          hospital: hospitalName,
          patient: patientName,
          units: String(unitsRequired),
          urgency: String(urgency),
          emergencyLat: hasEmergencyCoordinates ? String(emgLat) : '',
          emergencyLon: hasEmergencyCoordinates ? String(emgLon) : '',
          radarRadiusKm: '10.0',
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'channel_emergency_alerts',
            sound: 'default',
            priority: 'high',
            color: '#D32F2F',
            defaultVibrateTimings: true,
          },
        },
      };

      // Dispatch multicast push notifications
      const response = await messaging.sendEachForMulticast(messagePayload);
      console.log(`[EmergencyDispatcher] FCM Multicast Result: ${response.successCount} delivered, ${response.failureCount} failed.`);

      // Update donorsNotifiedCount in the emergency document
      await db.collection('emergency_requests').doc(requestId).set({
        donorsNotifiedCount: response.successCount,
        radarRadiusKm: 10.0,
        lastPushDispatchedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });

      // Record in logger bot
      await loggerBotService.logOtpEvent({
        requestId: requestId,
        rawEmail: 'emergency-dispatcher@bloodsync.org',
        requestStatus: REQUEST_STATUS.SUCCESS,
        otpGenStatus: 'SKIPPED',
        emailSendingStatus: 'SKIPPED',
        errorCategory: null,
        errorMessage: null,
        metadata: {
          action: 'FCM_EMERGENCY_DISPATCH',
          bloodGroupNeeded,
          hasEmergencyCoordinates,
          tokensCount: tokenList.length,
          deliveredCount: response.successCount,
          failedCount: response.failureCount,
        },
      });

      return {
        success: true,
        notifiedCount: response.successCount,
      };
    } catch (error) {
      console.error(`[EmergencyDispatcher] Error dispatching push notification for ${requestId}:`, error.message);
      return {
        success: false,
        notifiedCount: 0,
        error: error.message,
      };
    }
  }

  /**
   * Initializes real-time Firestore listener on emergency_requests to trigger onEmergencyCreated.
   */
  startListener() {
    if (isDispatcherRunning || !db) {
      return;
    }

    try {
      console.log('[EmergencyDispatcher] Starting real-time Firestore listener for emergency_requests...');
      isDispatcherRunning = true;

      db.collection('emergency_requests')
        .where('status', '==', 'BROADCASTING')
        .onSnapshot((snapshot) => {
          snapshot.docChanges().forEach(async (change) => {
            if (change.type === 'added') {
              const docData = change.doc.data();
              const id = change.doc.id;

              // Prevent processing dummy or stale seeded documents
              if (
                docData.patientName?.includes('Jane Doe') ||
                docData.hospitalName?.includes('Metro General') ||
                id.startsWith('emg_dummy')
              ) {
                return;
              }

              if (!processedEmergencyIds.has(id)) {
                processedEmergencyIds.add(id);
                // Dispatch push notifications to matching donors
                await this.onEmergencyCreated({ id, ...docData });
              }
            }
          });
        }, (error) => {
          console.warn(`[EmergencyDispatcher] Firestore listener warning: ${error.message}`);
        });
    } catch (err) {
      console.warn(`[EmergencyDispatcher] Could not start listener: ${err.message}`);
    }
  }
}

module.exports = new EmergencyDispatcherService();
