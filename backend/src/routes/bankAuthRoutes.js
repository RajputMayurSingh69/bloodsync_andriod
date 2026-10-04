/**
 * BloodSync Backend - Blood Bank Authentication Routes
 *
 * POST /bank-auth/register  -> Blood Bank Registration (Portal 2)
 * POST /bank-auth/login     -> Blood Bank Sign In
 * GET  /bank-auth/me        -> Protected: fetch bank profile
 */

'use strict';

const express  = require('express');
const router   = express.Router();
const bankAuthController = require('../controllers/bloodBankAuthController');

/**
 * POST /bank-auth/register
 * Body: { bankName, email, phone, city, address, password }
 * Response 201: { success, message, data: { bankId, token, firebaseCustomToken, ... } }
 */
router.post('/register', (req, res) => bankAuthController.register(req, res));

/**
 * POST /bank-auth/login
 * Body: { email, password }
 * Response 200: { success, message, data: { token, firebaseCustomToken, bankName, ... } }
 */
router.post('/login', (req, res) => bankAuthController.login(req, res));

/**
 * GET /bank-auth/me
 * Headers: Authorization: Bearer <jwt>
 * Response 200: { success, data: { bankName, email, city, stocks, ... } }
 */
router.get('/me', (req, res) => bankAuthController.getMe(req, res));

module.exports = router;
