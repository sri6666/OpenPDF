package com.esign.pdfservice.controller;

import com.esign.pdfservice.signature.PdfSignatureApplicator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test for PDF REST controller
 */
@WebMvcTest(PdfController.class)
class PdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PdfSignatureApplicator signatureApplicator;

    @Test
    @DisplayName("Should apply signature to PDF and return signed PDF")
    void testApplySignature_Success() throws Exception {
        // Prepare test data
        byte[] originalPdf = "%PDF-1.4\ntest pdf".getBytes();
        byte[] signedPdf = "%PDF-1.4\nsigned pdf".getBytes();
        byte[] signatureImage = "fake-image-data".getBytes();

        MockMultipartFile pdfFile = new MockMultipartFile(
            "pdf",
            "document.pdf",
            "application/pdf",
            originalPdf
        );

        MockMultipartFile imageFile = new MockMultipartFile(
            "signatureImage",
            "signature.png",
            "image/png",
            signatureImage
        );

        // Mock the service
        when(signatureApplicator.applySignature(any(), any())).thenReturn(signedPdf);

        // Execute request
        mockMvc.perform(multipart("/api/pdf/apply-signature")
                .file(pdfFile)
                .file(imageFile)
                .param("page", "1")
                .param("x", "100")
                .param("y", "200")
                .param("width", "150")
                .param("height", "50")
                .param("addTimestamp", "true")
                .param("signerName", "John Doe")
                .param("signerEmail", "john@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().bytes(signedPdf));
    }

    @Test
    @DisplayName("Should return 400 when PDF file is missing")
    void testApplySignature_MissingPdf() throws Exception {
        byte[] signatureImage = "fake-image-data".getBytes();

        MockMultipartFile imageFile = new MockMultipartFile(
            "signatureImage",
            "signature.png",
            "image/png",
            signatureImage
        );

        mockMvc.perform(multipart("/api/pdf/apply-signature")
                .file(imageFile)
                .param("page", "1")
                .param("x", "100")
                .param("y", "200")
                .param("width", "150")
                .param("height", "50"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("PDF file is required"));
    }

    @Test
    @DisplayName("Should return 400 when signature image is missing")
    void testApplySignature_MissingImage() throws Exception {
        byte[] originalPdf = "%PDF-1.4\ntest pdf".getBytes();

        MockMultipartFile pdfFile = new MockMultipartFile(
            "pdf",
            "document.pdf",
            "application/pdf",
            originalPdf
        );

        mockMvc.perform(multipart("/api/pdf/apply-signature")
                .file(pdfFile)
                .param("page", "1")
                .param("x", "100")
                .param("y", "200")
                .param("width", "150")
                .param("height", "50"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Signature image is required"));
    }

    @Test
    @DisplayName("Should return 400 when required parameters are missing")
    void testApplySignature_MissingParameters() throws Exception {
        byte[] originalPdf = "%PDF-1.4\ntest pdf".getBytes();
        byte[] signatureImage = "fake-image-data".getBytes();

        MockMultipartFile pdfFile = new MockMultipartFile(
            "pdf",
            "document.pdf",
            "application/pdf",
            originalPdf
        );

        MockMultipartFile imageFile = new MockMultipartFile(
            "signatureImage",
            "signature.png",
            "image/png",
            signatureImage
        );

        mockMvc.perform(multipart("/api/pdf/apply-signature")
                .file(pdfFile)
                .file(imageFile))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 500 when signature application fails")
    void testApplySignature_ProcessingError() throws Exception {
        byte[] originalPdf = "%PDF-1.4\ntest pdf".getBytes();
        byte[] signatureImage = "fake-image-data".getBytes();

        MockMultipartFile pdfFile = new MockMultipartFile(
            "pdf",
            "document.pdf",
            "application/pdf",
            originalPdf
        );

        MockMultipartFile imageFile = new MockMultipartFile(
            "signatureImage",
            "signature.png",
            "image/png",
            signatureImage
        );

        // Mock failure
        when(signatureApplicator.applySignature(any(), any()))
            .thenThrow(new RuntimeException("Invalid image format"));

        mockMvc.perform(multipart("/api/pdf/apply-signature")
                .file(pdfFile)
                .file(imageFile)
                .param("page", "1")
                .param("x", "100")
                .param("y", "200")
                .param("width", "150")
                .param("height", "50"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").exists());
    }
}
