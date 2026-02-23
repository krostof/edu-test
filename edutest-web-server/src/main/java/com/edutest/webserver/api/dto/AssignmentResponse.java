package com.edutest.webserver.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignmentResponse {
    private Long id;
    private String type;
    private String title;
    private String description;
    private Integer orderNumber;
    private Float points;
    private Boolean isAttachmentAllowed;

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
