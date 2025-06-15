package com.edutest.domain.assignment;

import lombok.*;

/**
 * Prosta klasa do przechowywania wyników walidacji
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;
    private String errorMessage;

    public static ValidationResult valid() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }

    public static ValidationResult invalid(String errorMessage) {
        return ValidationResult.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }

    public boolean isValid() {
        return valid;
    }

    public boolean hasError() {
        return !valid;
    }

    public boolean isInvalid() {
        return !valid;
    }
}
