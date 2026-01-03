const express = require('express');
const router = express.Router();

// TODO: Implement authentication in Sprint 7
router.post('/register', (req, res) => {
  res.status(501).json({ error: 'Not implemented yet' });
});

router.post('/login', (req, res) => {
  res.status(501).json({ error: 'Not implemented yet' });
});

module.exports = router;
