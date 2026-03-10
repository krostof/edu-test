package com.edutest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class CreateAssignmentRequest {

    @NotNull
    private String type;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Positive
    private Float points;

    // Single Choice / Multiple Choice
    private List<ChoiceOptionDto> options;
    private Boolean randomizeOptions;

    // Multiple Choice only
    private Boolean partialScoring;
    private Boolean penaltyForWrong;

    // Open Question
    private Integer minLength;
    private Integer maxLength;
    private String sampleAnswer;
    private String gradingRubric;
    private Boolean allowHtml;

    // Coding
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private String allowedLanguages;
    private String starterCode;
    private String solutionTemplate;
    private List<TestCaseDto> testCases;
}
