# Online PDF Editor Architecture Using OpenPDF

This document outlines how to build an online PDF editing website using OpenPDF.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                         Web Browser                              │
│                                                                  │
│  ┌────────────┐  ┌─────────────┐  ┌──────────────┐             │
│  │  PDF.js    │  │  React/Vue  │  │  File Upload │             │
│  │  Viewer    │  │  Interface  │  │  Component   │             │
│  └────────────┘  └─────────────┘  └──────────────┘             │
└────────────────────────┬─────────────────────────────────────────┘
                         │ REST API (JSON/FormData)
                         │
┌────────────────────────▼─────────────────────────────────────────┐
│                   Java Backend Server                            │
│                   (Spring Boot)                                  │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  REST Controllers                                        │   │
│  │  - /api/upload          - Upload PDF                     │   │
│  │  - /api/fill-form       - Fill form fields               │   │
│  │  - /api/merge           - Merge PDFs                     │   │
│  │  - /api/split           - Split PDF                      │   │
│  │  - /api/add-watermark   - Add watermark                  │   │
│  │  - /api/html-to-pdf     - Convert HTML to PDF            │   │
│  │  - /api/encrypt         - Encrypt PDF                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                         │                                        │
│  ┌─────────────────────▼───────────────────────────────────┐   │
│  │  Service Layer (OpenPDF Integration)                    │   │
│  │  - PdfFormService                                        │   │
│  │  - PdfMergeService                                       │   │
│  │  - PdfWatermarkService                                   │   │
│  │  - HtmlToPdfService                                      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                         │                                        │
│  ┌─────────────────────▼───────────────────────────────────┐   │
│  │  OpenPDF Library                                         │   │
│  │  - PdfWriter, PdfReader, PdfStamper                      │   │
│  │  - AcroFields, PdfCopy, ITextRenderer                    │   │
│  └─────────────────────────────────────────────────────────┘   │
└────────────────────────┬─────────────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────────────┐
│                  Storage Layer                                   │
│  - Local filesystem / S3 / Azure Blob / Google Cloud Storage    │
│  - Database for metadata (PostgreSQL, MongoDB)                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Feature Implementation Examples

### 1. Fill PDF Forms

**Backend Service** (`PdfFormService.java`):

```java
@Service
public class PdfFormService {

    public byte[] fillForm(MultipartFile pdfFile, Map<String, String> formData)
            throws IOException, DocumentException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PdfReader reader = new PdfReader(pdfFile.getInputStream())) {
            PdfStamper stamper = new PdfStamper(reader, output);
            AcroFields form = stamper.getAcroFields();

            // Fill each form field
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                form.setField(entry.getKey(), entry.getValue());
            }

            stamper.setFormFlattening(true); // Make read-only
            stamper.close();
        }

        return output.toByteArray();
    }

    public Map<String, String> getFormFields(MultipartFile pdfFile)
            throws IOException {

        Map<String, String> fields = new HashMap<>();

        try (PdfReader reader = new PdfReader(pdfFile.getInputStream())) {
            AcroFields form = reader.getAcroFields();

            for (String fieldName : form.getFields().keySet()) {
                fields.put(fieldName, form.getField(fieldName));
            }
        }

        return fields;
    }
}
```

**REST Controller** (`PdfFormController.java`):

```java
@RestController
@RequestMapping("/api/forms")
public class PdfFormController {

    @Autowired
    private PdfFormService formService;

    @PostMapping("/get-fields")
    public ResponseEntity<Map<String, String>> getFields(
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, String> fields = formService.getFormFields(file);
            return ResponseEntity.ok(fields);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/fill")
    public ResponseEntity<byte[]> fillForm(
            @RequestParam("file") MultipartFile file,
            @RequestBody Map<String, String> formData) {
        try {
            byte[] pdfBytes = formService.fillForm(file, formData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "filled-form.pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
```

**Frontend (React)**:

```javascript
import React, { useState } from 'react';
import axios from 'axios';

function PdfFormEditor() {
    const [pdfFile, setPdfFile] = useState(null);
    const [formFields, setFormFields] = useState({});
    const [formValues, setFormValues] = useState({});

    const handleFileUpload = async (event) => {
        const file = event.target.files[0];
        setPdfFile(file);

        // Get form fields from PDF
        const formData = new FormData();
        formData.append('file', file);

        const response = await axios.post('/api/forms/get-fields', formData);
        setFormFields(response.data);
    };

    const handleFieldChange = (fieldName, value) => {
        setFormValues({ ...formValues, [fieldName]: value });
    };

    const handleSubmit = async () => {
        const formData = new FormData();
        formData.append('file', pdfFile);

        const response = await axios.post('/api/forms/fill', formValues, {
            params: { file: pdfFile },
            responseType: 'blob'
        });

        // Download filled PDF
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'filled-form.pdf');
        document.body.appendChild(link);
        link.click();
    };

    return (
        <div>
            <h1>PDF Form Editor</h1>
            <input type="file" accept=".pdf" onChange={handleFileUpload} />

            {Object.keys(formFields).length > 0 && (
                <div>
                    <h2>Fill Form Fields:</h2>
                    {Object.keys(formFields).map(fieldName => (
                        <div key={fieldName}>
                            <label>{fieldName}:</label>
                            <input
                                type="text"
                                value={formValues[fieldName] || ''}
                                onChange={(e) => handleFieldChange(fieldName, e.target.value)}
                            />
                        </div>
                    ))}
                    <button onClick={handleSubmit}>Fill & Download PDF</button>
                </div>
            )}
        </div>
    );
}

export default PdfFormEditor;
```

---

### 2. Merge PDFs

**Backend Service** (`PdfMergeService.java`):

```java
@Service
public class PdfMergeService {

    public byte[] mergePdfs(List<MultipartFile> pdfFiles)
            throws IOException, DocumentException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        PdfCopy copy = new PdfCopy(document, output);

        document.open();

        for (MultipartFile file : pdfFiles) {
            try (PdfReader reader = new PdfReader(file.getInputStream())) {
                int numPages = reader.getNumberOfPages();

                for (int page = 1; page <= numPages; page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }
            }
        }

        document.close();
        return output.toByteArray();
    }
}
```

**REST Controller** (`PdfMergeController.java`):

```java
@RestController
@RequestMapping("/api/merge")
public class PdfMergeController {

    @Autowired
    private PdfMergeService mergeService;

    @PostMapping
    public ResponseEntity<byte[]> mergePdfs(
            @RequestParam("files") List<MultipartFile> files) {
        try {
            byte[] mergedPdf = mergeService.mergePdfs(files);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "merged.pdf");

            return new ResponseEntity<>(mergedPdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
```

---

### 3. Add Watermark

**Backend Service** (`PdfWatermarkService.java`):

```java
@Service
public class PdfWatermarkService {

    public byte[] addWatermark(MultipartFile pdfFile, String watermarkText)
            throws IOException, DocumentException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PdfReader reader = new PdfReader(pdfFile.getInputStream())) {
            PdfStamper stamper = new PdfStamper(reader, output);
            int numPages = reader.getNumberOfPages();

            Font watermarkFont = new Font(Font.HELVETICA, 60, Font.BOLD,
                new Color(192, 192, 192, 128)); // Semi-transparent gray

            for (int page = 1; page <= numPages; page++) {
                PdfContentByte canvas = stamper.getOverContent(page);
                Rectangle pageSize = reader.getPageSize(page);

                float x = pageSize.getWidth() / 2;
                float y = pageSize.getHeight() / 2;

                // Add watermark text
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(watermarkText, watermarkFont), x, y, 45);
            }

            stamper.close();
        }

        return output.toByteArray();
    }
}
```

---

### 4. HTML to PDF Conversion

**Backend Service** (`HtmlToPdfService.java`):

```java
@Service
public class HtmlToPdfService {

    public byte[] convertHtmlToPdf(String htmlContent)
            throws DocumentException, IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(output);

        return output.toByteArray();
    }

    public byte[] convertUrlToPdf(String url)
            throws DocumentException, IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocument(url);
        renderer.layout();
        renderer.createPDF(output);

        return output.toByteArray();
    }
}
```

**REST Controller** (`HtmlToPdfController.java`):

```java
@RestController
@RequestMapping("/api/html-to-pdf")
public class HtmlToPdfController {

    @Autowired
    private HtmlToPdfService htmlToPdfService;

    @PostMapping
    public ResponseEntity<byte[]> convertHtml(@RequestBody String htmlContent) {
        try {
            byte[] pdfBytes = htmlToPdfService.convertHtmlToPdf(htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "document.pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
```

---

### 5. Split PDF

**Backend Service** (`PdfSplitService.java`):

```java
@Service
public class PdfSplitService {

    public List<byte[]> splitPdf(MultipartFile pdfFile, int[] pageRanges)
            throws IOException, DocumentException {

        List<byte[]> splitPdfs = new ArrayList<>();

        try (PdfReader reader = new PdfReader(pdfFile.getInputStream())) {
            for (int i = 0; i < pageRanges.length; i += 2) {
                int startPage = pageRanges[i];
                int endPage = pageRanges[i + 1];

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Document document = new Document();
                PdfCopy copy = new PdfCopy(document, output);
                document.open();

                for (int page = startPage; page <= endPage; page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }

                document.close();
                splitPdfs.add(output.toByteArray());
            }
        }

        return splitPdfs;
    }
}
```

---

## Frontend Technologies

### PDF Viewer Integration (PDF.js)

```javascript
import React, { useEffect, useRef } from 'react';
import * as pdfjsLib from 'pdfjs-dist';

function PdfViewer({ pdfUrl }) {
    const canvasRef = useRef(null);

    useEffect(() => {
        const loadPdf = async () => {
            const pdf = await pdfjsLib.getDocument(pdfUrl).promise;
            const page = await pdf.getPage(1);

            const viewport = page.getViewport({ scale: 1.5 });
            const canvas = canvasRef.current;
            const context = canvas.getContext('2d');

            canvas.height = viewport.height;
            canvas.width = viewport.width;

            await page.render({ canvasContext: context, viewport }).promise;
        };

        loadPdf();
    }, [pdfUrl]);

    return <canvas ref={canvasRef} />;
}
```

---

## Deployment Architecture

### Production Setup

```yaml
# Docker Compose Example

version: '3.8'

services:
  # Java Backend with OpenPDF
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DATABASE_URL=jdbc:postgresql://db:5432/pdfeditor
      - S3_BUCKET_NAME=pdf-storage
    volumes:
      - pdf-temp:/tmp/pdfs
    depends_on:
      - db

  # React Frontend
  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

  # PostgreSQL for metadata
  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=pdfeditor
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=secure_password
    volumes:
      - postgres-data:/var/lib/postgresql/data

  # Redis for caching
  redis:
    image: redis:7
    ports:
      - "6379:6379"

volumes:
  pdf-temp:
  postgres-data:
```

---

## Security Considerations

### 1. File Upload Validation

```java
@Component
public class PdfValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public void validatePdfFile(MultipartFile file) throws InvalidFileException {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File too large");
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType)) {
            throw new InvalidFileException("Not a PDF file");
        }

        // Validate PDF structure
        try (PdfReader reader = new PdfReader(file.getInputStream())) {
            // If we can open it, it's a valid PDF
        } catch (Exception e) {
            throw new InvalidFileException("Invalid PDF structure");
        }
    }
}
```

### 2. Rate Limiting

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();

        if (!rateLimiter.tryAcquire(clientIp)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

### 3. Temporary File Cleanup

```java
@Service
public class TempFileCleanupService {

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldFiles() {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));

        try (Stream<Path> files = Files.walk(tempDir)) {
            files.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".pdf"))
                 .filter(this::isOlderThanOneHour)
                 .forEach(this::deleteQuietly);
        } catch (IOException e) {
            log.error("Failed to cleanup temp files", e);
        }
    }

    private boolean isOlderThanOneHour(Path path) {
        try {
            FileTime fileTime = Files.getLastModifiedTime(path);
            return fileTime.toMillis() < System.currentTimeMillis() - 3600000;
        } catch (IOException e) {
            return false;
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            log.warn("Failed to delete file: " + path, e);
        }
    }
}
```

---

## Performance Optimization

### 1. Async Processing for Large PDFs

```java
@Service
public class AsyncPdfProcessingService {

    @Async
    public CompletableFuture<byte[]> processLargePdf(MultipartFile file) {
        // Process PDF in background thread
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Heavy PDF processing
                return processPdf(file);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
}
```

### 2. Caching

```java
@Service
public class CachedPdfService {

    @Cacheable(value = "pdf-cache", key = "#fileHash")
    public byte[] getProcessedPdf(String fileHash, MultipartFile file) {
        // Cache processed PDFs to avoid reprocessing
        return processPdf(file);
    }
}
```

---

## Example Features You Can Build

1. **PDF Form Editor** ✅
2. **PDF Merger** ✅
3. **PDF Splitter** ✅
4. **Watermark Tool** ✅
5. **HTML to PDF Converter** ✅
6. **PDF Encryption Tool** ✅
7. **Digital Signature Tool** ✅
8. **PDF Compression**
9. **Page Rotation**
10. **Text Extraction**
11. **Invoice Generator**
12. **Certificate Generator**
13. **Resume Builder**
14. **Contract Builder**

---

## Limitations to Consider

### What OpenPDF CANNOT Do Easily:

1. **Visual Page Editor** - Moving text/images around like Adobe Acrobat
2. **OCR** - Requires external libraries (Tesseract)
3. **Advanced Graphics Editing** - Complex vector graphics manipulation
4. **Real-time Collaborative Editing** - Would need WebSockets + complex state management
5. **Client-side Only** - Requires server (can't run pure JavaScript in browser)

### Solutions:

- **For visual editing**: Consider additional libraries like Apache PDFBox or commercial solutions
- **For OCR**: Integrate Tesseract OCR
- **For client-side**: Use PDF.js for viewing, OpenPDF for server-side processing

---

## Conclusion

**YES, you can build an online PDF editing website with OpenPDF**, but it requires:

1. **Java backend** (Spring Boot recommended)
2. **OpenPDF library** for PDF operations
3. **Web frontend** (React/Vue/Angular)
4. **PDF.js** for browser PDF viewing
5. **Storage solution** (S3, Azure, local filesystem)
6. **Database** for metadata and user management

The architecture works best for:
- Form filling and generation
- PDF manipulation (merge, split, rotate)
- HTML to PDF conversion
- Watermarking and stamping
- Encryption and signatures

It's **not ideal** for:
- Adobe Acrobat-style visual editing
- Real-time collaborative editing
- Complex graphic design within PDFs

**Next Steps**: Choose your features and start with a Spring Boot backend + React frontend prototype!
