package com.example.hrcore.exception;

import lombok.Getter;

@Getter
public class InvalidOperationException extends RuntimeException {
    
    private final String operation;
    private final String reason;

    public InvalidOperationException(String message) {
        super(message);
        this.operation = null;
        this.reason = null;
    }

    public InvalidOperationException(String operation, String reason) {
        super(String.format("Cannot %s: %s", operation, reason));
        this.operation = operation;
        this.reason = reason;
    }
}
