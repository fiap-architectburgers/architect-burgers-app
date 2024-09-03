package com.example.gomesrodris.archburgers.domain.exception;

public class DomainPermissionException extends RuntimeException {
    public DomainPermissionException(String message) {
        super(message);
    }
}
