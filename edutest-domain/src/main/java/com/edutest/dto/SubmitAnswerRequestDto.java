package com.edutest.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for submitting answers to assignments.
 * At least one field must be provided based on assignment type:
 * - Single choice: selectedOptionId
 * - Multiple choice: selectedOptionIds
 * - Open question: answerText
 * - Coding: sourceCode and programmingLanguage
 */
@Data
@Builder
public class SubmitAnswerRequestDto {
    // Single choice
    private Long selectedOptionId;

    // Multiple choice
    private List<Long> selectedOptionIds;

    // Open question
    @Size(max = 50000, message = "Answer text must not exceed 50000 characters")
    private String answerText;

    // Coding
    @Size(max = 100000, message = "Source code must not exceed 100000 characters")
    private String sourceCode;

    @Size(max = 50, message = "Programming language must not exceed 50 characters")
    private String programmingLanguage;

    /**
     * Validates that at least one answer field is provided.
     */
    public boolean hasValidAnswer() {
        return selectedOptionId != null
                || (selectedOptionIds != null && !selectedOptionIds.isEmpty())
                || (answerText != null && !answerText.isBlank())
                || (sourceCode != null && !sourceCode.isBlank());
    }
}
