package com.esign.pdfservice.signature;

import com.esign.pdfservice.model.SignatureData;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CORE SERVICE - Applies signature images to PDF documents
 * This is the heart of the e-signature platform!
 */
@Service
public class PdfSignatureApplicator {

    private static final Logger logger = LoggerFactory.getLogger(PdfSignatureApplicator.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Apply a signature image to a PDF document
     * @param pdfData Original PDF
     * @param signature Signature data (image, position, etc.)
     * @return PDF with signature applied
     */
    public byte[] applySignature(byte[] pdfData, SignatureData signature) throws IOException {
        if (pdfData == null || pdfData.length == 0) {
            throw new IllegalArgumentException("PDF data cannot be null or empty");
        }

        if (signature.getImageData() == null || signature.getImageData().length == 0) {
            throw new IllegalArgumentException("Signature image data cannot be null or empty");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PdfReader reader = new PdfReader(pdfData)) {
            // Validate page number
            if (signature.getPage() < 1 || signature.getPage() > reader.getNumberOfPages()) {
                throw new IllegalArgumentException(
                    String.format("Invalid page number: %d (PDF has %d pages)",
                        signature.getPage(), reader.getNumberOfPages())
                );
            }

            PdfStamper stamper = new PdfStamper(reader, output);

            // Get the page content layer (over existing content)
            PdfContentByte canvas = stamper.getOverContent(signature.getPage());

            // Load and add signature image
            Image signatureImage;
            try {
                signatureImage = Image.getInstance(signature.getImageData());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid image format: " + e.getMessage(), e);
            }

            // Set position and scale to fit the specified box
            signatureImage.setAbsolutePosition(signature.getX(), signature.getY());
            signatureImage.scaleToFit(signature.getWidth(), signature.getHeight());

            // Add image to PDF
            canvas.addImage(signatureImage);

            logger.info("Applied signature image to page {} at ({}, {})",
                    signature.getPage(), signature.getX(), signature.getY());

            // Add timestamp if requested
            if (signature.isAddTimestamp()) {
                addTimestamp(canvas, signature);
            }

            // Add metadata about the signature
            stamper.setMoreInfo("SignatureApplied", LocalDateTime.now().toString());
            if (signature.getSignerName() != null) {
                stamper.setMoreInfo("SignedBy", signature.getSignerName());
            }
            if (signature.getSignerEmail() != null) {
                stamper.setMoreInfo("SignerEmail", signature.getSignerEmail());
            }

            stamper.close();

        } catch (DocumentException e) {
            throw new IOException("Failed to apply signature: " + e.getMessage(), e);
        }

        return output.toByteArray();
    }

    /**
     * Add timestamp text below the signature
     */
    private void addTimestamp(PdfContentByte canvas, SignatureData signature) {
        String timestamp = "Signed on: " + LocalDateTime.now().format(TIMESTAMP_FORMAT);
        if (signature.getSignerName() != null) {
            timestamp += " by " + signature.getSignerName();
        }

        Font timestampFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Phrase phrase = new Phrase(timestamp, timestampFont);

        // Position timestamp below signature
        float timestampY = signature.getY() - 12;

        ColumnText.showTextAligned(
            canvas,
            Element.ALIGN_LEFT,
            phrase,
            signature.getX(),
            timestampY,
            0
        );

        logger.debug("Added timestamp: {}", timestamp);
    }
}
