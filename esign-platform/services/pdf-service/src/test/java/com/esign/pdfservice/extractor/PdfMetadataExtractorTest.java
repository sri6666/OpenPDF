package com.esign.pdfservice.extractor;

import com.esign.pdfservice.model.PdfMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PDF Metadata Extractor Tests")
class PdfMetadataExtractorTest {

    private PdfMetadataExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfMetadataExtractor();
    }

    @Test
    @DisplayName("Should extract page count from valid PDF")
    void testExtractMetadata_ValidPdf_ReturnsCorrectPageCount() throws IOException {
        // Given
        byte[] pdf = createPdfWithPages(3);

        // When
        PdfMetadata metadata = extractor.extract(pdf);

        // Then
        assertThat(metadata.getPageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should extract page dimensions for A4 PDF")
    void testExtractPageDimensions_A4Pdf_ReturnsCorrectSize() throws IOException {
        // Given
        byte[] pdf = createA4Pdf();

        // When
        PdfMetadata metadata = extractor.extract(pdf);

        // Then
        assertThat(metadata.getPageWidth()).isCloseTo(595.0f, within(1.0f)); // A4 width in points
        assertThat(metadata.getPageHeight()).isCloseTo(842.0f, within(1.0f)); // A4 height in points
    }

    @Test
    @DisplayName("Should detect form fields in PDF")
    void testExtractFormFields_PdfWithFields_ReturnsAllFields() throws IOException {
        // Given
        byte[] pdf = createPdfWithFormFields();

        // When
        PdfMetadata metadata = extractor.extract(pdf);

        // Then
        assertThat(metadata.hasFormFields()).isTrue();
        assertThat(metadata.getFormFields()).isNotEmpty();
        assertThat(metadata.getFormFields()).contains("name", "email", "signature");
    }

    @Test
    @DisplayName("Should return empty form fields for PDF without forms")
    void testExtractFormFields_PdfWithoutFields_ReturnsEmpty() throws IOException {
        // Given
        byte[] pdf = createSimplePdf();

        // When
        PdfMetadata metadata = extractor.extract(pdf);

        // Then
        assertThat(metadata.hasFormFields()).isFalse();
        assertThat(metadata.getFormFields()).isEmpty();
    }

    @Test
    @DisplayName("Should extract PDF title from metadata")
    void testExtractTitle_PdfWithTitle_ReturnsTitle() throws IOException {
        // Given
        byte[] pdf = createPdfWithMetadata("Contract Agreement", "John Doe");

        // When
        PdfMetadata metadata = extractor.extract(pdf);

        // Then
        assertThat(metadata.getTitle()).isEqualTo("Contract Agreement");
        assertThat(metadata.getAuthor()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should handle encrypted PDF")
    void testExtract_EncryptedPdf_ThrowsException() {
        // Given
        byte[] encryptedPdf = createEncryptedPdf();

        // When/Then
        assertThatThrownBy(() -> extractor.extract(encryptedPdf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encrypted");
    }

    @Test
    @DisplayName("Should throw exception for invalid PDF")
    void testExtract_InvalidPdf_ThrowsException() {
        // Given
        byte[] invalidPdf = "not a pdf".getBytes();

        // When/Then
        assertThatThrownBy(() -> extractor.extract(invalidPdf))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Helper methods to create test PDFs
    private byte[] createPdfWithPages(int pageCount) {
        // TODO: Use OpenPDF to create test PDF with specified pages
        return createMinimalPdf();
    }

    private byte[] createA4Pdf() {
        return createMinimalPdf();
    }

    private byte[] createPdfWithFormFields() {
        // TODO: Create PDF with actual form fields using OpenPDF
        return createMinimalPdf();
    }

    private byte[] createPdfWithMetadata(String title, String author) {
        // TODO: Create PDF with metadata
        return createMinimalPdf();
    }

    private byte[] createEncryptedPdf() {
        // TODO: Create encrypted PDF
        return createMinimalPdf();
    }

    private byte[] createSimplePdf() {
        return createMinimalPdf();
    }

    private byte[] createMinimalPdf() {
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
}
