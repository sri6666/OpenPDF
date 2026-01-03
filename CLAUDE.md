# CLAUDE.md - OpenPDF Codebase Guide for AI Assistants

**Version:** 3.0.1-SNAPSHOT
**Last Updated:** 2026-01-03
**Purpose:** Comprehensive guide for AI assistants working with the OpenPDF codebase

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Repository Structure](#repository-structure)
3. [Module Architecture](#module-architecture)
4. [Development Workflow](#development-workflow)
5. [Code Style and Conventions](#code-style-and-conventions)
6. [Key Classes and Entry Points](#key-classes-and-entry-points)
7. [Package Organization](#package-organization)
8. [Testing Guidelines](#testing-guidelines)
9. [Common Tasks](#common-tasks)
10. [Important Conventions](#important-conventions)
11. [Things to Watch Out For](#things-to-watch-out-for)

---

## Project Overview

### What is OpenPDF?
OpenPDF is an open-source Java library for creating, editing, rendering, and encrypting PDF documents. It's a fork of iText 4.2.0, licensed under dual LGPL 2.1 and MPL 2.0 licenses.

### Key Capabilities
- **PDF Creation**: Generate PDFs from scratch with text, images, tables, forms
- **PDF Manipulation**: Modify existing PDFs (add/remove pages, modify content)
- **HTML to PDF**: Convert HTML/CSS to PDF using the openpdf-html module (Flying Saucer fork)
- **PDF Rendering**: Render PDF pages as images using openpdf-renderer
- **Encryption & Signing**: Secure PDFs with passwords and digital signatures
- **Kotlin Support**: Kotlin DSL for fluent PDF creation
- **PDF 2.0 Support**: ISO 32000-2 compliance

### Technology Stack
- **Language**: Java 21+ (primary), Kotlin (DSL module)
- **Build System**: Apache Maven (multi-module project)
- **Testing**: JUnit 5, Mockito 5, AssertJ
- **Code Quality**: Checkstyle, JaCoCo, Pitest, CodeQL
- **CI/CD**: GitHub Actions (Maven build on Java 21, 24, 25-ea)

### Statistics
- **Total Files**: 1,333 Java files, 2 Kotlin files
- **Lines of Code**: ~289,340 lines
- **Test Classes**: 119 test classes
- **Modules**: 7 Maven modules
- **Package Namespace**: `org.openpdf` (changed from `com.lowagie` in v3.0)

---

## Repository Structure

```
/home/user/OpenPDF/
├── openpdf-core/              # Core PDF library (main module)
│   ├── src/main/java/org/openpdf/text/
│   │   ├── pdf/               # 190+ PDF implementation classes
│   │   ├── html/              # HTML parsing support
│   │   ├── xml/               # XML/XMP metadata support
│   │   ├── factories/         # Factory patterns (FontFactory, etc.)
│   │   └── [Document, Paragraph, Chunk, Font, Image, Table, etc.]
│   ├── src/main/resources/    # Fonts, CMaps, localized error messages
│   └── src/test/              # 81 test classes
│
├── openpdf-html/              # HTML to PDF rendering (Flying Saucer fork)
│   ├── src/main/java/org/openpdf/
│   │   ├── css/               # CSS 2.1 parsing and styling
│   │   ├── layout/            # Box model and layout engine
│   │   ├── render/            # PDF rendering from HTML
│   │   └── simple/            # XHTMLPanel for Swing integration
│   └── src/main/resources/    # HTML/XHTML/DocBook schemas
│
├── openpdf-renderer/          # PDF rendering to images/display
│   └── src/main/java/org/openpdf/renderer/
│       ├── decrypt/           # PDF decryption
│       ├── colorspace/        # Color space handling
│       ├── font/              # Font rendering
│       └── decode/            # Stream decoding
│
├── openpdf-kotlin/            # Kotlin DSL wrapper
│   └── src/main/kotlin/       # PdfBuilder, HtmlPdfBuilder
│
├── pdf-swing/                 # Swing components for PDF display
│
├── pdf-toolbox/               # Examples and demonstrations
│   └── src/test/java/org/openpdf/examples/
│       ├── objects/           # Text, images, anchors examples
│       ├── tables/            # Table creation examples
│       ├── forms/             # AcroForm examples
│       ├── fonts/             # Font styling examples
│       ├── html/              # HTML to PDF examples
│       └── general/           # General PDF operations
│
├── openpdf-fonts-extra/       # Extra fonts (Liberation TTF)
│
├── config/                    # IDE code style configurations
│   ├── intellij-java-openpdf-style.xml
│   └── eclipse-java-openpdf-style.xml
│
├── .github/workflows/         # CI/CD pipelines
│   ├── maven.yml              # Main build pipeline
│   └── codeql.yml             # Security analysis
│
├── checkstyle.xml             # Code style rules (458 lines)
├── .editorconfig              # Editor configuration (632 lines)
├── pom.xml                    # Parent Maven POM (457 lines)
├── CONTRIBUTING.md            # Contribution guidelines
└── Security.md                # Security policy
```

---

## Module Architecture

### openpdf-core
**Artifact**: `com.github.librepdf:openpdf`
**Purpose**: Core PDF creation and manipulation library
**Key Packages**:
- `org.openpdf.text` - Main document classes (Document, Paragraph, Chunk, etc.)
- `org.openpdf.text.pdf` - PDF-specific implementation (PdfWriter, PdfDocument, PdfReader)
- `org.openpdf.text.pdf.fonts` - Font handling (BaseFont, CJK fonts, OpenType)
- `org.openpdf.text.pdf.crypto` - Encryption and security
- `org.openpdf.text.pdf.parser` - PDF parsing and text extraction
- `org.openpdf.text.pdf.codec` - Image codecs (JPEG, TIFF, WMF)
- `org.openpdf.text.xml` - XML and XMP metadata

**Dependencies**:
- Optional: BouncyCastle (encryption/signing)
- Optional: Apache FOP (advanced typography)

### openpdf-html
**Artifact**: `com.github.librepdf:openpdf-html`
**Purpose**: Convert HTML/CSS to PDF (fork of Flying Saucer)
**Key Packages**:
- `org.openpdf.css` - CSS 2.1 parsing and styling
- `org.openpdf.layout` - Box model and layout engine
- `org.openpdf.render` - Rendering to PDF
- `org.openpdf.simple` - XHTMLPanel for Swing

**Entry Point**: `ITextRenderer` class

### openpdf-renderer
**Artifact**: `com.github.librepdf:openpdf-renderer`
**Purpose**: Render PDF pages to images or Swing/JavaFX components
**Key Packages**:
- `org.openpdf.renderer` - Core rendering engine
- `org.openpdf.renderer.decrypt` - PDF decryption
- `org.openpdf.renderer.font` - Font rendering

### openpdf-kotlin
**Artifact**: `com.github.librepdf:openpdf-kotlin`
**Purpose**: Kotlin DSL for fluent PDF creation
**Key Classes**:
- `PdfBuilder` - DSL for building PDFs
- `HtmlPdfBuilder` - DSL for HTML to PDF

### pdf-toolbox
**Artifact**: `com.github.librepdf:pdf-toolbox`
**Purpose**: Examples and testing utilities (not for production use)
**Contains**: 50+ example classes demonstrating API usage

### openpdf-fonts-extra
**Artifact**: `com.github.librepdf:openpdf-fonts-extra`
**Purpose**: UTF-8 Liberation fonts to reduce core JAR size
**Usage**: Include this dependency to use bundled Liberation fonts

---

## Development Workflow

### Building the Project

```bash
# Clone the repository
git clone https://github.com/LibrePDF/OpenPDF.git
cd OpenPDF

# Build all modules (requires Java 21+)
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build with code coverage
mvn clean install
# Coverage reports in: target/site/jacoco/index.html

# Run checkstyle
mvn checkstyle:check

# Run mutation tests (Pitest)
mvn pitmp:run
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
cd openpdf-core
mvn test

# Run specific test class
mvn test -Dtest=AcroFieldsTest

# Run specific test method
mvn test -Dtest=AcroFieldsTest#testGetField
```

### IDE Setup

**IntelliJ IDEA**:
1. Import as Maven project
2. Import code style: `config/intellij-java-openpdf-style.xml`
3. Install Checkstyle plugin and point to `checkstyle.xml`
4. Install SonarLint plugin

**Eclipse**:
1. Import as Maven project
2. Import code style: `config/eclipse-java-openpdf-style.xml`
3. Install EditorConfig plugin (or use `.editorconfig`)
4. Install Checkstyle plugin and point to `checkstyle.xml`

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "Description of changes"

# Push to fork
git push origin feature/your-feature-name

# Create pull request on GitHub
```

### Release Process

- Managed by maintainers using `createRelease.sh`
- Versioning follows semantic versioning
- Releases published to Maven Central
- GPG signing required for releases (profile: `release`)

---

## Code Style and Conventions

### Style Guide
OpenPDF follows **Google Java Style Guide** with these modifications:
- **Indentation**: 4 spaces (not 2)
- **Continuation Indent**: 8 spaces
- **Max Line Length**: 120 characters
- **No Tabs**: Use spaces only
- **Encoding**: UTF-8

### Checkstyle Rules
Enforced via `checkstyle.xml` (458 lines):
- No unused imports
- No wildcard imports (enforced but commented in legacy code)
- Braces required for all control structures
- Left curly brace on same line
- One top-level class per file
- No line wrapping for package/import statements
- Empty blocks must contain comments or be truly empty

### Naming Conventions

**Classes**: PascalCase
```java
public class PdfWriter { }
public class AcroFields { }
```

**Methods**: camelCase
```java
public void openDocument() { }
public String getFieldName() { }
```

**Constants**: UPPER_SNAKE_CASE
```java
public static final int PAGE_SIZE_A4 = 1;
```

**Packages**: All lowercase, hierarchical
```java
org.openpdf.text.pdf.fonts
```

**Test Classes**: `{ClassName}Test`
```java
AcroFieldsTest.java
PdfWriterTest.java
```

**Example Classes**: `{Feature}Example`
```java
HelloWorldExample.java
TablesExample.java
```

### JavaDoc Requirements
- Public API methods must have JavaDoc
- Include `@param`, `@return`, `@throws` where applicable
- Legacy code may have incomplete JavaDoc (improvements welcome)

### Import Organization
- No wildcard imports (avoid `import java.util.*`)
- Organize imports: java.*, javax.*, org.*, com.*
- Remove unused imports (checkstyle enforces)

---

## Key Classes and Entry Points

### Core PDF Creation Flow

```java
// Basic document creation pattern
Document document = new Document();
PdfWriter.getInstance(document, new FileOutputStream("output.pdf"));
document.open();
document.add(new Paragraph("Hello World"));
document.close();
```

### Essential Classes

#### Document Creation
- **`org.openpdf.text.Document`** - Main document container, manages lifecycle
- **`org.openpdf.text.pdf.PdfWriter`** - Writes Document to PDF format
- **`org.openpdf.text.pdf.PdfDocument`** - Internal PDF document representation

#### Content Elements
- **`org.openpdf.text.Paragraph`** - Block of text with spacing
- **`org.openpdf.text.Chunk`** - Smallest unit of text (atomic)
- **`org.openpdf.text.Phrase`** - Collection of chunks with line spacing
- **`org.openpdf.text.Font`** - Text font and styling
- **`org.openpdf.text.Image`** - Image element (JPEG, PNG, TIFF, etc.)
- **`org.openpdf.text.pdf.PdfPTable`** - Table with cells and rows
- **`org.openpdf.text.pdf.PdfPCell`** - Table cell

#### PDF Manipulation
- **`org.openpdf.text.pdf.PdfReader`** - Read existing PDF files
- **`org.openpdf.text.pdf.PdfStamper`** - Modify existing PDFs
- **`org.openpdf.text.pdf.PdfCopy`** - Merge/split PDFs
- **`org.openpdf.text.pdf.PdfContentByte`** - Low-level drawing API

#### Forms and Fields
- **`org.openpdf.text.pdf.AcroFields`** - Read/write form fields
- **`org.openpdf.text.pdf.PdfFormField`** - Form field creation
- **`org.openpdf.text.pdf.TextField`** - Text input field

#### Fonts
- **`org.openpdf.text.pdf.BaseFont`** - Base font handling
- **`org.openpdf.text.factories.FontFactory`** - Font creation factory
- **`org.librepdf.openpdf.fonts.Liberation`** - Liberation fonts (requires openpdf-fonts-extra)

#### Encryption and Security
- **`org.openpdf.text.pdf.PdfEncryption`** - PDF encryption
- **`org.openpdf.text.pdf.PdfPublicKeySecurityHandler`** - Public key encryption
- **`org.openpdf.text.pdf.PdfSignatureAppearance`** - Digital signatures

#### HTML to PDF (openpdf-html)
- **`org.openpdf.pdf.ITextRenderer`** - Main HTML to PDF entry point

```java
// HTML to PDF example
ITextRenderer renderer = new ITextRenderer();
renderer.setDocumentFromString("<html><body>Hello</body></html>");
renderer.layout();
renderer.createPDF(new FileOutputStream("output.pdf"));
```

#### PDF Rendering (openpdf-renderer)
- **`org.openpdf.renderer.PDFFile`** - Load PDF for rendering
- **`org.openpdf.renderer.PDFPage`** - Render individual pages

---

## Package Organization

### openpdf-core Packages

```
org.openpdf.text/
├── text/                        # Main document classes
│   ├── Document, Paragraph, Chunk, Phrase
│   ├── Font, Image, Table, List, Chapter
│   └── Rectangle, Element, ElementListener
│
├── text.pdf/                    # PDF implementation (259+ classes)
│   ├── PdfWriter, PdfReader, PdfDocument
│   ├── PdfStamper, PdfCopy, PdfContentByte
│   ├── AcroFields, PdfFormField, TextField
│   ├── PdfPTable, PdfPCell, ColumnText
│   └── BaseFont, PdfString, PdfArray, PdfDictionary
│
├── text.pdf.crypto/             # Encryption
│   ├── AESCipher, ARCFOUREncryption
│   └── IVGenerator, DigestAlgorithms
│
├── text.pdf.fonts/              # Font handling
│   ├── BaseFont, CJKFont, Type1Font
│   ├── TrueTypeFont, OpenTypeFont
│   └── FontSelector, GlyphSubstitutionTable
│
├── text.pdf.fonts.cmaps/        # Character mappings
│
├── text.pdf.parser/             # PDF parsing
│   ├── PdfTextExtractor, LocationTextExtractionStrategy
│   └── ImageRenderInfo, TextRenderInfo
│
├── text.pdf.codec/              # Image codecs
│   ├── TiffImage, JpegImage, PngImage
│   └── WmfImage, Ccitt, GifImage
│
├── text.pdf.events/             # Event listeners
│   ├── PdfPageEventHelper
│   └── PageNumberEvents
│
├── text.pdf.draw/               # Graphics
│   ├── DrawInterface, LineSeparator
│   └── VerticalPositionMark
│
├── text.pdf.hyphenation/        # Hyphenation
│
├── text.pdf.collection/         # PDF collections
│
├── text.html/                   # HTML support
│   ├── HtmlParser, HtmlTags
│   └── HtmlEncoder
│
├── text.xml/                    # XML support
│   ├── XMLUtil, XmlParser
│   └── xml.xmp/                 # XMP metadata
│
├── text.factories/              # Factories
│   ├── FontFactory, ElementFactory
│   └── GreekAlphabetFactory
│
├── text.exceptions/             # Custom exceptions
│   ├── InvalidPdfException
│   └── BadPasswordException
│
├── text.error_messages/         # Localized errors
│   └── Messages (en, de, nl, pt)
│
└── text.utils/                  # Utilities
    ├── SystemPropertyUtil
    └── PdfEncryptor
```

### Architectural Patterns

1. **Factory Pattern**
   - `FontFactory` - Font creation
   - `ElementFactory` - Element creation

2. **Listener Pattern**
   - `ElementListener` - Document events
   - `PdfPageEventHelper` - Page events

3. **Strategy Pattern**
   - `SplitCharacter` - Word splitting
   - `HyphenationEvent` - Hyphenation

4. **Decorator Pattern**
   - `Chunk` wrapping text with formatting

5. **Template Method Pattern**
   - `PdfDocument` lifecycle (open/add/close)

---

## Testing Guidelines

### Test Structure

```
src/test/java/org/openpdf/
├── text/pdf/                    # Core PDF tests
│   ├── AcroFieldsTest.java
│   ├── PdfWriterTest.java
│   └── FontSubsetTest.java
├── text/pdf/sign/               # Signature tests
├── text/pdf/encryption/         # Encryption tests
└── text/html/                   # HTML tests
```

### Test Resources

```
src/test/resources/
├── *.pdf                        # Test PDF files
├── fonts/                       # Test fonts
│   ├── liberation/
│   ├── font-awesome/
│   └── NotoSansThai/
├── *.png, *.tiff               # Test images
└── pdf-2-0/                    # PDF 2.0 test files
```

### Testing Approach

**Unit Tests**
- Test individual classes/methods in isolation
- Use Mockito for mocking dependencies
- Focus on edge cases and error conditions

**Integration Tests**
- Test complete workflows (create PDF, read PDF, modify PDF)
- Generate actual PDF files
- Verify PDF structure and content

**Example Tests**
- Located in `pdf-toolbox/src/test/java/org/openpdf/examples/`
- Demonstrate API usage
- Generate sample PDFs

### Test Naming

```java
@Test
void testGetFieldReturnsCorrectValue() { }

@Test
void testEncryptionWithInvalidPasswordThrowsException() { }

@ParameterizedTest
@ValueSource(strings = {"Arial", "Helvetica", "Times-Roman"})
void testFontCreation(String fontName) { }
```

### Assertions

Prefer AssertJ for fluent assertions:

```java
import static org.assertj.core.api.Assertions.*;

assertThat(field.getValue()).isEqualTo("expected");
assertThat(document.getPageNumber()).isGreaterThan(0);
assertThatThrownBy(() -> reader.getPage(999))
    .isInstanceOf(InvalidPdfException.class);
```

### Test Coverage

- JaCoCo generates coverage reports in `target/site/jacoco/`
- Aim for reasonable coverage on new code
- Legacy code may have lower coverage

---

## Common Tasks

### Task 1: Create a Simple PDF

**File**: `openpdf-core/src/main/java/org/openpdf/text/`

```java
Document document = new Document();
PdfWriter.getInstance(document, new FileOutputStream("simple.pdf"));
document.open();
document.add(new Paragraph("Hello, OpenPDF!"));
document.close();
```

### Task 2: Add a Table

**File**: `openpdf-core/src/main/java/org/openpdf/text/pdf/PdfPTable.java`

```java
PdfPTable table = new PdfPTable(3); // 3 columns
table.addCell("Cell 1");
table.addCell("Cell 2");
table.addCell("Cell 3");
document.add(table);
```

### Task 3: Read an Existing PDF

**File**: `openpdf-core/src/main/java/org/openpdf/text/pdf/PdfReader.java`

```java
PdfReader reader = new PdfReader("input.pdf");
int numPages = reader.getNumberOfPages();
reader.close();
```

### Task 4: Fill Form Fields

**File**: `openpdf-core/src/main/java/org/openpdf/text/pdf/AcroFields.java`

```java
PdfReader reader = new PdfReader("form.pdf");
PdfStamper stamper = new PdfStamper(reader, new FileOutputStream("filled.pdf"));
AcroFields form = stamper.getAcroFields();
form.setField("name", "John Doe");
stamper.close();
```

### Task 5: Convert HTML to PDF

**File**: `openpdf-html/src/main/java/org/openpdf/pdf/ITextRenderer.java`

```java
ITextRenderer renderer = new ITextRenderer();
renderer.setDocumentFromString("<html><body><h1>Title</h1></body></html>");
renderer.layout();
renderer.createPDF(new FileOutputStream("html.pdf"));
```

### Task 6: Encrypt a PDF

**File**: `openpdf-core/src/main/java/org/openpdf/text/pdf/PdfWriter.java`

```java
PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("encrypted.pdf"));
writer.setEncryption(
    "user-password".getBytes(),
    "owner-password".getBytes(),
    PdfWriter.ALLOW_PRINTING,
    PdfWriter.ENCRYPTION_AES_128
);
```

### Task 7: Add an Image

**File**: `openpdf-core/src/main/java/org/openpdf/text/Image.java`

```java
Image image = Image.getInstance("photo.jpg");
image.scaleToFit(200, 200);
document.add(image);
```

### Task 8: Digital Signature

**File**: `openpdf-core/src/main/java/org/openpdf/text/pdf/PdfSignatureAppearance.java`

```java
// Requires BouncyCastle dependency
PdfReader reader = new PdfReader("input.pdf");
FileOutputStream os = new FileOutputStream("signed.pdf");
PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');
PdfSignatureAppearance sap = stamper.getSignatureAppearance();
// Configure signature with certificate and private key
```

---

## Important Conventions

### License Compliance

**CRITICAL**: All contributions must be dual-licensed under:
- **LGPL 2.1**: GNU Lesser General Public License
- **MPL 2.0**: Mozilla Public License

**Do NOT**:
- Add GPL or AGPL code
- Add code you didn't write yourself
- Add code from incompatible licenses

### Package Namespace Migration

**OpenPDF 3.0+ uses `org.openpdf` namespace**:
- Old: `com.lowagie.text.Document`
- New: `org.openpdf.text.Document`

When working with code:
- Use `org.openpdf` for all new code
- Legacy code may still reference `com.lowagie` (being migrated)

### Maven Artifact Naming

```xml
<!-- Core library -->
<groupId>com.github.librepdf</groupId>
<artifactId>openpdf</artifactId>

<!-- HTML module -->
<artifactId>openpdf-html</artifactId>

<!-- Renderer module -->
<artifactId>openpdf-renderer</artifactId>

<!-- Kotlin module -->
<artifactId>openpdf-kotlin</artifactId>

<!-- Extra fonts -->
<artifactId>openpdf-fonts-extra</artifactId>
```

### Security Considerations

**From Security.md**:
- OpenPDF does NOT validate or sanitize input
- Application developers must ensure all input is trusted
- No sandboxing or input validation performed
- Risk of code execution, XXE, denial of service if untrusted input is used

When reviewing code:
- Look for input validation gaps
- Flag untrusted file/stream processing
- Check for XXE vulnerabilities in XML parsing
- Verify proper resource cleanup (streams, readers)

### Contribution Requirements

**From CONTRIBUTING.md**:
1. GitHub account must contain real name
2. Fork the project on GitHub
3. Make changes in your fork
4. Run `mvn checkstyle:check` before submitting
5. Add tests for your changes
6. Fix any CI build failures
7. Only contribute code you wrote yourself

---

## Things to Watch Out For

### 1. Deprecated Classes and Methods

Some legacy iText classes may be marked `@Deprecated`:
- Check JavaDoc for replacement classes
- Avoid using deprecated APIs in new code
- Document migration path when deprecating

### 2. Resource Management

PDFs use streams that must be closed:

```java
// BAD: Stream leak
PdfReader reader = new PdfReader("file.pdf");
// ... use reader ...

// GOOD: Try-with-resources
try (PdfReader reader = new PdfReader("file.pdf")) {
    // ... use reader ...
}
```

### 3. Character Encoding

Always specify UTF-8 encoding:

```java
// Font encoding
BaseFont font = BaseFont.createFont(
    BaseFont.HELVETICA,
    BaseFont.CP1252,  // Western encoding
    BaseFont.EMBEDDED
);
```

### 4. Line Length

Checkstyle enforces 120 character line limit:
- Break long lines appropriately
- Use line continuations with 8-space indent

### 5. Test Resource Paths

Test PDFs are excluded from `.gitignore`:

```gitignore
*.pdf
# Allow pdf as source resources for testing
!openpdf/src/test/resources/*.pdf
!pdf-toolbox/src/test/resources/*.pdf
```

Don't commit generated PDFs to `target/` or root directory.

### 6. Multi-Release JARs

OpenPDF uses multi-release JARs for Java 9+ features:

```xml
<manifestEntries>
  <Automatic-Module-Name>${java-module-name}</Automatic-Module-Name>
  <Multi-Release>true</Multi-Release>
</manifestEntries>
```

Be aware of version-specific code in `META-INF/versions/`.

### 7. Optional Dependencies

Some features require optional dependencies:

**BouncyCastle** (encryption/signing):
```xml
<dependency>
  <groupId>org.bouncycastle</groupId>
  <artifactId>bcprov-jdk18on</artifactId>
</dependency>
<dependency>
  <groupId>org.bouncycastle</groupId>
  <artifactId>bcpkix-jdk18on</artifactId>
</dependency>
```

**Apache FOP** (advanced typography):
```xml
<dependency>
  <groupId>org.apache.xmlgraphics</groupId>
  <artifactId>fop</artifactId>
</dependency>
```

Check if optional dependencies are available before using related features.

### 8. PDF 2.0 Support

OpenPDF supports PDF 2.0 (ISO 32000-2):
- Test files in `src/test/resources/pdf-2-0/`
- New features may only work with PDF 2.0
- Check PDF version compatibility

### 9. Legacy Code

OpenPDF inherited code from iText 4.2.0:
- Some code may not follow current best practices
- Gradual improvement is ongoing
- Preserve existing behavior when refactoring
- Add tests before major changes

### 10. Font Licensing

Be aware of font licensing when embedding fonts:
- Liberation fonts (LGPL) - safe to use
- Commercial fonts may have embedding restrictions
- Check font license before embedding

---

## Quick Reference Commands

```bash
# Build everything
mvn clean install

# Run checkstyle
mvn checkstyle:check

# Run tests
mvn test

# Run single test
mvn test -Dtest=AcroFieldsTest

# Generate JavaDoc
mvn javadoc:javadoc

# Code coverage
mvn clean install
open target/site/jacoco/index.html

# Mutation testing
mvn pitmp:run

# Deploy release (maintainers only)
mvn clean deploy -Prelease

# Format POM files
mvn tidy:pom
```

---

## Additional Resources

- **Main README**: `/README.md`
- **Contributing Guide**: `/CONTRIBUTING.md`
- **Security Policy**: `/Security.md`
- **Examples**: `/pdf-toolbox/src/test/java/org/openpdf/examples/`
- **JavaDoc**: https://javadoc.io/doc/com.github.librepdf/openpdf/latest/
- **Wiki**: https://github.com/LibrePDF/OpenPDF/wiki
- **Discussions**: https://github.com/LibrePDF/OpenPDF/discussions
- **Gitter Chat**: https://gitter.im/LibrePDF/OpenPDF

---

## Version History

This codebase is currently on:
- **Version**: 3.0.1-SNAPSHOT
- **Java**: 21+ required
- **Namespace**: `org.openpdf` (changed from `com.lowagie` in v3.0)
- **Build Date**: 2026-01-03

**Previous Versions**:
- 3.0.0 (2025-08-17) - Package namespace change, Java 21 required
- 2.0.x - Java 17+ required
- 1.4.x - Java 11+ required
- 1.3.x - Java 8+ required

---

## For AI Assistants: Key Takeaways

1. **Always read files before modifying** - Don't propose changes to code you haven't seen
2. **Run checkstyle before committing** - `mvn checkstyle:check`
3. **Add tests for new features** - Tests in `src/test/java/`
4. **Follow 120-char line limit** - 4-space indentation, no tabs
5. **Use `org.openpdf` namespace** - Not `com.lowagie`
6. **Close all streams** - Use try-with-resources
7. **Check optional dependencies** - BouncyCastle and FOP are optional
8. **Preserve backward compatibility** - OpenPDF has many users
9. **Security first** - No input validation by default, document risks
10. **Dual licensing** - All code must be LGPL/MPL compatible

---

**Document End**
