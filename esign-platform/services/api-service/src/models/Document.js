const mongoose = require('mongoose');

const documentSchema = new mongoose.Schema({
  fileName: {
    type: String,
    required: true
  },
  fileSize: {
    type: Number,
    required: true
  },
  fileUrl: {
    type: String,
    required: true
  },
  status: {
    type: String,
    enum: ['uploaded', 'processing', 'ready', 'sent', 'signed', 'completed', 'cancelled'],
    default: 'uploaded'
  },
  pageCount: {
    type: Number
  },
  uploadedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  },
  signatureFields: [{
    fieldId: String,
    page: Number,
    x: Number,
    y: Number,
    width: Number,
    height: Number,
    recipientEmail: String,
    signed: {
      type: Boolean,
      default: false
    },
    signedAt: Date
  }],
  recipients: [{
    email: String,
    name: String,
    order: Number,
    status: {
      type: String,
      enum: ['pending', 'sent', 'viewed', 'signed'],
      default: 'pending'
    },
    sentAt: Date,
    viewedAt: Date,
    signedAt: Date
  }],
  metadata: {
    type: Map,
    of: String
  }
}, {
  timestamps: true
});

// Indexes
documentSchema.index({ status: 1 });
documentSchema.index({ uploadedBy: 1 });
documentSchema.index({ createdAt: -1 });

module.exports = mongoose.model('Document', documentSchema);
