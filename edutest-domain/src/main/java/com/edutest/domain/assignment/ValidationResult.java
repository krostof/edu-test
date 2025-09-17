package com.edutest.domain.assignment;

import lombok.*;

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

    public boolean hasError() {
        return !valid;
    }

    public boolean isInvalid() {
        return !valid;
    }
}
