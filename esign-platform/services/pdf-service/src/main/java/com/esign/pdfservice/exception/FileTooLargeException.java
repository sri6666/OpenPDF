package com.esign.pdfservice.exception;

/**
 * Exception thrown when uploaded file exceeds maximum allowed size
 */
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(String message) {
        super(message);
    }

    public FileTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }
}
