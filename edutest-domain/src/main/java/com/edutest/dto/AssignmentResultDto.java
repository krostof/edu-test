package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentResultDto {
    private Long assignmentId;
    private String assignmentTitle;
    private String assignmentType;
    private Integer orderNumber;
    private Float maxPoints;
    private Float earnedScore;
    private Boolean isCorrect;
    private Boolean isGraded;
    private String teacherFeedback;
    private AnswerDto answer;

    public static AssignmentResultDto notAnswered(Long assignmentId, String title, String type, Integer orderNumber, Float maxPoints) {
        return AssignmentResultDto.builder()
                .assignmentId(assignmentId)
                .assignmentTitle(title)
                .assignmentType(type)
                .orderNumber(orderNumber)
                .maxPoints(maxPoints)
                .earnedScore(0f)
                .isCorrect(false)
                .isGraded(true)
                .answer(null)
                .build();
    }
}
