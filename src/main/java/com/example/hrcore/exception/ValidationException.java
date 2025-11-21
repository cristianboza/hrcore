package com.example.hrcore.exception;

import lombok.Getter;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ValidationException extends RuntimeException {
    
    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = Map.of("error", message);
    }

    public ValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public ValidationException(List<FieldError> fieldErrors) {
        super("Validation failed");
        this.errors = fieldErrors.stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
    }

    public static ValidationException of(String field, String message) {
        return new ValidationException(Map.of(field, message));
    }
}
