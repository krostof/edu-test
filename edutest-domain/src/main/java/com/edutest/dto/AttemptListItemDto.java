package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttemptListItemDto {
    private Long attemptId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long groupId;
    private String groupName;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Float score;
    private Float maxPossibleScore;
    private Float scorePercentage;
    private String status;
    private int pendingGradingCount;

    public static String calculateStatus(boolean isCompleted, int pendingGradingCount) {
        if (!isCompleted) {
            return "IN_PROGRESS";
        } else if (pendingGradingCount > 0) {
            return "SUBMITTED";
        } else {
            return "GRADED";
        }
    }
}
