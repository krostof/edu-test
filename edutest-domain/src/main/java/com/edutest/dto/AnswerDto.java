package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AnswerDto {
    private Long id;
    private Long assignmentId;
    private String assignmentType;
    private LocalDateTime answeredAt;

    // Single choice fields
    private Long selectedOptionId;

    // Multiple choice fields
    private List<Long> selectedOptionIds;

    // Open question fields
    private String answerText;
    private Integer wordCount;
    private Integer characterCount;

    // Coding fields
    private String sourceCode;
    private String programmingLanguage;
    private String compilationStatus;
    private String compilationError;
    private String executionStatus;
    private List<TestCaseResultDto> testCaseResults;
    private Integer testCasesPassed;
    private Integer testCasesTotal;

    // Grading fields
    private Float score;
    private Boolean isGraded;
    private String teacherFeedback;
}
