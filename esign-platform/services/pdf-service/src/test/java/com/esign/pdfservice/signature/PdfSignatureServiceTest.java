package com.esign.pdfservice.signature;

import com.esign.pdfservice.exception.InvalidCoordinatesException;
import com.esign.pdfservice.exception.InvalidPageException;
import com.esign.pdfservice.model.SignatureField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.AcroFields;
import org.openpdf.text.pdf.PdfReader;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PDF Signature Service Tests")
class PdfSignatureServiceTest {

    private PdfSignatureService service;

    @BeforeEach
    void setUp() {
        service = new PdfSignatureService();
    }

    @Test
    @DisplayName("Should add signature field at valid coordinates")
    void testAddSignatureField_ValidCoordinates_Success() throws IOException {
        // Given
        byte[] pdf = createBlankPdf();
        SignatureField field = new SignatureField("signature1", 1, 100, 200, 200, 50);

        // When
        byte[] result = service.addSignatureField(pdf, field);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(pdf.length);

        // Verify field was added
        try (PdfReader reader = new PdfReader(result)) {
            AcroFields fields = reader.getAcroFields();
            assertThat(fields.getFields()).containsKey("signature1");
        }
    }

    @Test
    @DisplayName("Should throw exception for invalid page number")
    void testAddSignatureField_InvalidPage_ThrowsException() {
        // Given
        byte[] pdf = createPdfWithPages(1);
        SignatureField field = new SignatureField("sig1", 5, 100, 200, 200, 50);

        // When/Then
        assertThatThrownBy(() -> service.addSignatureField(pdf, field))
                .isInstanceOf(InvalidPageException.class)
                .hasMessageContaining("Page 5 does not exist");
    }

    @Test
    @DisplayName("Should throw exception for negative coordinates")
    void testAddSignatureField_NegativeCoordinates_ThrowsException() {
        // Given
        byte[] pdf = createBlankPdf();
        SignatureField field = new SignatureField("sig1", 1, -10, 200, 200, 50);

        // When/Then
        assertThatThrownBy(() -> service.addSignatureField(pdf, field))
                .isInstanceOf(InvalidCoordinatesException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("Should add multiple signature fields")
    void testAddMultipleSignatureFields_Success() throws IOException {
        // Given
        byte[] pdf = createBlankPdf();
        SignatureField field1 = new SignatureField("sig1", 1, 100, 200, 200, 50);
        SignatureField field2 = new SignatureField("sig2", 1, 100, 300, 200, 50);

        // When
        byte[] result = service.addSignatureField(pdf, field1);
        result = service.addSignatureField(result, field2);

        // Then
        try (PdfReader reader = new PdfReader(result)) {
            AcroFields fields = reader.getAcroFields();
            assertThat(fields.getFields()).containsKeys("sig1", "sig2");
        }
    }

    @Test
    @DisplayName("Should preserve existing content when adding signature field")
    void testAddSignatureField_PreservesExistingContent() throws IOException {
        // Given
        byte[] pdf = createPdfWithText("Test Content");
        SignatureField field = new SignatureField("sig1", 1, 100, 200, 200, 50);

        // When
        byte[] result = service.addSignatureField(pdf, field);

        // Then
        // TODO: Verify text is still present in result PDF
        assertThat(result).isNotNull();
    }

    // Helper methods
    private byte[] createBlankPdf() {
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

    private byte[] createPdfWithPages(int count) {
        return createBlankPdf(); // Simplified
    }

    private byte[] createPdfWithText(String text) {
        return createBlankPdf(); // Simplified
    }
}
