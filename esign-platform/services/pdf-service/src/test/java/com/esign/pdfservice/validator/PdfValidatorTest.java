package com.esign.pdfservice.validator;

import com.esign.pdfservice.exception.FileTooLargeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for PdfValidator
 * Following TDD - tests written BEFORE implementation
 */
@DisplayName("PDF Validator Tests")
class PdfValidatorTest {

    private PdfValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PdfValidator();
    }

    @Test
    @DisplayName("Should validate a valid PDF file")
    void testValidatePdf_ValidFile_ReturnsTrue() throws IOException {
        // Given
        byte[] validPdf = loadTestPdf("valid.pdf");

        // When
        boolean result = validator.isValid(validPdf);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid PDF file")
    void testValidatePdf_InvalidFile_ReturnsFalse() {
        // Given
        byte[] invalidPdf = "not a pdf".getBytes();

        // When
        boolean result = validator.isValid(invalidPdf);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject null PDF data")
    void testValidatePdf_NullData_ReturnsFalse() {
        // Given
        byte[] nullPdf = null;

        // When
        boolean result = validator.isValid(nullPdf);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject empty PDF data")
    void testValidatePdf_EmptyData_ReturnsFalse() {
        // Given
        byte[] emptyPdf = new byte[0];

        // When
        boolean result = validator.isValid(emptyPdf);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for file larger than 10MB")
    void testValidatePdf_TooLarge_ThrowsException() {
        // Given
        byte[] largePdf = new byte[11 * 1024 * 1024]; // 11MB

        // When/Then
        assertThatThrownBy(() -> validator.validate(largePdf))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("10MB");
    }

    @Test
    @DisplayName("Should accept file exactly 10MB")
    void testValidatePdf_ExactlyMax_Succeeds() throws IOException {
        // Given
        byte[] exactSizePdf = createValidPdfOfSize(10 * 1024 * 1024);

        // When/Then
        assertThatCode(() -> validator.validate(exactSizePdf))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should get page count from valid PDF")
    void testGetPageCount_ValidPdf_ReturnsCorrectCount() throws IOException {
        // Given
        byte[] pdf = loadTestPdf("3-pages.pdf");

        // When
        int pageCount = validator.getPageCount(pdf);

        // Then
        assertThat(pageCount).isEqualTo(3);
    }

    // Helper methods
    private byte[] loadTestPdf(String filename) throws IOException {
        InputStream is = getClass().getResourceAsStream("/test-pdfs/" + filename);
        if (is == null) {
            // Create a minimal valid PDF for testing
            return createMinimalValidPdf();
        }
        return is.readAllBytes();
    }

    private byte[] createMinimalValidPdf() {
        // Minimal valid PDF structure
        String pdf = "%PDF-1.4\n" +
                "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                "2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n" +
                "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\n" +
                "xref\n0 4\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000056 00000 n\n" +
                "0000000114 00000 n\n" +
                "trailer<</Size 4/Root 1 0 R>>\n" +
                "startxref\n203\n" +
                "%%EOF";
        return pdf.getBytes();
    }

    private byte[] createValidPdfOfSize(int targetSize) {
        // Create a minimal PDF and pad to target size
        byte[] minimal = createMinimalValidPdf();
        if (minimal.length >= targetSize) {
            return minimal;
        }

        byte[] padded = new byte[targetSize];
        System.arraycopy(minimal, 0, padded, 0, minimal.length);
        // Fill the rest with whitespace (valid in PDF)
        for (int i = minimal.length; i < targetSize; i++) {
            padded[i] = ' ';
        }
        return padded;
    }
}
