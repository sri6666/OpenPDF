package com.esign.pdfservice.model;

import java.util.ArrayList;
import java.util.List;

/**
 * PDF metadata extracted from a document
 */
public class PdfMetadata {
    private int pageCount;
    private float pageWidth;
    private float pageHeight;
    private List<String> formFields;
    private String title;
    private String author;
    private String subject;
    private String creator;
    private boolean encrypted;

    public PdfMetadata() {
        this.formFields = new ArrayList<>();
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public float getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(float pageWidth) {
        this.pageWidth = pageWidth;
    }

    public float getPageHeight() {
        return pageHeight;
    }

    public void setPageHeight(float pageHeight) {
        this.pageHeight = pageHeight;
    }

    public List<String> getFormFields() {
        return formFields;
    }

    public void setFormFields(List<String> formFields) {
        this.formFields = formFields != null ? formFields : new ArrayList<>();
    }

    public boolean hasFormFields() {
        return formFields != null && !formFields.isEmpty();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }
}
