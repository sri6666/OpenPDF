const request = require('supertest');
const app = require('../../src/app');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const Document = require('../../src/models/Document');

describe('Document Controller Integration Tests', () => {
  let mongoServer;

  beforeAll(async () => {
    mongoServer = await MongoMemoryServer.create();
    const mongoUri = mongoServer.getUri();
    await mongoose.connect(mongoUri);
  });

  afterAll(async () => {
    await mongoose.disconnect();
    await mongoServer.stop();
  });

  afterEach(async () => {
    await Document.deleteMany({});
  });

  describe('POST /api/documents/upload', () => {
    it('should upload valid PDF and return document ID', async () => {
      // Given
      const pdfBuffer = Buffer.from('%PDF-1.4\ntest pdf');

      // When
      const response = await request(app)
        .post('/api/documents/upload')
        .attach('file', pdfBuffer, 'test.pdf')
        .expect(201);

      // Then
      expect(response.body).toHaveProperty('documentId');
      expect(response.body).toHaveProperty('fileName', 'test.pdf');
      expect(response.body).toHaveProperty('status', 'uploaded');
    });

    it('should reject non-PDF files', async () => {
      // Given
      const txtBuffer = Buffer.from('not a pdf');

      // When/Then
      await request(app)
        .post('/api/documents/upload')
        .attach('file', txtBuffer, 'test.txt')
        .expect(400);
    });

    it('should reject files larger than 10MB', async () => {
      // Given
      const largePdf = Buffer.alloc(11 * 1024 * 1024);

      // When/Then
      await request(app)
        .post('/api/documents/upload')
        .attach('file', largePdf, 'large.pdf')
        .expect(413);
    });

    it('should reject request without file', async () => {
      // When/Then
      await request(app)
        .post('/api/documents/upload')
        .expect(400);
    });
  });

  describe('GET /api/documents/:id', () => {
    it('should return document metadata', async () => {
      // Given
      const doc = await Document.create({
        fileName: 'test.pdf',
        fileSize: 1024,
        fileUrl: '/storage/test.pdf',
        status: 'uploaded'
      });

      // When
      const response = await request(app)
        .get(`/api/documents/${doc._id}`)
        .expect(200);

      // Then
      expect(response.body.fileName).toBe('test.pdf');
      expect(response.body.status).toBe('uploaded');
    });

    it('should return 404 for non-existent document', async () => {
      // Given
      const fakeId = new mongoose.Types.ObjectId();

      // When/Then
      await request(app)
        .get(`/api/documents/${fakeId}`)
        .expect(404);
    });
  });

  describe('GET /api/documents', () => {
    it('should return list of documents', async () => {
      // Given
      await Document.create([
        { fileName: 'doc1.pdf', fileSize: 1024, fileUrl: '/storage/doc1.pdf', status: 'uploaded' },
        { fileName: 'doc2.pdf', fileSize: 2048, fileUrl: '/storage/doc2.pdf', status: 'uploaded' }
      ]);

      // When
      const response = await request(app)
        .get('/api/documents')
        .expect(200);

      // Then
      expect(response.body.documents).toHaveLength(2);
    });

    it('should support pagination', async () => {
      // Given
      for (let i = 0; i < 25; i++) {
        await Document.create({
          fileName: `doc${i}.pdf`,
          fileSize: 1024,
          fileUrl: `/storage/doc${i}.pdf`,
          status: 'uploaded'
        });
      }

      // When
      const response = await request(app)
        .get('/api/documents?page=2&limit=10')
        .expect(200);

      // Then
      expect(response.body.documents).toHaveLength(10);
      expect(response.body.page).toBe(2);
      expect(response.body.totalPages).toBe(3);
    });
  });

  describe('DELETE /api/documents/:id', () => {
    it('should delete document', async () => {
      // Given
      const doc = await Document.create({
        fileName: 'delete-me.pdf',
        fileSize: 1024,
        fileUrl: '/storage/delete-me.pdf',
        status: 'uploaded'
      });

      // When
      await request(app)
        .delete(`/api/documents/${doc._id}`)
        .expect(204);

      // Then - verify it's gone
      const found = await Document.findById(doc._id);
      expect(found).toBeNull();
    });

    it('should return 404 when deleting non-existent document', async () => {
      // Given
      const fakeId = new mongoose.Types.ObjectId();

      // When/Then
      await request(app)
        .delete(`/api/documents/${fakeId}`)
        .expect(404);
    });
  });
});
