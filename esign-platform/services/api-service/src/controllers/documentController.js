const express = require('express');
const multer = require('multer');
const Document = require('../models/Document');
const documentService = require('../services/documentService');

const router = express.Router();

// Configure multer for file uploads
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB
  },
  fileFilter: (req, file, cb) => {
    if (file.mimetype === 'application/pdf') {
      cb(null, true);
    } else {
      cb(new Error('Only PDF files are allowed'), false);
    }
  }
});

/**
 * POST /api/documents/upload
 * Upload a PDF document
 */
router.post('/upload', upload.single('file'), async (req, res, next) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }

    // Save document
    const document = await documentService.uploadDocument(req.file);

    res.status(201).json({
      documentId: document._id,
      fileName: document.fileName,
      fileSize: document.fileSize,
      status: document.status
    });
  } catch (error) {
    if (error.message.includes('Only PDF')) {
      return res.status(400).json({ error: error.message });
    }
    next(error);
  }
});

/**
 * GET /api/documents
 * List all documents with pagination
 */
router.get('/', async (req, res, next) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;

    const [documents, total] = await Promise.all([
      Document.find()
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limit)
        .lean(),
      Document.countDocuments()
    ]);

    res.json({
      documents,
      page,
      limit,
      totalPages: Math.ceil(total / limit),
      total
    });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /api/documents/:id
 * Get document by ID
 */
router.get('/:id', async (req, res, next) => {
  try {
    const document = await Document.findById(req.params.id);

    if (!document) {
      return res.status(404).json({ error: 'Document not found' });
    }

    res.json(document);
  } catch (error) {
    if (error.name === 'CastError') {
      return res.status(404).json({ error: 'Document not found' });
    }
    next(error);
  }
});

/**
 * DELETE /api/documents/:id
 * Delete document
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const document = await Document.findByIdAndDelete(req.params.id);

    if (!document) {
      return res.status(404).json({ error: 'Document not found' });
    }

    // TODO: Also delete file from storage

    res.status(204).send();
  } catch (error) {
    if (error.name === 'CastError') {
      return res.status(404).json({ error: 'Document not found' });
    }
    next(error);
  }
});

// Handle multer errors
router.use((error, req, res, next) => {
  if (error instanceof multer.MulterError) {
    if (error.code === 'LIMIT_FILE_SIZE') {
      return res.status(413).json({ error: 'File too large. Maximum size is 10MB' });
    }
  }
  next(error);
});

module.exports = router;
