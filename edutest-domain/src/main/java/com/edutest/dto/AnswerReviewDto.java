package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AnswerReviewDto {
    private Long answerId;
    private Long attemptId;
    private Long assignmentId;
    private String assignmentTitle;
    private String assignmentType;
    private String assignmentDescription;
    private Float maxPoints;
    private Long studentId;
    private String studentName;
    private LocalDateTime answeredAt;

    // Answer content (depends on type)
    private String answerText;
    private Long selectedOptionId;
    private List<Long> selectedOptionIds;
    private String sourceCode;
    private String programmingLanguage;

    // For choice questions
    private List<Long> correctOptionIds;
    private List<ChoiceOptionDto> options;

    // For open questions
    private String sampleAnswer;
    private String gradingRubric;

    // Grading info
    private Float score;
    private Boolean isGraded;
    private String teacherFeedback;
}
