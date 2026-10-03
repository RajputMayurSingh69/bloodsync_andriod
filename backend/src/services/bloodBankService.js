/**
 * BloodSync Backend - Blood Bank Inventory Management Service
 * 
 * Provides transactional stock increment/decrement operations
 * for donations and emergency blood dispatches.
 */

const { admin, db } = require('../config/firebase');
const loggerBotService = require('./loggerBotService');
const { REQUEST_STATUS } = require('../config/constants');

const VALID_BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

class BloodBankService {

  /**
   * Safely updates blood inventory units in a blood bank using a Firestore transaction.
   * Prevents negative stock balances and logs an immutable audit trail.
   * 
   * @param {string} bankId Blood bank document ID
   * @param {Object} payload
   * @param {string} payload.bloodGroup e.g. 'O+', 'A+'
   * @param {number} payload.deltaUnits positive integer to add, negative integer to deduct
   * @param {string} payload.operation 'DONATION_RECEIVED' | 'EMERGENCY_DISPATCH' | 'MANUAL_ADJUSTMENT'
   * @param {string} [payload.reason] Contextual explanation (e.g. SOS request ID, Donor ID)
   * @param {string} [payload.performedBy] Operator/User identifier
   * @returns {Promise<{ success: boolean, previousStock: number, newStock: number, bankId: string, bloodGroup: string, error?: string }>}
   */
  async updateStock(bankId, payload) {
    if (!db) {
      return { success: false, error: 'Database uninitialized' };
    }

    if (!bankId) {
      return { success: false, error: 'bloodBankId is required' };
    }

    const {
      bloodGroup,
      deltaUnits,
      operation = 'MANUAL_ADJUSTMENT',
      reason = '',
      performedBy = 'system_admin',
    } = payload || {};

    const cleanGroup = String(bloodGroup || '').trim().toUpperCase();
    if (!VALID_BLOOD_GROUPS.includes(cleanGroup)) {
      return { success: false, error: `Invalid blood group: ${bloodGroup}. Must be one of ${VALID_BLOOD_GROUPS.join(', ')}` };
    }

    const delta = parseInt(deltaUnits, 10);
    if (isNaN(delta) || delta === 0) {
      return { success: false, error: 'deltaUnits must be a non-zero integer' };
    }

    const bankRef = db.collection('blood_banks').doc(bankId);

    try {
      const result = await db.runTransaction(async (transaction) => {
        const bankDoc = await transaction.get(bankRef);

        if (!bankDoc.exists) {
          throw new Error(`Blood bank ${bankId} does not exist in registry`);
        }

        const data = bankDoc.data() || {};
        const stocks = data.stocks || {
          'A+': 40, 'A-': 10, 'B+': 35, 'B-': 8,
          'AB+': 15, 'AB-': 4, 'O+': 50, 'O-': 12
        };

        const currentStock = typeof stocks[cleanGroup] === 'number' ? stocks[cleanGroup] : 0;
        const newStock = currentStock + delta;

        if (newStock < 0) {
          throw new Error(`Insufficient stock for ${cleanGroup}. Available: ${currentStock} units, requested deduction: ${Math.abs(delta)} units.`);
        }

        // Update current group stock
        stocks[cleanGroup] = newStock;

        // Compute total units
        const totalUnits = Object.values(stocks).reduce((sum, val) => sum + (Number(val) || 0), 0);

        // Record stock transaction history log
        const logRef = bankRef.collection('stock_transactions').doc();
        transaction.set(logRef, {
          id: logRef.id,
          bankId,
          bloodGroup: cleanGroup,
          deltaUnits: delta,
          previousStock: currentStock,
          newStock,
          operation,
          reason,
          performedBy,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Update blood bank document
        transaction.set(bankRef, {
          stocks,
          totalUnitsAvailable: totalUnits,
          bloodStockStatus: newStock <= 5 ? 'Critical Low' : newStock <= 15 ? 'Low' : 'Adequate',
          lastStockUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });

        return {
          success: true,
          bankId,
          bloodGroup: cleanGroup,
          previousStock: currentStock,
          newStock,
          deltaUnits: delta,
          operation,
        };
      });

      // Audit log in loggerBot
      await loggerBotService.logOtpEvent({
        requestId: `stock_${bankId}_${Date.now()}`,
        rawEmail: 'inventory@bloodsync.org',
        requestStatus: REQUEST_STATUS.SUCCESS,
        otpGenStatus: 'SKIPPED',
        emailSendingStatus: 'SKIPPED',
        errorCategory: null,
        errorMessage: null,
        metadata: {
          action: 'STOCK_TRANSACTION',
          ...result,
        },
      });

      return result;
    } catch (err) {
      console.error(`[BloodBankService] Transaction failed for bank ${bankId}:`, err.message);
      return {
        success: false,
        error: err.message,
      };
    }
  }

  /**
   * Retrieves all registered blood banks and their stock levels.
   */
  async getAllBanks() {
    if (!db) return [];
    try {
      const snap = await db.collection('blood_banks').get();
      return snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    } catch (err) {
      console.error('[BloodBankService] Error fetching blood banks:', err.message);
      return [];
    }
  }

  /**
   * Seeds initial blood banks if the collection is empty.
   */
  async initializeDefaults() {
    if (!db) return;
    try {
      const snap = await db.collection('blood_banks').limit(1).get();
      if (snap.empty) {
        console.log('[BloodBankService] Seeding default blood banks into Firestore...');
        const initialBanks = [
          {
            id: 'bb_metro_central',
            name: 'Metro Central Blood Bank',
            address: '108 Healthcare Blvd, Central District',
            phone: '+91 79 2656 1234',
            openHours: '24/7',
            distanceKm: 2.1,
            bloodStockStatus: 'Adequate',
            latitude: 23.0225,
            longitude: 72.5714,
            stocks: { 'A+': 55, 'A-': 12, 'B+': 48, 'B-': 9, 'AB+': 22, 'AB-': 5, 'O+': 68, 'O-': 14 },
          },
          {
            id: 'bb_redcross_civic',
            name: 'Red Cross Civic Blood Center',
            address: 'Ring Road, Civic Center Cross',
            phone: '+91 79 2658 5678',
            openHours: '24/7',
            distanceKm: 4.5,
            bloodStockStatus: 'Adequate',
            latitude: 23.0300,
            longitude: 72.5800,
            stocks: { 'A+': 42, 'A-': 8, 'B+': 39, 'B-': 6, 'AB+': 18, 'AB-': 3, 'O+': 52, 'O-': 10 },
          },
          {
            id: 'bb_apollo_life',
            name: 'Apollo Lifeline Regional Bank',
            address: 'Plot 14, SG Highway Medical Zone',
            phone: '+91 79 4000 9999',
            openHours: '24/7',
            distanceKm: 6.8,
            bloodStockStatus: 'Adequate',
            latitude: 23.0450,
            longitude: 72.5350,
            stocks: { 'A+': 60, 'A-': 15, 'B+': 50, 'B-': 11, 'AB+': 25, 'AB-': 6, 'O+': 75, 'O-': 18 },
          },
        ];

        for (const bank of initialBanks) {
          const total = Object.values(bank.stocks).reduce((a, b) => a + b, 0);
          await db.collection('blood_banks').doc(bank.id).set({
            ...bank,
            totalUnitsAvailable: total,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        }
        console.log('[BloodBankService] Default blood banks seeded successfully.');
      }
    } catch (err) {
      console.warn('[BloodBankService] Could not initialize default blood banks:', err.message);
    }
  }
}

module.exports = new BloodBankService();
