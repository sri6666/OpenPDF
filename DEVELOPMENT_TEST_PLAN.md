# E-Signature Platform - Development & Test Plan

**Project:** Generic E-Signature Platform (OpenSign + OpenPDF Integration)
**Approach:** Incremental development with test-first methodology
**Stack:** Java (OpenPDF) + Node.js (OpenSign) + React
**Timeline:** 12 weeks (3-month MVP)

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Development Principles](#development-principles)
3. [Sprint Plan (12 Sprints)](#sprint-plan)
4. [Testing Strategy](#testing-strategy)
5. [Tech Stack Details](#tech-stack-details)
6. [Code Examples](#code-examples)
7. [CI/CD Pipeline](#cicd-pipeline)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React)                        │
│  - PDF viewer (PDF.js)                                      │
│  - Signature placement UI                                   │
│  - Document upload                                          │
└────────────────────┬────────────────────────────────────────┘
                     │ REST API
┌────────────────────▼────────────────────────────────────────┐
│              Backend API (Node.js/Express)                  │
│  - Authentication (JWT)                                     │
│  - Document management                                      │
│  - Signature workflow                                       │
│  - Email notifications                                      │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP API calls
┌────────────────────▼────────────────────────────────────────┐
│           PDF Service (Java/Spring Boot)                    │
│  - OpenPDF library                                          │
│  - PDF manipulation                                         │
│  - Signature embedding                                      │
│  - Form filling                                             │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                  Storage & Database                         │
│  - MongoDB (document metadata)                              │
│  - S3/Local (PDF files)                                     │
│  - Redis (job queue)                                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Development Principles

### Test-Driven Development (TDD)

**Order of Testing:**
```
1. Unit Tests (70% coverage target)
   ↓
2. Integration Tests (API layer)
   ↓
3. E2E Tests (Critical paths only, 5-10 tests max)
```

### Development Rules

✅ **DO:**
- Write test before implementation
- Keep sprints small (1 week max)
- Deploy to staging after each sprint
- Merge to main only with passing tests
- Write tests for bugs before fixing

❌ **DON'T:**
- Write code without tests
- Let test coverage drop below 70%
- Skip integration tests for APIs
- Write comprehensive UI tests (too brittle)
- Leave "TODO: add tests" comments

---

## Sprint Plan (12 Sprints)

### Sprint 0: Project Setup (Week 0)
**Goal:** Set up development environment and CI/CD

**Tasks:**
- [ ] Initialize monorepo (Nx, Turborepo, or Lerna)
- [ ] Set up Java Spring Boot project (PDF service)
- [ ] Set up Node.js Express project (API)
- [ ] Set up React project (Frontend)
- [ ] Configure Docker Compose for local dev
- [ ] Set up GitHub Actions CI/CD
- [ ] Configure test frameworks (JUnit, Jest, Vitest)
- [ ] Set up code coverage tools (JaCoCo, Istanbul)

**Deliverables:**
```
repo/
├── services/
│   ├── pdf-service/         # Java Spring Boot
│   ├── api-service/         # Node.js Express
│   └── frontend/            # React
├── docker-compose.yml
├── .github/workflows/
│   ├── test.yml
│   └── deploy.yml
└── README.md
```

**Tests:** N/A (infrastructure setup)

**Time:** 3-5 days

---

### Sprint 1: PDF Upload & Storage (Week 1)
**Goal:** Upload PDF, store in S3/local, save metadata to DB

**Features:**
- Upload PDF via API
- Validate PDF file
- Store in S3 or local filesystem
- Save metadata to MongoDB
- Return document ID

**Test Plan:**

**Unit Tests (Write First):**
```java
// PDF Service - Java
@Test
void testValidatePdf_ValidFile_ReturnsTrue() {
    // Given
    byte[] validPdf = loadTestPdf("valid.pdf");
    PdfValidator validator = new PdfValidator();

    // When
    boolean result = validator.isValid(validPdf);

    // Then
    assertTrue(result);
}

@Test
void testValidatePdf_InvalidFile_ReturnsFalse() {
    // Given
    byte[] invalidPdf = "not a pdf".getBytes();
    PdfValidator validator = new PdfValidator();

    // When
    boolean result = validator.isValid(invalidPdf);

    // Then
    assertFalse(result);
}

@Test
void testValidatePdf_TooLarge_ThrowsException() {
    // Given
    byte[] largePdf = new byte[11 * 1024 * 1024]; // 11MB
    PdfValidator validator = new PdfValidator();

    // When/Then
    assertThrows(FileTooLargeException.class,
        () -> validator.isValid(largePdf));
}
```

**API Tests (Integration):**
```javascript
// API Service - Node.js
describe('POST /api/documents/upload', () => {
  it('should upload valid PDF and return document ID', async () => {
    // Given
    const pdfBuffer = fs.readFileSync('test/fixtures/valid.pdf');

    // When
    const response = await request(app)
      .post('/api/documents/upload')
      .attach('file', pdfBuffer, 'test.pdf')
      .expect(201);

    // Then
    expect(response.body).toHaveProperty('documentId');
    expect(response.body).toHaveProperty('fileName', 'test.pdf');
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
      .expect(413); // Payload Too Large
  });
});
```

**Implementation (After Tests Pass):**
```java
// PdfValidator.java
@Component
public class PdfValidator {
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    public boolean isValid(byte[] pdfData) {
        if (pdfData.length > MAX_SIZE) {
            throw new FileTooLargeException("File exceeds 10MB limit");
        }

        try (PdfReader reader = new PdfReader(pdfData)) {
            return reader.getNumberOfPages() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
```

```javascript
// uploadController.js
const uploadDocument = async (req, res) => {
  try {
    // Validate file exists
    if (!req.file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }

    // Validate PDF (call Java service)
    const isValid = await pdfService.validatePdf(req.file.buffer);
    if (!isValid) {
      return res.status(400).json({ error: 'Invalid PDF file' });
    }

    // Store file
    const fileUrl = await storageService.upload(req.file.buffer);

    // Save metadata
    const document = await Document.create({
      fileName: req.file.originalname,
      fileSize: req.file.size,
      fileUrl: fileUrl,
      uploadedAt: new Date()
    });

    res.status(201).json({
      documentId: document._id,
      fileName: document.fileName
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};
```

**UI Tests (Minimal - Critical Path Only):**
```javascript
// frontend/e2e/upload.spec.js
test('should upload PDF successfully', async ({ page }) => {
  await page.goto('/upload');

  // Upload file
  await page.setInputFiles('input[type="file"]', 'test/fixtures/valid.pdf');

  // Verify success message
  await expect(page.locator('.success-message')).toBeVisible();
});
```

**Coverage Target:**
- Unit Tests: 90%
- API Tests: 100% of endpoints
- E2E Tests: 1 happy path test

**Deliverables:**
- ✅ PDF upload API working
- ✅ File validation implemented
- ✅ Tests passing (30+ unit tests, 5+ integration tests)
- ✅ Coverage >70%

---

### Sprint 2: PDF Metadata Extraction (Week 2)
**Goal:** Extract PDF metadata (pages, form fields, text)

**Features:**
- Get page count
- Extract form fields (if any)
- Get PDF dimensions
- Extract text from pages (optional)

**Test Plan:**

**Unit Tests (Write First):**
```java
@Test
void testExtractMetadata_ValidPdf_ReturnsCorrectPageCount() {
    // Given
    byte[] pdf = loadTestPdf("3-pages.pdf");
    PdfMetadataExtractor extractor = new PdfMetadataExtractor();

    // When
    PdfMetadata metadata = extractor.extract(pdf);

    // Then
    assertEquals(3, metadata.getPageCount());
}

@Test
void testExtractFormFields_PdfWithFields_ReturnsAllFields() {
    // Given
    byte[] pdf = loadTestPdf("form.pdf");
    PdfMetadataExtractor extractor = new PdfMetadataExtractor();

    // When
    PdfMetadata metadata = extractor.extract(pdf);

    // Then
    assertTrue(metadata.hasFormFields());
    assertEquals(5, metadata.getFormFields().size());
    assertTrue(metadata.getFormFields().contains("name"));
    assertTrue(metadata.getFormFields().contains("email"));
}

@Test
void testExtractPageDimensions_A4Pdf_ReturnsCorrectSize() {
    // Given
    byte[] pdf = loadTestPdf("a4.pdf");
    PdfMetadataExtractor extractor = new PdfMetadataExtractor();

    // When
    PdfMetadata metadata = extractor.extract(pdf);

    // Then
    assertEquals(595, metadata.getPageWidth(1), 1.0); // A4 width in points
    assertEquals(842, metadata.getPageHeight(1), 1.0); // A4 height in points
}
```

**Implementation:**
```java
@Service
public class PdfMetadataExtractor {

    public PdfMetadata extract(byte[] pdfData) throws IOException {
        try (PdfReader reader = new PdfReader(pdfData)) {
            PdfMetadata metadata = new PdfMetadata();

            // Page count
            metadata.setPageCount(reader.getNumberOfPages());

            // Page dimensions
            Rectangle pageSize = reader.getPageSize(1);
            metadata.setPageWidth(pageSize.getWidth());
            metadata.setPageHeight(pageSize.getHeight());

            // Form fields
            AcroFields fields = reader.getAcroFields();
            if (fields != null) {
                metadata.setFormFields(new ArrayList<>(fields.getFields().keySet()));
            }

            return metadata;
        }
    }
}
```

**API Tests:**
```javascript
describe('GET /api/documents/:id/metadata', () => {
  it('should return correct metadata for uploaded PDF', async () => {
    // Given
    const documentId = await uploadTestPdf('3-pages.pdf');

    // When
    const response = await request(app)
      .get(`/api/documents/${documentId}/metadata`)
      .expect(200);

    // Then
    expect(response.body.pageCount).toBe(3);
    expect(response.body.pageWidth).toBeCloseTo(595, 0);
    expect(response.body.pageHeight).toBeCloseTo(842, 0);
  });

  it('should return form fields if present', async () => {
    // Given
    const documentId = await uploadTestPdf('form.pdf');

    // When
    const response = await request(app)
      .get(`/api/documents/${documentId}/metadata`)
      .expect(200);

    // Then
    expect(response.body.hasFormFields).toBe(true);
    expect(response.body.formFields).toContain('name');
    expect(response.body.formFields).toContain('email');
  });
});
```

**Coverage Target:**
- Unit Tests: 85%
- API Tests: 100%
- E2E Tests: 0 (not needed for metadata)

**Deliverables:**
- ✅ Metadata extraction working
- ✅ API endpoint implemented
- ✅ Tests passing (20+ unit tests, 5+ API tests)

---

### Sprint 3: Signature Placement (Week 3)
**Goal:** Place signature boxes on PDF pages

**Features:**
- Define signature field coordinates
- Multiple signature fields per document
- Assign signature fields to recipients

**Test Plan:**

**Unit Tests:**
```java
@Test
void testAddSignatureField_ValidCoordinates_Success() {
    // Given
    byte[] pdf = loadTestPdf("blank.pdf");
    SignatureField field = new SignatureField(
        "signature1",
        1,    // page number
        100,  // x
        200,  // y
        200,  // width
        50    // height
    );
    PdfSignatureService service = new PdfSignatureService();

    // When
    byte[] result = service.addSignatureField(pdf, field);

    // Then
    assertNotNull(result);

    // Verify field was added
    try (PdfReader reader = new PdfReader(result)) {
        AcroFields fields = reader.getAcroFields();
        assertTrue(fields.getFields().containsKey("signature1"));
    }
}

@Test
void testAddSignatureField_InvalidPage_ThrowsException() {
    // Given
    byte[] pdf = loadTestPdf("1-page.pdf");
    SignatureField field = new SignatureField("sig1", 5, 100, 200, 200, 50);
    PdfSignatureService service = new PdfSignatureService();

    // When/Then
    assertThrows(InvalidPageException.class,
        () -> service.addSignatureField(pdf, field));
}

@Test
void testAddSignatureField_NegativeCoordinates_ThrowsException() {
    // Given
    byte[] pdf = loadTestPdf("blank.pdf");
    SignatureField field = new SignatureField("sig1", 1, -10, 200, 200, 50);
    PdfSignatureService service = new PdfSignatureService();

    // When/Then
    assertThrows(InvalidCoordinatesException.class,
        () -> service.addSignatureField(pdf, field));
}
```

**API Tests:**
```javascript
describe('POST /api/documents/:id/signature-fields', () => {
  it('should add signature field to PDF', async () => {
    // Given
    const documentId = await uploadTestPdf('blank.pdf');
    const signatureField = {
      fieldId: 'signature1',
      page: 1,
      x: 100,
      y: 200,
      width: 200,
      height: 50,
      recipientEmail: 'signer@example.com'
    };

    // When
    const response = await request(app)
      .post(`/api/documents/${documentId}/signature-fields`)
      .send(signatureField)
      .expect(201);

    // Then
    expect(response.body.fieldId).toBe('signature1');

    // Verify it's retrievable
    const getResponse = await request(app)
      .get(`/api/documents/${documentId}/signature-fields`)
      .expect(200);

    expect(getResponse.body).toHaveLength(1);
    expect(getResponse.body[0].fieldId).toBe('signature1');
  });

  it('should reject invalid page number', async () => {
    // Given
    const documentId = await uploadTestPdf('1-page.pdf');
    const signatureField = {
      fieldId: 'signature1',
      page: 5, // Invalid
      x: 100,
      y: 200,
      width: 200,
      height: 50
    };

    // When/Then
    await request(app)
      .post(`/api/documents/${documentId}/signature-fields`)
      .send(signatureField)
      .expect(400);
  });

  it('should support multiple signature fields', async () => {
    // Given
    const documentId = await uploadTestPdf('blank.pdf');

    // When
    await request(app)
      .post(`/api/documents/${documentId}/signature-fields`)
      .send({ fieldId: 'sig1', page: 1, x: 100, y: 200, width: 200, height: 50 })
      .expect(201);

    await request(app)
      .post(`/api/documents/${documentId}/signature-fields`)
      .send({ fieldId: 'sig2', page: 1, x: 100, y: 300, width: 200, height: 50 })
      .expect(201);

    // Then
    const response = await request(app)
      .get(`/api/documents/${documentId}/signature-fields`)
      .expect(200);

    expect(response.body).toHaveLength(2);
  });
});
```

**Implementation:**
```java
@Service
public class PdfSignatureService {

    public byte[] addSignatureField(byte[] pdfData, SignatureField field)
            throws IOException, DocumentException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PdfReader reader = new PdfReader(pdfData)) {
            // Validate page number
            if (field.getPage() < 1 || field.getPage() > reader.getNumberOfPages()) {
                throw new InvalidPageException("Page " + field.getPage() + " does not exist");
            }

            // Validate coordinates
            if (field.getX() < 0 || field.getY() < 0) {
                throw new InvalidCoordinatesException("Coordinates cannot be negative");
            }

            PdfStamper stamper = new PdfStamper(reader, output);

            // Create text field as signature placeholder
            TextField tf = new TextField(stamper.getWriter(),
                new Rectangle(
                    field.getX(),
                    field.getY(),
                    field.getX() + field.getWidth(),
                    field.getY() + field.getHeight()
                ),
                field.getFieldId()
            );

            tf.setOptions(TextField.READ_ONLY);
            tf.setText(""); // Empty initially

            stamper.addAnnotation(tf.getTextField(), field.getPage());
            stamper.close();
        }

        return output.toByteArray();
    }
}
```

**Frontend Tests (Minimal):**
```javascript
test('should allow placing signature field on PDF', async ({ page }) => {
  await page.goto('/editor/doc123');

  // Click to add signature field
  await page.click('.add-signature-btn');

  // Click on PDF to place it
  await page.click('.pdf-canvas', { position: { x: 100, y: 200 } });

  // Verify field appears
  await expect(page.locator('.signature-field')).toBeVisible();
});
```

**Coverage Target:**
- Unit Tests: 90%
- API Tests: 100%
- E2E Tests: 1 test

**Deliverables:**
- ✅ Signature field placement working
- ✅ API endpoints implemented
- ✅ Tests passing (25+ unit, 8+ API)

---

### Sprint 4: Image Signature Creation (Week 4)
**Goal:** Allow users to create signatures (draw, type, upload)

**Features:**
- Draw signature (canvas)
- Type signature (text to image)
- Upload signature image
- Save signature as PNG

**Test Plan:**

**Unit Tests:**
```javascript
// signatureGenerator.test.js
describe('SignatureGenerator', () => {
  it('should generate signature image from text', () => {
    // Given
    const generator = new SignatureGenerator();
    const text = 'John Doe';

    // When
    const imageBuffer = generator.textToImage(text, {
      font: 'Dancing Script',
      fontSize: 32,
      color: '#000000'
    });

    // Then
    expect(imageBuffer).toBeInstanceOf(Buffer);
    expect(imageBuffer.length).toBeGreaterThan(0);
  });

  it('should validate uploaded signature image', () => {
    // Given
    const validator = new SignatureValidator();
    const validImage = fs.readFileSync('test/fixtures/signature.png');

    // When
    const result = validator.isValidSignature(validImage);

    // Then
    expect(result.valid).toBe(true);
  });

  it('should reject non-image files', () => {
    // Given
    const validator = new SignatureValidator();
    const textFile = Buffer.from('not an image');

    // When
    const result = validator.isValidSignature(textFile);

    // Then
    expect(result.valid).toBe(false);
    expect(result.error).toContain('Invalid image');
  });

  it('should resize large signature images', () => {
    // Given
    const processor = new SignatureProcessor();
    const largeImage = fs.readFileSync('test/fixtures/large-signature.png');

    // When
    const resized = processor.resize(largeImage, { maxWidth: 200, maxHeight: 50 });

    // Then
    expect(resized.length).toBeLessThan(largeImage.length);
  });
});
```

**API Tests:**
```javascript
describe('POST /api/signatures/create', () => {
  it('should create signature from text', async () => {
    // Given
    const signatureData = {
      type: 'text',
      text: 'John Doe',
      font: 'Dancing Script',
      fontSize: 32
    };

    // When
    const response = await request(app)
      .post('/api/signatures/create')
      .send(signatureData)
      .expect(201);

    // Then
    expect(response.body).toHaveProperty('signatureId');
    expect(response.body).toHaveProperty('imageUrl');
  });

  it('should accept uploaded signature image', async () => {
    // Given
    const signatureImage = fs.readFileSync('test/fixtures/signature.png');

    // When
    const response = await request(app)
      .post('/api/signatures/upload')
      .attach('signature', signatureImage, 'signature.png')
      .expect(201);

    // Then
    expect(response.body).toHaveProperty('signatureId');
  });

  it('should save drawn signature from canvas data', async () => {
    // Given
    const canvasData = {
      type: 'drawn',
      dataUrl: 'data:image/png;base64,iVBORw0KGgoAAAANS...'
    };

    // When
    const response = await request(app)
      .post('/api/signatures/create')
      .send(canvasData)
      .expect(201);

    // Then
    expect(response.body.signatureId).toBeDefined();
  });
});
```

**Coverage Target:**
- Unit Tests: 85%
- API Tests: 100%
- E2E Tests: 2 tests (draw + type signature)

**Deliverables:**
- ✅ Signature creation working
- ✅ All 3 methods supported (draw, type, upload)
- ✅ Tests passing (15+ unit, 6+ API)

---

### Sprint 5: Apply Signature to PDF (Week 5)
**Goal:** Embed signature image into PDF at specified location

**Features:**
- Take signature image + coordinates
- Embed into PDF using OpenPDF
- Generate signed PDF
- Return modified PDF

**Test Plan:**

**Unit Tests (Critical - This is Core Feature):**
```java
@Test
void testApplySignature_ValidSignature_Success() {
    // Given
    byte[] pdf = loadTestPdf("blank.pdf");
    byte[] signatureImage = loadTestImage("signature.png");
    SignatureData signature = new SignatureData(
        signatureImage,
        1,    // page
        100,  // x
        200,  // y
        200,  // width
        50    // height
    );
    PdfSignatureApplicator applicator = new PdfSignatureApplicator();

    // When
    byte[] signedPdf = applicator.applySignature(pdf, signature);

    // Then
    assertNotNull(signedPdf);
    assertTrue(signedPdf.length > pdf.length); // Image added

    // Verify signature is in PDF
    try (PdfReader reader = new PdfReader(signedPdf)) {
        assertEquals(1, reader.getNumberOfPages());
        // Could check XObject count increased
    }
}

@Test
void testApplySignature_PreservesExistingContent() {
    // Given
    byte[] pdf = loadTestPdf("with-text.pdf");
    String originalText = extractText(pdf);
    byte[] signature = loadTestImage("signature.png");
    SignatureData sigData = new SignatureData(signature, 1, 100, 200, 200, 50);
    PdfSignatureApplicator applicator = new PdfSignatureApplicator();

    // When
    byte[] signedPdf = applicator.applySignature(pdf, sigData);

    // Then
    String newText = extractText(signedPdf);
    assertEquals(originalText, newText); // Text preserved
}

@Test
void testApplySignature_MultipleSignatures_AllApplied() {
    // Given
    byte[] pdf = loadTestPdf("blank.pdf");
    byte[] sig1 = loadTestImage("signature1.png");
    byte[] sig2 = loadTestImage("signature2.png");
    PdfSignatureApplicator applicator = new PdfSignatureApplicator();

    // When
    byte[] result = applicator.applySignature(pdf,
        new SignatureData(sig1, 1, 100, 200, 200, 50));
    result = applicator.applySignature(result,
        new SignatureData(sig2, 1, 100, 300, 200, 50));

    // Then
    assertNotNull(result);
    // Both signatures should be present
}

@Test
void testApplySignature_AddTimestamp() {
    // Given
    byte[] pdf = loadTestPdf("blank.pdf");
    byte[] signature = loadTestImage("signature.png");
    SignatureData sigData = new SignatureData(signature, 1, 100, 200, 200, 50);
    sigData.setAddTimestamp(true);
    PdfSignatureApplicator applicator = new PdfSignatureApplicator();

    // When
    byte[] signedPdf = applicator.applySignature(pdf, sigData);

    // Then
    // Verify timestamp text appears near signature
    String text = extractText(signedPdf);
    assertTrue(text.contains("Signed on:"));
}
```

**Implementation:**
```java
@Service
public class PdfSignatureApplicator {

    public byte[] applySignature(byte[] pdfData, SignatureData signature)
            throws IOException, DocumentException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PdfReader reader = new PdfReader(pdfData)) {
            PdfStamper stamper = new PdfStamper(reader, output);

            // Get the page content
            PdfContentByte canvas = stamper.getOverContent(signature.getPage());

            // Load signature image
            Image signatureImage = Image.getInstance(signature.getImageData());

            // Set position and size
            signatureImage.setAbsolutePosition(signature.getX(), signature.getY());
            signatureImage.scaleToFit(signature.getWidth(), signature.getHeight());

            // Add image to PDF
            canvas.addImage(signatureImage);

            // Add timestamp if requested
            if (signature.isAddTimestamp()) {
                String timestamp = "Signed on: " +
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    new Phrase(timestamp, new Font(Font.HELVETICA, 8)),
                    signature.getX(),
                    signature.getY() - 12,
                    0
                );
            }

            // Add metadata
            stamper.setMoreInfo("Signature Applied", LocalDateTime.now().toString());

            stamper.close();
        }

        return output.toByteArray();
    }
}
```

**API Tests:**
```javascript
describe('POST /api/documents/:id/sign', () => {
  it('should apply signature to PDF', async () => {
    // Given
    const documentId = await uploadTestPdf('blank.pdf');
    const signatureId = await createTestSignature('John Doe');

    const signRequest = {
      signatureId: signatureId,
      fieldId: 'signature1',
      page: 1,
      x: 100,
      y: 200,
      width: 200,
      height: 50
    };

    // When
    const response = await request(app)
      .post(`/api/documents/${documentId}/sign`)
      .send(signRequest)
      .expect(200);

    // Then
    expect(response.body.status).toBe('signed');
    expect(response.body.signedPdfUrl).toBeDefined();

    // Download and verify PDF was modified
    const pdfResponse = await request(app)
      .get(response.body.signedPdfUrl)
      .expect(200);

    expect(pdfResponse.headers['content-type']).toBe('application/pdf');
  });

  it('should reject signing with invalid signature ID', async () => {
    // Given
    const documentId = await uploadTestPdf('blank.pdf');

    // When/Then
    await request(app)
      .post(`/api/documents/${documentId}/sign`)
      .send({
        signatureId: 'invalid-id',
        page: 1,
        x: 100,
        y: 200
      })
      .expect(404);
  });
});
```

**E2E Test (Critical Path):**
```javascript
test('complete signing workflow', async ({ page }) => {
  // Upload document
  await page.goto('/upload');
  await page.setInputFiles('input[type="file"]', 'test.pdf');
  await page.click('button:has-text("Upload")');

  // Wait for upload
  await page.waitForURL('**/editor/**');

  // Create signature
  await page.click('button:has-text("Add Signature")');
  await page.fill('input[name="name"]', 'John Doe');
  await page.click('button:has-text("Create")');

  // Place signature on PDF
  await page.click('.pdf-canvas', { position: { x: 100, y: 200 } });

  // Sign document
  await page.click('button:has-text("Sign Document")');

  // Verify success
  await expect(page.locator('.success-message')).toContainText('Document signed');

  // Download should be available
  const downloadPromise = page.waitForEvent('download');
  await page.click('button:has-text("Download")');
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('.pdf');
});
```

**Coverage Target:**
- Unit Tests: 95% (critical feature)
- API Tests: 100%
- E2E Tests: 1 complete workflow

**Deliverables:**
- ✅ Signature application working
- ✅ PDF generation correct
- ✅ Tests comprehensive (30+ unit, 10+ API, 1 E2E)

---

### Sprint 6: Multi-Party Signing (Week 6)
**Goal:** Support multiple signers for one document

**Features:**
- Assign signature fields to recipients
- Track signing status per recipient
- Email notifications for each signer
- Sequential vs. parallel signing

**Test Plan:**

**Unit Tests:**
```javascript
describe('SigningWorkflow', () => {
  it('should track multiple recipients', () => {
    // Given
    const workflow = new SigningWorkflow('doc123');

    // When
    workflow.addRecipient('alice@example.com', 'Alice', 1);
    workflow.addRecipient('bob@example.com', 'Bob', 2);

    // Then
    expect(workflow.getRecipients()).toHaveLength(2);
    expect(workflow.getStatus()).toBe('pending');
  });

  it('should enforce sequential signing order', () => {
    // Given
    const workflow = new SigningWorkflow('doc123', { mode: 'sequential' });
    workflow.addRecipient('alice@example.com', 'Alice', 1);
    workflow.addRecipient('bob@example.com', 'Bob', 2);

    // When - Bob tries to sign before Alice
    const canBobSign = workflow.canSign('bob@example.com');

    // Then
    expect(canBobSign).toBe(false);
  });

  it('should allow parallel signing', () => {
    // Given
    const workflow = new SigningWorkflow('doc123', { mode: 'parallel' });
    workflow.addRecipient('alice@example.com', 'Alice', 1);
    workflow.addRecipient('bob@example.com', 'Bob', 2);

    // When
    const canAliceSign = workflow.canSign('alice@example.com');
    const canBobSign = workflow.canSign('bob@example.com');

    // Then
    expect(canAliceSign).toBe(true);
    expect(canBobSign).toBe(true);
  });

  it('should mark workflow complete when all signed', () => {
    // Given
    const workflow = new SigningWorkflow('doc123');
    workflow.addRecipient('alice@example.com', 'Alice', 1);
    workflow.addRecipient('bob@example.com', 'Bob', 2);

    // When
    workflow.markSigned('alice@example.com');
    expect(workflow.getStatus()).toBe('in_progress');

    workflow.markSigned('bob@example.com');

    // Then
    expect(workflow.getStatus()).toBe('completed');
  });
});
```

**API Tests:**
```javascript
describe('POST /api/documents/:id/send-for-signature', () => {
  it('should create signing workflow with multiple recipients', async () => {
    // Given
    const documentId = await uploadTestPdf('contract.pdf');
    const workflow = {
      recipients: [
        { email: 'alice@example.com', name: 'Alice', order: 1 },
        { email: 'bob@example.com', name: 'Bob', order: 2 }
      ],
      mode: 'sequential',
      message: 'Please sign this contract'
    };

    // When
    const response = await request(app)
      .post(`/api/documents/${documentId}/send-for-signature`)
      .send(workflow)
      .expect(201);

    // Then
    expect(response.body.workflowId).toBeDefined();
    expect(response.body.status).toBe('sent');
  });

  it('should send email to first recipient in sequential mode', async () => {
    // Given
    const documentId = await uploadTestPdf('contract.pdf');
    const workflow = {
      recipients: [
        { email: 'alice@example.com', name: 'Alice', order: 1 },
        { email: 'bob@example.com', name: 'Bob', order: 2 }
      ],
      mode: 'sequential'
    };

    // When
    await request(app)
      .post(`/api/documents/${documentId}/send-for-signature`)
      .send(workflow)
      .expect(201);

    // Then
    const emails = await getTestEmails();
    expect(emails).toHaveLength(1);
    expect(emails[0].to).toBe('alice@example.com');
  });
});

describe('POST /api/signing/:token/sign', () => {
  it('should allow recipient to sign with valid token', async () => {
    // Given
    const { documentId, token } = await createSigningWorkflow([
      { email: 'alice@example.com', name: 'Alice' }
    ]);
    const signatureId = await createTestSignature('Alice');

    // When
    const response = await request(app)
      .post(`/api/signing/${token}/sign`)
      .send({ signatureId })
      .expect(200);

    // Then
    expect(response.body.status).toBe('signed');
  });

  it('should reject expired signing token', async () => {
    // Given
    const expiredToken = 'expired-token-123';

    // When/Then
    await request(app)
      .post(`/api/signing/${expiredToken}/sign`)
      .send({ signatureId: 'sig123' })
      .expect(401);
  });
});
```

**Coverage Target:**
- Unit Tests: 90%
- API Tests: 100%
- E2E Tests: 1 multi-party workflow

**Deliverables:**
- ✅ Multi-party workflows working
- ✅ Email notifications sending
- ✅ Sequential/parallel modes
- ✅ Tests passing (25+ unit, 12+ API)

---

### Sprint 7: Authentication & Authorization (Week 7)
**Goal:** User accounts, login, JWT tokens

**Features:**
- User registration
- Login (email + password)
- JWT token generation
- Protected routes
- Password hashing (bcrypt)

**Test Plan:**

**Unit Tests:**
```javascript
describe('AuthService', () => {
  it('should hash password on registration', async () => {
    // Given
    const authService = new AuthService();
    const plainPassword = 'password123';

    // When
    const hashedPassword = await authService.hashPassword(plainPassword);

    // Then
    expect(hashedPassword).not.toBe(plainPassword);
    expect(hashedPassword.length).toBeGreaterThan(50);
  });

  it('should validate correct password', async () => {
    // Given
    const authService = new AuthService();
    const plainPassword = 'password123';
    const hashedPassword = await authService.hashPassword(plainPassword);

    // When
    const isValid = await authService.verifyPassword(plainPassword, hashedPassword);

    // Then
    expect(isValid).toBe(true);
  });

  it('should reject incorrect password', async () => {
    // Given
    const authService = new AuthService();
    const hashedPassword = await authService.hashPassword('password123');

    // When
    const isValid = await authService.verifyPassword('wrong', hashedPassword);

    // Then
    expect(isValid).toBe(false);
  });

  it('should generate valid JWT token', () => {
    // Given
    const authService = new AuthService();
    const userId = 'user123';

    // When
    const token = authService.generateToken(userId);

    // Then
    expect(token).toBeDefined();
    const decoded = authService.verifyToken(token);
    expect(decoded.userId).toBe(userId);
  });

  it('should reject expired token', () => {
    // Given
    const authService = new AuthService({ tokenExpiry: '1ms' });
    const token = authService.generateToken('user123');

    // When - wait for expiry
    setTimeout(() => {
      expect(() => authService.verifyToken(token))
        .toThrow('Token expired');
    }, 10);
  });
});
```

**API Tests:**
```javascript
describe('POST /api/auth/register', () => {
  it('should register new user', async () => {
    // Given
    const userData = {
      email: 'test@example.com',
      password: 'securepassword123',
      name: 'Test User'
    };

    // When
    const response = await request(app)
      .post('/api/auth/register')
      .send(userData)
      .expect(201);

    // Then
    expect(response.body.userId).toBeDefined();
    expect(response.body.token).toBeDefined();
    expect(response.body.email).toBe(userData.email);
  });

  it('should reject duplicate email', async () => {
    // Given
    await createTestUser('existing@example.com');

    // When/Then
    await request(app)
      .post('/api/auth/register')
      .send({ email: 'existing@example.com', password: 'pass123' })
      .expect(409); // Conflict
  });

  it('should reject weak password', async () => {
    // When/Then
    await request(app)
      .post('/api/auth/register')
      .send({ email: 'test@example.com', password: '123' })
      .expect(400);
  });
});

describe('POST /api/auth/login', () => {
  it('should login with valid credentials', async () => {
    // Given
    await createTestUser('user@example.com', 'password123');

    // When
    const response = await request(app)
      .post('/api/auth/login')
      .send({ email: 'user@example.com', password: 'password123' })
      .expect(200);

    // Then
    expect(response.body.token).toBeDefined();
  });

  it('should reject invalid password', async () => {
    // Given
    await createTestUser('user@example.com', 'password123');

    // When/Then
    await request(app)
      .post('/api/auth/login')
      .send({ email: 'user@example.com', password: 'wrong' })
      .expect(401);
  });
});

describe('Protected routes', () => {
  it('should allow access with valid token', async () => {
    // Given
    const token = await loginAndGetToken('user@example.com', 'password123');

    // When
    const response = await request(app)
      .get('/api/documents')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);
  });

  it('should reject requests without token', async () => {
    // When/Then
    await request(app)
      .get('/api/documents')
      .expect(401);
  });

  it('should reject invalid token', async () => {
    // When/Then
    await request(app)
      .get('/api/documents')
      .set('Authorization', 'Bearer invalid-token')
      .expect(401);
  });
});
```

**Coverage Target:**
- Unit Tests: 95%
- API Tests: 100%
- E2E Tests: 1 login flow

**Deliverables:**
- ✅ Authentication working
- ✅ JWT tokens implemented
- ✅ Protected routes
- ✅ Tests passing (20+ unit, 15+ API)

---

### Sprint 8: Document Management (Week 8)
**Goal:** List, view, delete documents per user

**Features:**
- List user's documents
- Filter by status (draft, sent, completed)
- Search documents
- Delete documents
- Document permissions

**Test Plan:**

**Unit Tests:**
```javascript
describe('DocumentService', () => {
  it('should return only user documents', async () => {
    // Given
    const service = new DocumentService();
    await createTestDocument('user1', 'doc1.pdf');
    await createTestDocument('user1', 'doc2.pdf');
    await createTestDocument('user2', 'doc3.pdf');

    // When
    const docs = await service.getDocuments('user1');

    // Then
    expect(docs).toHaveLength(2);
    expect(docs.every(d => d.userId === 'user1')).toBe(true);
  });

  it('should filter documents by status', async () => {
    // Given
    const service = new DocumentService();
    await createTestDocument('user1', 'draft.pdf', { status: 'draft' });
    await createTestDocument('user1', 'sent.pdf', { status: 'sent' });
    await createTestDocument('user1', 'complete.pdf', { status: 'completed' });

    // When
    const sentDocs = await service.getDocuments('user1', { status: 'sent' });

    // Then
    expect(sentDocs).toHaveLength(1);
    expect(sentDocs[0].status).toBe('sent');
  });

  it('should prevent access to other user documents', async () => {
    // Given
    const service = new DocumentService();
    const doc = await createTestDocument('user1', 'private.pdf');

    // When/Then
    await expect(service.getDocument(doc.id, 'user2'))
      .rejects.toThrow('Access denied');
  });
});
```

**API Tests:**
```javascript
describe('GET /api/documents', () => {
  it('should return user documents', async () => {
    // Given
    const token = await loginAndGetToken('user@example.com');
    await createTestDocument('user@example.com', 'doc1.pdf');
    await createTestDocument('user@example.com', 'doc2.pdf');

    // When
    const response = await request(app)
      .get('/api/documents')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    // Then
    expect(response.body.documents).toHaveLength(2);
  });

  it('should support pagination', async () => {
    // Given
    const token = await loginAndGetToken('user@example.com');
    for (let i = 0; i < 25; i++) {
      await createTestDocument('user@example.com', `doc${i}.pdf`);
    }

    // When
    const response = await request(app)
      .get('/api/documents?page=2&limit=10')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    // Then
    expect(response.body.documents).toHaveLength(10);
    expect(response.body.page).toBe(2);
    expect(response.body.totalPages).toBe(3);
  });
});

describe('DELETE /api/documents/:id', () => {
  it('should delete own document', async () => {
    // Given
    const token = await loginAndGetToken('user@example.com');
    const docId = await createTestDocument('user@example.com', 'delete-me.pdf');

    // When
    await request(app)
      .delete(`/api/documents/${docId}`)
      .set('Authorization', `Bearer ${token}`)
      .expect(204);

    // Then - document should be gone
    await request(app)
      .get(`/api/documents/${docId}`)
      .set('Authorization', `Bearer ${token}`)
      .expect(404);
  });

  it('should prevent deleting other user document', async () => {
    // Given
    const user1Token = await loginAndGetToken('user1@example.com');
    const user2Token = await loginAndGetToken('user2@example.com');
    const docId = await createTestDocument('user1@example.com', 'private.pdf');

    // When/Then
    await request(app)
      .delete(`/api/documents/${docId}`)
      .set('Authorization', `Bearer ${user2Token}`)
      .expect(403);
  });
});
```

**Coverage Target:**
- Unit Tests: 85%
- API Tests: 100%
- E2E Tests: 0 (covered by other flows)

---

### Sprint 9: Email Notifications (Week 9)
**Goal:** Send emails for signing requests, reminders, completions

**Features:**
- Send signing request emails
- Email templates
- Reminder emails
- Completion notifications
- Email tracking (opened, clicked)

**Test Plan:**

**Unit Tests:**
```javascript
describe('EmailService', () => {
  it('should generate signing request email', () => {
    // Given
    const emailService = new EmailService();
    const data = {
      recipientName: 'Alice',
      senderName: 'Bob',
      documentName: 'Contract.pdf',
      signingUrl: 'https://app.com/sign/abc123'
    };

    // When
    const email = emailService.generateSigningRequest(data);

    // Then
    expect(email.subject).toContain('Contract.pdf');
    expect(email.html).toContain('Alice');
    expect(email.html).toContain(data.signingUrl);
  });

  it('should track email opens', async () => {
    // Given
    const emailService = new EmailService();
    const emailId = await emailService.send({
      to: 'test@example.com',
      subject: 'Test',
      html: '<p>Test</p>'
    });

    // When
    await emailService.trackOpen(emailId);

    // Then
    const status = await emailService.getStatus(emailId);
    expect(status.opened).toBe(true);
    expect(status.openedAt).toBeDefined();
  });

  it('should not send duplicate emails within 24 hours', async () => {
    // Given
    const emailService = new EmailService();
    await emailService.send({
      to: 'test@example.com',
      type: 'signing_request',
      documentId: 'doc123'
    });

    // When - try to send again immediately
    const sent = await emailService.send({
      to: 'test@example.com',
      type: 'signing_request',
      documentId: 'doc123'
    });

    // Then
    expect(sent).toBe(false);
  });
});
```

**API Tests:**
```javascript
describe('Email notifications', () => {
  it('should send email when document sent for signature', async () => {
    // Given
    const token = await loginAndGetToken('sender@example.com');
    const documentId = await uploadTestPdf('contract.pdf');

    // When
    await request(app)
      .post(`/api/documents/${documentId}/send-for-signature`)
      .set('Authorization', `Bearer ${token}`)
      .send({
        recipients: [{ email: 'signer@example.com', name: 'Alice' }]
      })
      .expect(201);

    // Then
    const emails = await getTestEmails();
    expect(emails).toHaveLength(1);
    expect(emails[0].to).toBe('signer@example.com');
    expect(emails[0].subject).toContain('contract.pdf');
  });

  it('should send completion email to all parties', async () => {
    // Given
    const workflow = await createSigningWorkflow([
      { email: 'signer@example.com', name: 'Alice' }
    ]);

    // When - complete signing
    await completeSigningWorkflow(workflow.id);

    // Then
    const emails = await getTestEmails();
    const completionEmails = emails.filter(e =>
      e.subject.includes('completed')
    );
    expect(completionEmails.length).toBeGreaterThan(0);
  });
});
```

**Coverage Target:**
- Unit Tests: 80%
- API Tests: 100% (email sending scenarios)
- E2E Tests: 0 (hard to test email in E2E)

---

### Sprint 10: Audit Trail & Compliance (Week 10)
**Goal:** Track all actions, generate audit logs, compliance features

**Features:**
- Log all document actions
- IP address tracking
- Timestamp all signatures
- Generate audit trail PDF
- Tamper-proof hashes

**Test Plan:**

**Unit Tests:**
```java
@Test
void testAuditLog_RecordsAction() {
    // Given
    AuditLogger logger = new AuditLogger();
    AuditEvent event = new AuditEvent(
        "DOCUMENT_SIGNED",
        "user123",
        "doc456",
        "192.168.1.1"
    );

    // When
    logger.log(event);

    // Then
    List<AuditEvent> events = logger.getEvents("doc456");
    assertEquals(1, events.size());
    assertEquals("DOCUMENT_SIGNED", events.get(0).getAction());
}

@Test
void testAuditTrail_GeneratesPdf() {
    // Given
    AuditLogger logger = new AuditLogger();
    logger.log(new AuditEvent("DOCUMENT_CREATED", "user1", "doc1", "192.168.1.1"));
    logger.log(new AuditEvent("DOCUMENT_SIGNED", "user2", "doc1", "192.168.1.2"));
    AuditTrailGenerator generator = new AuditTrailGenerator();

    // When
    byte[] auditPdf = generator.generatePdf("doc1");

    // Then
    assertNotNull(auditPdf);
    assertTrue(auditPdf.length > 0);

    // Verify it's a valid PDF
    try (PdfReader reader = new PdfReader(auditPdf)) {
        assertTrue(reader.getNumberOfPages() > 0);
    }
}

@Test
void testDocumentHash_DetectsTampering() {
    // Given
    byte[] originalPdf = loadTestPdf("contract.pdf");
    DocumentHasher hasher = new DocumentHasher();
    String originalHash = hasher.hash(originalPdf);

    // When - modify PDF
    byte[] modifiedPdf = modifyPdf(originalPdf);
    String modifiedHash = hasher.hash(modifiedPdf);

    // Then
    assertNotEquals(originalHash, modifiedHash);
}
```

**API Tests:**
```javascript
describe('GET /api/documents/:id/audit-trail', () => {
  it('should return audit trail for document', async () => {
    // Given
    const token = await loginAndGetToken('user@example.com');
    const docId = await uploadTestPdf('contract.pdf');
    await signDocument(docId, 'signer@example.com');

    // When
    const response = await request(app)
      .get(`/api/documents/${docId}/audit-trail`)
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    // Then
    expect(response.body.events).toHaveLength(2); // upload + sign
    expect(response.body.events[0].action).toBe('DOCUMENT_UPLOADED');
    expect(response.body.events[1].action).toBe('DOCUMENT_SIGNED');
  });

  it('should include IP addresses in audit trail', async () => {
    // Given
    const token = await loginAndGetToken('user@example.com');
    const docId = await uploadTestPdf('contract.pdf');

    // When
    const response = await request(app)
      .get(`/api/documents/${docId}/audit-trail`)
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    // Then
    expect(response.body.events[0].ipAddress).toBeDefined();
  });
});

describe('GET /api/documents/:id/audit-trail/pdf', () => {
  it('should generate audit trail PDF', async () => {
    // Given
    const token = await loginAndGetToken('user@example.com');
    const docId = await uploadTestPdf('contract.pdf');

    // When
    const response = await request(app)
      .get(`/api/documents/${docId}/audit-trail/pdf`)
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    // Then
    expect(response.headers['content-type']).toBe('application/pdf');
  });
});
```

**Coverage Target:**
- Unit Tests: 90%
- API Tests: 100%
- E2E Tests: 0

---

### Sprint 11: Performance & Optimization (Week 11)
**Goal:** Optimize slow operations, add caching, improve response times

**Features:**
- Cache PDF metadata
- Background job processing (Bull/BullMQ)
- Database indexing
- Response compression
- CDN for static assets

**Test Plan:**

**Performance Tests:**
```javascript
describe('Performance tests', () => {
  it('should handle large PDF upload within 5 seconds', async () => {
    // Given
    const largePdf = generateTestPdf(8 * 1024 * 1024); // 8MB
    const startTime = Date.now();

    // When
    await request(app)
      .post('/api/documents/upload')
      .attach('file', largePdf, 'large.pdf')
      .expect(201);

    // Then
    const duration = Date.now() - startTime;
    expect(duration).toBeLessThan(5000);
  });

  it('should return cached metadata quickly', async () => {
    // Given
    const docId = await uploadTestPdf('contract.pdf');
    await request(app).get(`/api/documents/${docId}/metadata`); // Prime cache

    // When
    const startTime = Date.now();
    await request(app)
      .get(`/api/documents/${docId}/metadata`)
      .expect(200);
    const duration = Date.now() - startTime;

    // Then
    expect(duration).toBeLessThan(100); // Should be fast from cache
  });

  it('should handle 100 concurrent requests', async () => {
    // Given
    const requests = [];
    for (let i = 0; i < 100; i++) {
      requests.push(
        request(app).get('/api/health').expect(200)
      );
    }

    // When
    const startTime = Date.now();
    await Promise.all(requests);
    const duration = Date.now() - startTime;

    // Then
    expect(duration).toBeLessThan(5000);
  });
});
```

**Load Tests (Artillery/K6):**
```yaml
# load-test.yml
config:
  target: 'http://localhost:3000'
  phases:
    - duration: 60
      arrivalRate: 10
scenarios:
  - name: 'Upload and sign document'
    flow:
      - post:
          url: '/api/auth/login'
          json:
            email: 'test@example.com'
            password: 'password123'
          capture:
            - json: '$.token'
              as: 'token'
      - post:
          url: '/api/documents/upload'
          headers:
            Authorization: 'Bearer {{ token }}'
          formData:
            file: '@test.pdf'
```

**Coverage Target:**
- Performance Tests: 5-10 critical paths
- Load Tests: 1 scenario

---

### Sprint 12: Final Integration & Launch Prep (Week 12)
**Goal:** Bug fixes, polish, deployment preparation

**Tasks:**
- [ ] Fix all critical bugs
- [ ] Increase test coverage to >70%
- [ ] Security audit (OWASP top 10)
- [ ] Performance testing
- [ ] Documentation (API docs, user guide)
- [ ] Deployment scripts
- [ ] Monitoring setup (Sentry, DataDog)
- [ ] Backup strategy

**Test Plan:**

**Security Tests:**
```javascript
describe('Security tests', () => {
  it('should prevent SQL injection', async () => {
    // When/Then
    await request(app)
      .get('/api/documents?search=\' OR 1=1--')
      .expect(400); // Should reject malicious input
  });

  it('should prevent XSS in document names', async () => {
    // Given
    const maliciousName = '<script>alert("xss")</script>.pdf';

    // When
    const response = await request(app)
      .post('/api/documents/upload')
      .attach('file', testPdfBuffer, maliciousName)
      .expect(201);

    // Then
    expect(response.body.fileName).not.toContain('<script>');
  });

  it('should enforce rate limiting', async () => {
    // Given
    const requests = [];
    for (let i = 0; i < 150; i++) {
      requests.push(request(app).get('/api/health'));
    }

    // When
    const responses = await Promise.all(requests);

    // Then
    const rateLimited = responses.filter(r => r.status === 429);
    expect(rateLimited.length).toBeGreaterThan(0);
  });
});
```

---

## Testing Strategy

### Test Pyramid

```
         /\
        /  \     5-10 E2E Tests (Critical paths only)
       /────\
      /      \   50-100 API/Integration Tests
     /────────\
    /          \ 200-400 Unit Tests (70% of all tests)
   /────────────\
```

### Coverage Targets

| Layer | Target | Why |
|-------|--------|-----|
| Unit Tests | 70-80% | Fast, reliable, catch regressions |
| API Tests | 100% endpoints | Ensure contracts work |
| E2E Tests | 5-10 critical paths | Expensive, fragile, minimal |

### Test Categories

**1. Unit Tests (70% of tests)**
```
- Business logic
- Validation functions
- Utility functions
- Data transformations
```

**2. Integration Tests (25% of tests)**
```
- API endpoints
- Database operations
- External service calls
- File operations
```

**3. E2E Tests (5% of tests)**
```
- Complete user workflows
- Critical business paths only
```

---

## Tech Stack Details

### Backend (Java - PDF Service)

```xml
<!-- pom.xml -->
<dependencies>
    <!-- OpenPDF -->
    <dependency>
        <groupId>com.github.librepdf</groupId>
        <artifactId>openpdf</artifactId>
        <version>3.0.0</version>
    </dependency>

    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Backend (Node.js - API Service)

```json
{
  "dependencies": {
    "express": "^4.18.0",
    "mongoose": "^8.0.0",
    "jsonwebtoken": "^9.0.0",
    "bcrypt": "^5.1.0",
    "multer": "^1.4.5-lts.1",
    "bull": "^4.11.0",
    "nodemailer": "^6.9.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "jest": "^29.7.0",
    "supertest": "^6.3.0",
    "@faker-js/faker": "^8.3.0",
    "mongodb-memory-server": "^9.1.0"
  }
}
```

### Frontend (React)

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "axios": "^1.6.0",
    "pdfjs-dist": "^3.11.0",
    "zustand": "^4.4.0"
  },
  "devDependencies": {
    "vitest": "^1.0.0",
    "@testing-library/react": "^14.1.0",
    "@playwright/test": "^1.40.0"
  }
}
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test-java:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run tests
        run: |
          cd services/pdf-service
          mvn clean test
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  test-node:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - name: Install dependencies
        run: |
          cd services/api-service
          npm ci
      - name: Run tests
        run: npm test
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - name: Install Playwright
        run: npx playwright install --with-deps
      - name: Start services
        run: docker-compose up -d
      - name: Run E2E tests
        run: npm run test:e2e
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
```

---

## Development Workflow

### Day-to-Day Process

**1. Pick a task from sprint plan**
```bash
# Create feature branch
git checkout -b feature/sprint-3-signature-placement
```

**2. Write tests first (TDD)**
```java
// Write failing test
@Test
void testAddSignatureField_ValidCoordinates_Success() {
    // Test implementation
}
```

**3. Run test (should fail)**
```bash
mvn test -Dtest=PdfSignatureServiceTest
# Test fails ✓ (expected)
```

**4. Implement feature**
```java
public byte[] addSignatureField(byte[] pdf, SignatureField field) {
    // Implementation
}
```

**5. Run test (should pass)**
```bash
mvn test -Dtest=PdfSignatureServiceTest
# Test passes ✓
```

**6. Check coverage**
```bash
mvn jacoco:report
open target/site/jacoco/index.html
# Verify >70% coverage
```

**7. Commit**
```bash
git add .
git commit -m "Add signature field placement with tests"
git push origin feature/sprint-3-signature-placement
```

**8. Create PR**
- CI runs all tests
- Code review
- Merge when green ✓

---

## Summary

**12-Week MVP Plan:**

| Sprint | Feature | Tests | Status |
|--------|---------|-------|--------|
| 0 | Project setup | - | |
| 1 | PDF upload | 35 tests | |
| 2 | Metadata extraction | 25 tests | |
| 3 | Signature placement | 33 tests | |
| 4 | Signature creation | 21 tests | |
| 5 | Apply signature | 40 tests | |
| 6 | Multi-party signing | 37 tests | |
| 7 | Authentication | 35 tests | |
| 8 | Document management | 25 tests | |
| 9 | Email notifications | 20 tests | |
| 10 | Audit trail | 25 tests | |
| 11 | Performance | 15 tests | |
| 12 | Launch prep | 20 tests | |

**Total: ~331 tests minimum**

---

## Next Steps

**Today:**
1. Set up project structure
2. Initialize Git repository
3. Configure CI/CD

**This Week:**
1. Complete Sprint 0 (setup)
2. Start Sprint 1 (PDF upload)
3. Write first 10 unit tests

**This Month:**
1. Complete Sprints 1-4
2. Have basic PDF signing working
3. 100+ tests passing

**3 Months:**
1. Complete all 12 sprints
2. 300+ tests passing
3. MVP deployed to production
4. First beta users signing documents

---

**Ready to start? Let's begin with Sprint 0!**
