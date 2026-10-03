/**
 * BloodSync Backend - Blood Bank Inventory Routes
 */

const express = require('express');
const router = express.Router();
const bloodBankService = require('../services/bloodBankService');

// GET /blood-banks - List all blood banks and current stock levels
router.get('/', async (req, res) => {
  try {
    const banks = await bloodBankService.getAllBanks();
    res.status(200).json({ success: true, count: banks.length, data: banks });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// POST /blood-banks/:id/update-stock - Transactional stock update for a specific blood bank
router.post('/:id/update-stock', async (req, res) => {
  const bankId = req.params.id;
  const result = await bloodBankService.updateStock(bankId, req.body);
  res.status(result.success ? 200 : 400).json(result);
});

// POST /blood-banks/update-stock - Transactional stock update with bloodBankId in body
router.post('/update-stock', async (req, res) => {
  const bankId = req.body.bloodBankId || req.body.id || req.body.bankId;
  const result = await bloodBankService.updateStock(bankId, req.body);
  res.status(result.success ? 200 : 400).json(result);
});

module.exports = router;
