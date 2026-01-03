package com.esign.pdfservice.controller;

import com.esign.pdfservice.extractor.PdfMetadataExtractor;
import com.esign.pdfservice.model.PdfMetadata;
import com.esign.pdfservice.model.SignatureData;
import com.esign.pdfservice.signature.PdfSignatureApplicator;
import com.esign.pdfservice.validator.PdfValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private static final Logger logger = LoggerFactory.getLogger(PdfController.class);

    @Autowired
    private PdfValidator pdfValidator;

    @Autowired
    private PdfMetadataExtractor metadataExtractor;

    @Autowired
    private PdfSignatureApplicator signatureApplicator;

    /**
     * Validate a PDF file
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validatePdf(@RequestParam("file") MultipartFile file) {
        try {
            byte[] pdfData = file.getBytes();
            boolean isValid = pdfValidator.isValid(pdfData);

            return ResponseEntity.ok(new ValidationResponse(isValid, isValid ? "Valid PDF" : "Invalid PDF"));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ValidationResponse(false, "Failed to read file: " + e.getMessage()));
        }
    }

    /**
     * Extract metadata from PDF
     */
    @PostMapping("/metadata")
    public ResponseEntity<PdfMetadata> extractMetadata(@RequestParam("file") MultipartFile file) {
        try {
            byte[] pdfData = file.getBytes();
            PdfMetadata metadata = metadataExtractor.extract(pdfData);

            return ResponseEntity.ok(metadata);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Apply a signature image to a PDF document
     *
     * @param pdf PDF file to sign
     * @param signatureImage Signature image (PNG, JPG, etc.)
     * @param page Page number (1-indexed)
     * @param x X coordinate on page
     * @param y Y coordinate on page
     * @param width Signature width
     * @param height Signature height
     * @param addTimestamp Whether to add timestamp below signature
     * @param signerName Name of person signing (optional)
     * @param signerEmail Email of person signing (optional)
     * @return Signed PDF file
     */
    @PostMapping(value = "/apply-signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applySignature(
            @RequestParam("pdf") MultipartFile pdf,
            @RequestParam("signatureImage") MultipartFile signatureImage,
            @RequestParam("page") Integer page,
            @RequestParam("x") Float x,
            @RequestParam("y") Float y,
            @RequestParam("width") Float width,
            @RequestParam("height") Float height,
            @RequestParam(value = "addTimestamp", defaultValue = "false") Boolean addTimestamp,
            @RequestParam(value = "signerName", required = false) String signerName,
            @RequestParam(value = "signerEmail", required = false) String signerEmail
    ) {
        try {
            // Validate inputs
            if (pdf == null || pdf.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "PDF file is required"));
            }

            if (signatureImage == null || signatureImage.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Signature image is required"));
            }

            if (page == null || x == null || y == null || width == null || height == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required parameters: page, x, y, width, height"));
            }

            // Build signature data
            SignatureData signatureData = new SignatureData();
            signatureData.setImageData(signatureImage.getBytes());
            signatureData.setPage(page);
            signatureData.setX(x);
            signatureData.setY(y);
            signatureData.setWidth(width);
            signatureData.setHeight(height);
            signatureData.setAddTimestamp(addTimestamp);
            signatureData.setSignerName(signerName);
            signatureData.setSignerEmail(signerEmail);

            // Apply signature
            byte[] signedPdf = signatureApplicator.applySignature(pdf.getBytes(), signatureData);

            logger.info("Applied signature to PDF: {} (page {}, position: {}, {})",
                pdf.getOriginalFilename(), page, x, y);

            // Return signed PDF with download headers
            String filename = pdf.getOriginalFilename();
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                filename = filename.substring(0, filename.length() - 4) + "-signed.pdf";
            } else {
                filename = "signed-document.pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(signedPdf.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(signedPdf);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid signature parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));

        } catch (IOException e) {
            logger.error("Failed to apply signature to PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process PDF: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Unexpected error applying signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    // Response DTOs
    public static class ValidationResponse {
        private boolean valid;
        private String message;

        public ValidationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
