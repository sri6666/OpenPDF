package com.esign.pdfservice.validator;

import com.esign.pdfservice.exception.FileTooLargeException;
import org.openpdf.text.pdf.PdfReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PDF file validator using OpenPDF
 * Validates PDF structure, size, and extracts basic metadata
 */
@Component
public class PdfValidator {

    private static final Logger logger = LoggerFactory.getLogger(PdfValidator.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Check if byte array is a valid PDF
     * @param pdfData PDF file as byte array
     * @return true if valid PDF, false otherwise
     */
    public boolean isValid(byte[] pdfData) {
        if (pdfData == null || pdfData.length == 0) {
            logger.debug("PDF data is null or empty");
            return false;
        }

        try (PdfReader reader = new PdfReader(pdfData)) {
            int pages = reader.getNumberOfPages();
            logger.debug("Valid PDF with {} pages", pages);
            return pages > 0;
        } catch (Exception e) {
            logger.debug("Invalid PDF: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate PDF and throw exception if invalid
     * @param pdfData PDF file as byte array
     * @throws FileTooLargeException if file exceeds 10MB
     * @throws IllegalArgumentException if PDF is invalid
     */
    public void validate(byte[] pdfData) {
        if (pdfData == null || pdfData.length == 0) {
            throw new IllegalArgumentException("PDF data cannot be null or empty");
        }

        if (pdfData.length > MAX_FILE_SIZE) {
            throw new FileTooLargeException(
                String.format("PDF file size (%d bytes) exceeds maximum allowed size (10MB)",
                    pdfData.length)
            );
        }

        if (!isValid(pdfData)) {
            throw new IllegalArgumentException("Invalid PDF structure");
        }
    }

    /**
     * Get page count from PDF
     * @param pdfData PDF file as byte array
     * @return number of pages
     * @throws IllegalArgumentException if PDF is invalid
     */
    public int getPageCount(byte[] pdfData) {
        validate(pdfData);

        try (PdfReader reader = new PdfReader(pdfData)) {
            return reader.getNumberOfPages();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read PDF: " + e.getMessage(), e);
        }
    }
}
