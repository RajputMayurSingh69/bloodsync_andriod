/**
 * BloodSync Backend - Authentication Routes
 */

const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');

// POST /auth/request-otp - Request a cryptographically secure 6-digit OTP
router.post('/request-otp', (req, res) => authController.requestOtp(req, res));

// POST /auth/verify-otp - Verify OTP and obtain JWT + Firebase Custom Token
router.post('/verify-otp', (req, res) => authController.verifyOtp(req, res));

module.exports = router;
