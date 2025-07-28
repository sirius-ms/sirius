package de.unijena.bioinf.ms.middleware.security.exceptions;

public record FieldValidationError(String field, String message, Object rejectedValue) {
    public static FieldValidationError of(String field, String message, Object rejectedValue) {
        return new FieldValidationError(field, message , rejectedValue);
    }
}