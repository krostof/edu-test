package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubmitAnswerRequestDto {
    // Single choice
    private Long selectedOptionId;

    // Multiple choice
    private List<Long> selectedOptionIds;

    // Open question
    private String answerText;

    // Coding
    private String sourceCode;
    private String programmingLanguage;
}
