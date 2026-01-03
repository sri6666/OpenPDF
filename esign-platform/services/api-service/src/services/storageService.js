const fs = require('fs').promises;
const path = require('path');
const crypto = require('crypto');

class StorageService {
  constructor() {
    this.storageType = process.env.STORAGE_TYPE || 'local';
    this.storagePath = process.env.STORAGE_PATH || path.join(__dirname, '../../storage');
  }

  /**
   * Initialize storage directory
   */
  async init() {
    if (this.storageType === 'local') {
      await fs.mkdir(this.storagePath, { recursive: true });
    }
  }

  /**
   * Save file to storage
   * @param {Buffer} buffer - File buffer
   * @param {string} originalName - Original filename
   * @returns {Promise<string>} File URL/path
   */
  async save(buffer, originalName) {
    const fileName = this.generateFileName(originalName);

    if (this.storageType === 'local') {
      const filePath = path.join(this.storagePath, fileName);
      await fs.writeFile(filePath, buffer);
      return `/storage/${fileName}`;
    }

    // TODO: Implement S3 storage
    throw new Error('S3 storage not implemented yet');
  }

  /**
   * Get file from storage
   * @param {string} fileUrl - File URL/path
   * @returns {Promise<Buffer>}
   */
  async get(fileUrl) {
    if (this.storageType === 'local') {
      const fileName = path.basename(fileUrl);
      const filePath = path.join(this.storagePath, fileName);
      return await fs.readFile(filePath);
    }

    throw new Error('S3 storage not implemented yet');
  }

  /**
   * Delete file from storage
   * @param {string} fileUrl - File URL/path
   */
  async delete(fileUrl) {
    if (this.storageType === 'local') {
      const fileName = path.basename(fileUrl);
      const filePath = path.join(this.storagePath, fileName);
      await fs.unlink(filePath);
    }

    // TODO: Implement S3 storage
  }

  /**
   * Generate unique filename
   * @param {string} originalName
   * @returns {string}
   */
  generateFileName(originalName) {
    const timestamp = Date.now();
    const random = crypto.randomBytes(8).toString('hex');
    const ext = path.extname(originalName);
    return `${timestamp}-${random}${ext}`;
  }
}

module.exports = new StorageService();
