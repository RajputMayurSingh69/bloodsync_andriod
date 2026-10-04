/**
 * BloodSync Backend - Blood Bank Authentication Controller
 *
 * Handles:
 *  POST /bank-auth/register  - Blood Bank Registration Portal
 *  POST /bank-auth/login     - Blood Bank Sign In
 *  GET  /bank-auth/me        - Get authenticated bank profile (protected)
 *
 * Architecture:
 *  - Input validation -> Duplicate guard -> bcrypt hash -> Firestore write
 *    -> Firebase Auth user (custom claim: role=blood_bank) -> JWT -> welcome email
 *  - Consistent safe error responses (no stack traces to client)
 *  - Full audit trail via loggerBotService
 */

'use strict';

const { v4: uuidv4 } = require('uuid');
const bcrypt          = require('bcryptjs');
const jwt             = require('jsonwebtoken');
const { auth, db }    = require('../config/firebase');
const { SECURITY_CONFIG }           = require('../config/environment');
const { ERROR_CATEGORIES, REQUEST_STATUS, FIRESTORE_COLLECTIONS } = require('../config/constants');
const { isValidEmail, isValidPhone, isNonEmptyString } = require('../utils/validator');
const { maskEmail, sanitizeIp }     = require('../utils/sanitizer');
const emailService      = require('../services/emailService');
const loggerBotService  = require('../services/loggerBotService');

const BCRYPT_ROUNDS = 12;
const COLLECTION    = FIRESTORE_COLLECTIONS.BANK_REGISTRATIONS || 'bank_registrations';

/* -----------------------------------------------------------------
   HELPER: Sign a JWT for a blood bank session
----------------------------------------------------------------- */
function signBankJwt(bankId, email) {
  return jwt.sign(
    { sub: bankId, email, role: 'blood_bank', type: 'bank_session' },
    SECURITY_CONFIG.jwtSecret,
    { expiresIn: SECURITY_CONFIG.jwtExpiration || '24h' }
  );
}

/* -----------------------------------------------------------------
   HELPER: Verify Bearer JWT from Authorization header
----------------------------------------------------------------- */
function verifyBankJwt(authHeader) {
  if (!authHeader || !authHeader.startsWith('Bearer ')) return null;
  try {
    return jwt.verify(authHeader.slice(7), SECURITY_CONFIG.jwtSecret);
  } catch {
    return null;
  }
}

class BloodBankAuthController {

  /* ================================================================
     POST /bank-auth/register
     Blood Bank Registration Portal - full onboarding flow
  ================================================================ */
  async register(req, res) {
    const startTime = Date.now();
    const requestId = req.headers['x-request-id'] || uuidv4();
    const clientIp  = sanitizeIp(req.ip || req.connection?.remoteAddress);
    const userAgent = req.headers['user-agent'] || 'unknown';

    const { bankName, email, phone, city, address, password } = req.body || {};

    // STEP 1: Input Validation
    const validationErrors = [];
    if (!isNonEmptyString(bankName, 3))
      validationErrors.push('Blood bank name must be at least 3 characters.');
    if (!email || !isValidEmail(email))
      validationErrors.push('A valid email address is required.');
    if (!isValidPhone(phone))
      validationErrors.push('A valid phone number (min 10 digits) is required.');
    if (!isNonEmptyString(city, 2))
      validationErrors.push('City is required.');
    if (!isNonEmptyString(address, 8))
      validationErrors.push('Full address (min 8 characters) is required.');
    if (!password || password.length < 6)
      validationErrors.push('Password must be at least 6 characters.');

    if (validationErrors.length > 0) {
      await loggerBotService.logOtpEvent({
        requestId,
        rawEmail: email || 'unknown',
        requestStatus: REQUEST_STATUS.FAILURE,
        otpGenStatus: 'SKIPPED',
        emailSendingStatus: 'SKIPPED',
        errorCategory: ERROR_CATEGORIES.VALIDATION_ERROR,
        errorMessage: `Bank registration validation failed: ${validationErrors.join(' | ')}`,
        metadata: { clientIp, userAgent, action: 'BANK_REGISTER', durationMs: Date.now() - startTime },
      });
      return res.status(400).json({ success: false, message: 'Validation failed.', errors: validationErrors });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const normalizedPhone = phone.trim();
    const normalizedName  = bankName.trim();
    const normalizedCity  = city.trim();
    const normalizedAddr  = address.trim();

    // STEP 2: Duplicate Email Guard
    if (db) {
      try {
        const existing = await db
          .collection(COLLECTION)
          .where('email', '==', normalizedEmail)
          .limit(1)
          .get();
        if (!existing.empty) {
          return res.status(409).json({
            success: false,
            message: 'An account with this email already exists. Please sign in.',
          });
        }
      } catch (dbErr) {
        console.error('[BankAuth] Duplicate check failed:', dbErr.message);
      }
    }

    // STEP 3: Hash Password
    let passwordHash;
    try {
      passwordHash = await bcrypt.hash(password, BCRYPT_ROUNDS);
    } catch (hashErr) {
      console.error('[BankAuth] bcrypt hash error:', hashErr.message);
      return res.status(500).json({ success: false, message: 'Registration failed. Please try again later.' });
    }

    // STEP 4: Write to Firestore
    const bankId  = `bank_${uuidv4().replace(/-/g, '').slice(0, 16)}`;
    const bankDoc = {
      bankId,
      bankName:    normalizedName,
      email:       normalizedEmail,
      phone:       normalizedPhone,
      city:        normalizedCity,
      address:     normalizedAddr,
      passwordHash,
      role:        'blood_bank',
      verified:    false,
      active:      true,
      stocks:      { 'A+': 0, 'A-': 0, 'B+': 0, 'B-': 0, 'AB+': 0, 'AB-': 0, 'O+': 0, 'O-': 0 },
      totalUnitsAvailable: 0,
      registrationIp: clientIp,
      createdAt:   new Date().toISOString(),
      updatedAt:   new Date().toISOString(),
    };

    if (db) {
      try {
        await db.collection(COLLECTION).doc(bankId).set(bankDoc);

        // Lightweight entry in blood_banks for inventory queries
        await db.collection(FIRESTORE_COLLECTIONS.BLOOD_BANKS || 'blood_banks').doc(bankId).set({
          bankId,
          name:    normalizedName,
          email:   normalizedEmail,
          phone:   normalizedPhone,
          city:    normalizedCity,
          address: normalizedAddr,
          verified: false,
          active:   true,
          stocks:  bankDoc.stocks,
          totalUnitsAvailable: 0,
          bloodStockStatus: 'Pending Setup',
          createdAt: new Date().toISOString(),
        });
      } catch (fsErr) {
        console.error('[BankAuth] Firestore write failed:', fsErr.message);
        await loggerBotService.logOtpEvent({
          requestId,
          rawEmail: normalizedEmail,
          requestStatus: REQUEST_STATUS.FAILURE,
          otpGenStatus: 'SKIPPED',
          emailSendingStatus: 'SKIPPED',
          errorCategory: ERROR_CATEGORIES.FIREBASE_ERROR,
          errorMessage: `Firestore bank registration write failed: ${fsErr.message}`,
          metadata: { clientIp, userAgent, action: 'BANK_REGISTER', durationMs: Date.now() - startTime },
        });
        return res.status(500).json({ success: false, message: 'Registration failed. Please try again later.' });
      }
    }

    // STEP 5: Firebase Auth User with custom claim role=blood_bank
    let firebaseCustomToken = null;
    if (auth) {
      try {
        let userRecord;
        try {
          userRecord = await auth.getUserByEmail(normalizedEmail);
        } catch (findErr) {
          if (findErr.code === 'auth/user-not-found') {
            userRecord = await auth.createUser({
              uid:           bankId,
              email:         normalizedEmail,
              emailVerified: false,
              displayName:   normalizedName,
            });
          }
        }
        if (userRecord) {
          await auth.setCustomUserClaims(userRecord.uid, { role: 'blood_bank', bankId, active: true });
          firebaseCustomToken = await auth.createCustomToken(userRecord.uid, { role: 'blood_bank', bankId });
        }
      } catch (authErr) {
        console.warn('[BankAuth] Firebase Auth setup warning:', authErr.message);
      }
    }

    // STEP 6: Issue JWT
    const jwtToken = signBankJwt(bankId, normalizedEmail);

    // STEP 7: Send Welcome Email (best-effort)
    try {
      if (emailService && typeof emailService.sendOtpEmail === 'function') {
        await emailService.sendOtpEmail({
          recipientEmail: normalizedEmail,
          otp: `WELCOME_${normalizedName}`,
          requestId,
        });
      }
    } catch (mailErr) {
      console.warn('[BankAuth] Welcome email failed:', mailErr.message);
    }

    // STEP 8: Audit Log
    await loggerBotService.logOtpEvent({
      requestId,
      rawEmail: normalizedEmail,
      requestStatus: REQUEST_STATUS.SUCCESS,
      otpGenStatus: 'SKIPPED',
      emailSendingStatus: 'SENT',
      errorCategory: null,
      errorMessage: null,
      metadata: {
        action: 'BANK_REGISTERED', bankId, bankName: normalizedName, city: normalizedCity,
        clientIp, userAgent, durationMs: Date.now() - startTime,
      },
    });

    // STEP 9: Success Response
    return res.status(201).json({
      success: true,
      message: 'Blood bank registered successfully. Your account is under review.',
      data: {
        bankId,
        bankName:    normalizedName,
        email:       normalizedEmail,
        maskedEmail: maskEmail(normalizedEmail),
        city:        normalizedCity,
        phone:       normalizedPhone,
        address:     normalizedAddr,
        verified:    false,
        active:      true,
        token:       jwtToken,
        firebaseCustomToken,
      },
    });
  }


  /* ================================================================
     POST /bank-auth/login
     Blood Bank Sign In
  ================================================================ */
  async login(req, res) {
    const startTime = Date.now();
    const requestId = req.headers['x-request-id'] || uuidv4();
    const clientIp  = sanitizeIp(req.ip || req.connection?.remoteAddress);
    const userAgent = req.headers['user-agent'] || 'unknown';

    const { email, password } = req.body || {};

    if (!email || !isValidEmail(email))
      return res.status(400).json({ success: false, message: 'A valid email address is required.' });
    if (!password)
      return res.status(400).json({ success: false, message: 'Password is required.' });

    const normalizedEmail = email.trim().toLowerCase();

    if (!db)
      return res.status(503).json({ success: false, message: 'Service temporarily unavailable.' });

    // Fetch bank record
    let bankData = null;
    let bankDocId = null;
    try {
      const snap = await db.collection(COLLECTION).where('email', '==', normalizedEmail).limit(1).get();
      if (snap.empty)
        return res.status(401).json({ success: false, message: 'Invalid email or password.' });
      const doc = snap.docs[0];
      bankData  = doc.data();
      bankDocId = doc.id;
    } catch (dbErr) {
      console.error('[BankAuth] Login DB fetch failed:', dbErr.message);
      return res.status(500).json({ success: false, message: 'Sign in failed. Please try again.' });
    }

    // Verify password
    const passwordMatch = await bcrypt.compare(password, bankData.passwordHash || '');
    if (!passwordMatch) {
      await loggerBotService.logOtpEvent({
        requestId, rawEmail: normalizedEmail,
        requestStatus: REQUEST_STATUS.FAILURE,
        otpGenStatus: 'SKIPPED', emailSendingStatus: 'SKIPPED',
        errorCategory: ERROR_CATEGORIES.UNAUTHORIZED_ERROR,
        errorMessage: 'Bank login failed: incorrect password.',
        metadata: { clientIp, userAgent, action: 'BANK_LOGIN', durationMs: Date.now() - startTime },
      });
      return res.status(401).json({ success: false, message: 'Invalid email or password.' });
    }

    if (bankData.active === false)
      return res.status(403).json({ success: false, message: 'Your account has been deactivated. Contact BloodSync support.' });

    // Issue tokens
    const jwtToken = signBankJwt(bankDocId, normalizedEmail);
    let firebaseCustomToken = null;
    if (auth) {
      try {
        const uid = bankData.bankId || bankDocId;
        firebaseCustomToken = await auth.createCustomToken(uid, { role: 'blood_bank', bankId: uid });
      } catch (authErr) {
        console.warn('[BankAuth] Firebase token error on login:', authErr.message);
      }
    }

    // Audit log
    await loggerBotService.logOtpEvent({
      requestId, rawEmail: normalizedEmail,
      requestStatus: REQUEST_STATUS.SUCCESS,
      otpGenStatus: 'SKIPPED', emailSendingStatus: 'SKIPPED',
      errorCategory: null, errorMessage: null,
      metadata: {
        action: 'BANK_LOGIN', bankId: bankData.bankId, bankName: bankData.bankName,
        clientIp, userAgent, durationMs: Date.now() - startTime,
      },
    });

    const { passwordHash: _pw, registrationIp: _ip, ...safeData } = bankData;
    return res.status(200).json({
      success: true,
      message: 'Signed in successfully.',
      data: { ...safeData, maskedEmail: maskEmail(normalizedEmail), token: jwtToken, firebaseCustomToken },
    });
  }


  /* ================================================================
     GET /bank-auth/me
     Returns authenticated blood bank profile.
     Requires: Authorization: Bearer <jwt>
  ================================================================ */
  async getMe(req, res) {
    const decoded = verifyBankJwt(req.headers['authorization']);
    if (!decoded || decoded.role !== 'blood_bank')
      return res.status(401).json({ success: false, message: 'Unauthorized. Valid bank token required.' });

    if (!db)
      return res.status(503).json({ success: false, message: 'Service temporarily unavailable.' });

    try {
      const snap = await db.collection(COLLECTION).where('email', '==', decoded.email).limit(1).get();
      if (snap.empty)
        return res.status(404).json({ success: false, message: 'Bank profile not found.' });

      const { passwordHash: _pw, registrationIp: _ip, ...safeData } = snap.docs[0].data();
      return res.status(200).json({ success: true, data: safeData });
    } catch (err) {
      console.error('[BankAuth] getMe failed:', err.message);
      return res.status(500).json({ success: false, message: 'Failed to fetch profile.' });
    }
  }
}

module.exports = new BloodBankAuthController();
