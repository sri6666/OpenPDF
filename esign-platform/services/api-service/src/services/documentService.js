const Document = require('../models/Document');
const storageService = require('./storageService');
const path = require('path');

class DocumentService {
  /**
   * Upload a PDF document
   * @param {Object} file - Multer file object
   * @returns {Promise<Document>}
   */
  async uploadDocument(file) {
    // Save file to storage
    const fileUrl = await storageService.save(file.buffer, file.originalname);

    // Create document record
    const document = await Document.create({
      fileName: file.originalname,
      fileSize: file.size,
      fileUrl: fileUrl,
      status: 'uploaded'
    });

    // TODO: Call PDF service to extract metadata (page count, etc.)

    return document;
  }

  /**
   * Get document by ID
   * @param {string} id - Document ID
   * @returns {Promise<Document>}
   */
  async getDocument(id) {
    const document = await Document.findById(id);
    if (!document) {
      throw new Error('Document not found');
    }
    return document;
  }

  /**
   * Delete document
   * @param {string} id - Document ID
   */
  async deleteDocument(id) {
    const document = await this.getDocument(id);

    // Delete file from storage
    await storageService.delete(document.fileUrl);

    // Delete document record
    await Document.findByIdAndDelete(id);
  }
}

module.exports = new DocumentService();
