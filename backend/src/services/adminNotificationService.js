/**
 * BloodSync Backend - Admin Notification & New Donor Registration Dispatcher
 * 
 * Listens to new donor registrations in Cloud Firestore and dispatches
 * real-time push notifications strictly and exclusively to authorized admin users.
 * 
 * GUARANTEE:
 * The newly registered donor is NEVER included in the recipient list.
 */

'use strict';

const { admin, db } = require('../config/firebase');
const loggerBotService = require('./loggerBotService');
const { REQUEST_STATUS } = require('../config/constants');

// Cache of processed registration IDs to prevent duplicate admin dispatch
const processedDonorIds = new Set();
let isListenerActive = false;

class AdminNotificationService {

  /**
   * Dispatches an administrative "New Donor Registered" notification strictly to authorized admins.
   * 
   * @param {Object} donorData
   * @param {string} donorData.donorId - The newly registered user's UID
   * @param {string} [donorData.name]
   * @param {string} [donorData.bloodGroup]
   * @param {string} [donorData.city]
   * @param {string} [donorData.phone]
   * @param {string} [donorData.email]
   * @returns {Promise<{ success: boolean, adminsNotifiedCount: number, error?: string }>}
   */
  async onDonorRegistered(donorData) {
    if (!donorData || (!donorData.donorId && !donorData.id)) {
      return { success: false, adminsNotifiedCount: 0, error: 'Invalid donor registration payload' };
    }

    const donorId = donorData.donorId || donorData.id;
    const donorName = donorData.name || 'New Voluntary Donor';
    const bloodGroup = donorData.bloodGroup || 'Blood Group';
    const city = donorData.city || 'BloodSync Community';
    const phone = donorData.phone || '';
    const email = donorData.email || '';

    console.log(`[AdminNotificationService] Processing new donor registration: ${donorName} (${bloodGroup}, UID: ${donorId})`);

    if (!db) {
      console.warn('[AdminNotificationService] Firestore DB not available. Cannot fetch admin tokens.');
      return { success: false, adminsNotifiedCount: 0, error: 'Firestore uninitialized' };
    }

    try {
      // 1. Fetch authorized administrator profiles from Firestore
      const adminUsersSnap = await db.collection('users')
        .where('role', '==', 'admin')
        .get();

      // Collect admin FCM tokens; STRICT EXCLUSION: Ensure new user is NEVER an admin recipient
      const adminTokens = [];
      const adminUids = [];

      adminUsersSnap.forEach((doc) => {
        const adminId = doc.id;
        const data = doc.data();
        const fcmToken = data.fcmToken;

        // CRITICAL SECURITY & LOGIC GUARD:
        // Exclude the newly registered donor even if they have an admin claim
        if (adminId !== donorId && fcmToken) {
          adminTokens.push(fcmToken);
          adminUids.push(adminId);
        }
      });

      console.log(`[AdminNotificationService] Found ${adminTokens.length} active admin FCM tokens (excluded registering user: ${donorId}).`);

      const notificationTitle = '🚨 New Donor Registered!';
      const notificationBody = `${donorName} (${bloodGroup}) registered from ${city}.`;

      // 2. Persist notification to /admin_notifications for Admin Dashboard
      const adminNotifDoc = {
        type: 'NEW_DONOR_REGISTERED',
        title: notificationTitle,
        body: notificationBody,
        donorId,
        donorName,
        bloodGroup,
        city,
        phone,
        email,
        recipientRole: 'admin',
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      await db.collection('admin_notifications').add(adminNotifDoc);

      // Also create unread notification in each admin user's private notification subcollection
      for (const adminUid of adminUids) {
        try {
          await db.collection('users').doc(adminUid)
            .collection('notifications').add({
              type: 'NEW_DONOR_REGISTERED',
              title: notificationTitle,
              body: notificationBody,
              donorId,
              isRead: false,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        } catch (e) {
          console.warn(`[AdminNotificationService] Failed to write notification to admin ${adminUid}:`, e.message);
        }
      }

      // 3. Dispatch FCM Push to Admin Device Tokens (if any registered)
      let deliveredCount = 0;
      const messaging = admin.messaging ? admin.messaging() : null;

      if (messaging && adminTokens.length > 0) {
        const messagePayload = {
          tokens: adminTokens,
          notification: {
            title: notificationTitle,
            body: notificationBody,
          },
          data: {
            type: 'NEW_DONOR_REGISTERED',
            donorId: String(donorId),
            donorName: String(donorName),
            bloodGroup: String(bloodGroup),
            city: String(city),
            registeredAt: String(Date.now()),
          },
          android: {
            priority: 'high',
            notification: {
              channelId: 'channel_emergency_alerts',
              sound: 'default',
              priority: 'high',
              color: '#C52828',
            },
          },
        };

        const fcmResponse = await messaging.sendEachForMulticast(messagePayload);
        deliveredCount = fcmResponse.successCount;
        console.log(`[AdminNotificationService] Multicast to admins result: ${deliveredCount} delivered, ${fcmResponse.failureCount} failed.`);
      }

      // 4. Mark donor_registrations record as admin-notified
      await db.collection('donor_registrations').doc(donorId).set({
        adminNotified: true,
        adminNotifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        adminsNotifiedCount: deliveredCount,
      }, { merge: true });

      // 5. Audit Log
      await loggerBotService.logOtpEvent({
        requestId: `new_donor_${donorId}_${Date.now()}`,
        rawEmail: email || 'admin-notifier@bloodsync.org',
        requestStatus: REQUEST_STATUS.SUCCESS,
        otpGenStatus: 'SKIPPED',
        emailSendingStatus: 'SKIPPED',
        errorCategory: null,
        errorMessage: null,
        metadata: {
          action: 'ADMIN_NEW_DONOR_NOTIFIED',
          donorId,
          bloodGroup,
          adminsTargeted: adminTokens.length,
          deliveredCount,
        },
      });

      return {
        success: true,
        adminsNotifiedCount: deliveredCount,
      };
    } catch (err) {
      console.error(`[AdminNotificationService] Error notifying admins of donor ${donorId}:`, err.message);
      return {
        success: false,
        adminsNotifiedCount: 0,
        error: err.message,
      };
    }
  }

  /**
   * Initializes a real-time Firestore listener on `donor_registrations`
   * to automatically trigger admin notifications when a new user registers.
   */
  startListener() {
    if (isListenerActive || !db) {
      return;
    }

    try {
      console.log('[AdminNotificationService] Starting real-time Firestore listener on donor_registrations...');
      isListenerActive = true;

      db.collection('donor_registrations').onSnapshot((snapshot) => {
        snapshot.docChanges().forEach(async (change) => {
          if (change.type === 'added') {
            const data = change.doc.data();
            const id = change.doc.id;

            // If already processed in-memory or in database, skip
            if (data.adminNotified === true || processedDonorIds.has(id)) {
              return;
            }

            processedDonorIds.add(id);
            await this.onDonorRegistered({ id, ...data });
          }
        });
      }, (err) => {
        console.warn(`[AdminNotificationService] Firestore listener notice: ${err.message}`);
      });
    } catch (err) {
      console.warn(`[AdminNotificationService] Could not start listener: ${err.message}`);
    }
  }
}

module.exports = new AdminNotificationService();
