package org.example.documentmanagementservice.Execeptions;

public class DocumentGenerationException extends RuntimeException {
    public DocumentGenerationException(String message) {
        super(message);
    }
}