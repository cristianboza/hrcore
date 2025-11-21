package com.example.hrcore.exception;

import lombok.Getter;
import java.util.UUID;

@Getter
public class UserNotFoundException extends ResourceNotFoundException {
    
    private final UUID userId;
    private final String email;

    public UserNotFoundException(UUID userId) {
        super("User", "id", userId);
        this.userId = userId;
        this.email = null;
    }

    public UserNotFoundException(String email) {
        super("User", "email", email);
        this.userId = null;
        this.email = email;
    }

    public UserNotFoundException(String message, UUID userId) {
        super(message);
        this.userId = userId;
        this.email = null;
    }
}
