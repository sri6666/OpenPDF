package com.esign.pdfservice.signature;

import com.esign.pdfservice.exception.InvalidCoordinatesException;
import com.esign.pdfservice.exception.InvalidPageException;
import com.esign.pdfservice.model.SignatureField;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfFormField;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service for adding signature fields to PDF documents
 */
@Service
public class PdfSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(PdfSignatureService.class);

    /**
     * Add a signature field to a PDF
     * @param pdfData Original PDF as byte array
     * @param field Signature field to add
     * @return Modified PDF with signature field
     */
    public byte[] addSignatureField(byte[] pdfData, SignatureField field) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PdfReader reader = new PdfReader(pdfData)) {
            // Validate page number
            if (field.getPage() < 1 || field.getPage() > reader.getNumberOfPages()) {
                throw new InvalidPageException(
                    String.format("Page %d does not exist (PDF has %d pages)",
                        field.getPage(), reader.getNumberOfPages())
                );
            }

            // Validate coordinates
            if (field.getX() < 0 || field.getY() < 0) {
                throw new InvalidCoordinatesException("Coordinates cannot be negative");
            }

            PdfStamper stamper = new PdfStamper(reader, output);

            // Create text field as signature placeholder
            TextField tf = new TextField(
                stamper.getWriter(),
                new Rectangle(
                    field.getX(),
                    field.getY(),
                    field.getX() + field.getWidth(),
                    field.getY() + field.getHeight()
                ),
                field.getFieldId()
            );

            tf.setOptions(TextField.READ_ONLY);
            tf.setText(""); // Empty initially
            tf.setBorderWidth(1);
            tf.setBorderColor(java.awt.Color.GRAY);

            PdfFormField formField = tf.getTextField();
            stamper.addAnnotation(formField, field.getPage());

            stamper.close();

            logger.debug("Added signature field '{}' to page {} at ({}, {})",
                    field.getFieldId(), field.getPage(), field.getX(), field.getY());

        } catch (DocumentException e) {
            throw new IOException("Failed to add signature field: " + e.getMessage(), e);
        }

        return output.toByteArray();
    }
}
