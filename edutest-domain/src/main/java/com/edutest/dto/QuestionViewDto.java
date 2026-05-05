package com.edutest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionViewDto {
    private Long assignmentId;
    private Integer questionIndex;
    private Integer totalQuestions;
    private String title;
    private String description;
    private String assignmentType;
    private Integer points;
    private List<QuestionOptionDto> options;
    private PreviousAnswerDto previousAnswer;

    // Coding assignment specific fields
    private String programmingLanguage;
    private String starterCode;
}
