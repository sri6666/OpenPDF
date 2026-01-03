package com.esign.pdfservice.extractor;

import com.esign.pdfservice.model.PdfMetadata;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.AcroFields;
import org.openpdf.text.pdf.PdfReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Extracts metadata from PDF documents using OpenPDF
 */
@Component
public class PdfMetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdfMetadataExtractor.class);

    /**
     * Extract metadata from PDF
     * @param pdfData PDF file as byte array
     * @return PdfMetadata object containing extracted information
     * @throws IllegalArgumentException if PDF is invalid or encrypted
     */
    public PdfMetadata extract(byte[] pdfData) {
        if (pdfData == null || pdfData.length == 0) {
            throw new IllegalArgumentException("PDF data cannot be null or empty");
        }

        try (PdfReader reader = new PdfReader(pdfData)) {
            // Check if encrypted
            if (reader.isEncrypted()) {
                throw new IllegalArgumentException("Cannot extract metadata from encrypted PDF");
            }

            PdfMetadata metadata = new PdfMetadata();

            // Extract page count
            metadata.setPageCount(reader.getNumberOfPages());

            // Extract page dimensions (from first page)
            if (reader.getNumberOfPages() > 0) {
                Rectangle pageSize = reader.getPageSize(1);
                metadata.setPageWidth(pageSize.getWidth());
                metadata.setPageHeight(pageSize.getHeight());
            }

            // Extract form fields
            AcroFields fields = reader.getAcroFields();
            if (fields != null && !fields.getFields().isEmpty()) {
                metadata.setFormFields(new ArrayList<>(fields.getFields().keySet()));
            }

            // Extract document metadata
            Map<String, String> info = reader.getInfo();
            if (info != null) {
                metadata.setTitle(info.get("Title"));
                metadata.setAuthor(info.get("Author"));
                metadata.setSubject(info.get("Subject"));
                metadata.setCreator(info.get("Creator"));
            }

            metadata.setEncrypted(reader.isEncrypted());

            logger.debug("Extracted metadata: {} pages, {}x{} dimensions",
                    metadata.getPageCount(), metadata.getPageWidth(), metadata.getPageHeight());

            return metadata;

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to extract PDF metadata: " + e.getMessage(), e);
        }
    }
}
