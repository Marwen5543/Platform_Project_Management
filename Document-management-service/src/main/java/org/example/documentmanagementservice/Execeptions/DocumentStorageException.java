package org.example.documentmanagementservice.Execeptions;

public class DocumentStorageException extends RuntimeException {
    public DocumentStorageException(String message) {
        super(message);
    }
}
