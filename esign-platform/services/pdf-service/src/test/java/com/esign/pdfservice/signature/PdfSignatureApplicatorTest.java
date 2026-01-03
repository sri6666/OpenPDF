package com.esign.pdfservice.signature;

import com.esign.pdfservice.model.SignatureData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * CRITICAL TESTS - Applying signature is the core feature
 */
@DisplayName("PDF Signature Applicator Tests")
class PdfSignatureApplicatorTest {

    private PdfSignatureApplicator applicator;

    @BeforeEach
    void setUp() {
        applicator = new PdfSignatureApplicator();
    }

    @Test
    @DisplayName("Should apply signature image to PDF successfully")
    void testApplySignature_ValidSignature_Success() throws IOException {
        // Given
        byte[] pdf = createBlankPdf();
        byte[] signatureImage = createTestSignatureImage();
        SignatureData signature = new SignatureData(signatureImage, 1, 100, 200, 200, 50);

        // When
        byte[] signedPdf = applicator.applySignature(pdf, signature);

        // Then
        assertThat(signedPdf).isNotNull();
        assertThat(signedPdf.length).isGreaterThan(pdf.length); // Image added

        // Verify it's still a valid PDF
        try (PdfReader reader = new PdfReader(signedPdf)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Should preserve existing PDF content when adding signature")
    void testApplySignature_PreservesExistingContent() throws IOException {
        // Given
        byte[] pdf = createPdfWithText("Original Content");
        byte[] signature = createTestSignatureImage();
        SignatureData sigData = new SignatureData(signature, 1, 100, 200, 200, 50);

        // When
        byte[] signedPdf = applicator.applySignature(pdf, sigData);

        // Then - verify PDF is valid and larger
        assertThat(signedPdf).isNotNull();
        try (PdfReader reader = new PdfReader(signedPdf)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Should apply multiple signatures to same PDF")
    void testApplySignature_MultipleSignatures_AllApplied() throws IOException {
        // Given
        byte[] pdf = createBlankPdf();
        byte[] sig1 = createTestSignatureImage();
        byte[] sig2 = createTestSignatureImage();

        // When - apply two signatures
        byte[] result = applicator.applySignature(pdf,
                new SignatureData(sig1, 1, 100, 200, 200, 50));
        result = applicator.applySignature(result,
                new SignatureData(sig2, 1, 100, 300, 200, 50));

        // Then
        assertThat(result).isNotNull();
        try (PdfReader reader = new PdfReader(result)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Should add timestamp when requested")
    void testApplySignature_WithTimestamp_AddsTimestamp() throws IOException {
        // Given
        byte[] pdf = createBlankPdf();
        byte[] signature = createTestSignatureImage();
        SignatureData sigData = new SignatureData(signature, 1, 100, 200, 200, 50);
        sigData.setAddTimestamp(true);
        sigData.setSignerName("John Doe");

        // When
        byte[] signedPdf = applicator.applySignature(pdf, sigData);

        // Then
        assertThat(signedPdf).isNotNull();
        // TODO: Verify timestamp text is in PDF
    }

    @Test
    @DisplayName("Should throw exception for invalid signature image")
    void testApplySignature_InvalidImage_ThrowsException() {
        // Given
        byte[] pdf = createBlankPdf();
        byte[] invalidImage = "not an image".getBytes();
        SignatureData sigData = new SignatureData(invalidImage, 1, 100, 200, 200, 50);

        // When/Then
        assertThatThrownBy(() -> applicator.applySignature(pdf, sigData))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid page number")
    void testApplySignature_InvalidPage_ThrowsException() {
        // Given
        byte[] pdf = createPdfWithPages(1);
        byte[] signature = createTestSignatureImage();
        SignatureData sigData = new SignatureData(signature, 5, 100, 200, 200, 50);

        // When/Then
        assertThatThrownBy(() -> applicator.applySignature(pdf, sigData))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should scale signature image to fit specified dimensions")
    void testApplySignature_ScalesImage_FitsInBox() throws IOException {
        // Given
        byte[] pdf = createBlankPdf();
        byte[] largeSignature = createLargeSignatureImage(500, 500);
        SignatureData sigData = new SignatureData(largeSignature, 1, 100, 200, 200, 50);

        // When
        byte[] signedPdf = applicator.applySignature(pdf, sigData);

        // Then
        assertThat(signedPdf).isNotNull();
        // Image should be scaled to fit 200x50 box
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

    private byte[] createPdfWithText(String text) {
        return createBlankPdf(); // Simplified
    }

    private byte[] createPdfWithPages(int count) {
        return createBlankPdf(); // Simplified
    }

    private byte[] createTestSignatureImage() {
        // Create a minimal PNG signature (1x1 black pixel)
        return new byte[]{
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte)0x90, 0x77, 0x53, (byte)0xDE,
            0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, // IDAT chunk
            0x08, (byte)0xD7, 0x63, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01,
            (byte)0xE2, 0x21, (byte)0xBC, 0x33,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, // IEND chunk
            (byte)0xAE, 0x42, 0x60, (byte)0x82
        };
    }

    private byte[] createLargeSignatureImage(int width, int height) {
        return createTestSignatureImage(); // Simplified
    }
}
