package com.edutest.domain.assignment.openquestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LengthStatus {
    private boolean valid;
    private String message;
    private int currentLength;
    private Integer minLength;
    private Integer maxLength;
    private int remainingCharacters;
    private int requiredCharacters;

    public static LengthStatus valid(int currentLength) {
        return LengthStatus.builder()
                .valid(true)
                .message("Within length limits")
                .currentLength(currentLength)
                .build();
    }

    public static LengthStatus tooShort(int currentLength, Integer minLength) {
        int required = Math.max(0, minLength - currentLength);
        String message = String.format("Too short (minimum: %d characters)", minLength);

        return LengthStatus.builder()
                .valid(false)
                .message(message)
                .currentLength(currentLength)
                .minLength(minLength)
                .requiredCharacters(required)
                .build();
    }

    public static LengthStatus tooLong(int currentLength, Integer maxLength) {
        String message = String.format("Too long (maximum: %d characters)", maxLength);

        return LengthStatus.builder()
                .valid(false)
                .message(message)
                .currentLength(currentLength)
                .maxLength(maxLength)
                .remainingCharacters(Math.max(0, maxLength - currentLength))
                .build();
    }

    public static LengthStatus noAnswer() {
        return LengthStatus.builder()
                .valid(false)
                .message("No answer provided")
                .currentLength(0)
                .build();
    }
}
