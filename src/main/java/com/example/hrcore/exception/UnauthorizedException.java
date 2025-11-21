package com.example.hrcore.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {
    
    private final String action;
    private final String resource;

    public UnauthorizedException(String message) {
        super(message);
        this.action = null;
        this.resource = null;
    }

    public UnauthorizedException(String action, String resource) {
        super(String.format("You do not have permission to %s %s", action, resource));
        this.action = action;
        this.resource = resource;
    }

    public UnauthorizedException(String action, String resource, String reason) {
        super(String.format("You do not have permission to %s %s: %s", action, resource, reason));
        this.action = action;
        this.resource = resource;
    }
}
