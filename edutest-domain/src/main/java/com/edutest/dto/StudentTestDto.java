package com.edutest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentTestDto {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer timeLimit;
    private Boolean allowNavigation;
    private Integer assignmentCount;

    // Status czasowy: PAST, ACTIVE, UPCOMING
    private String timeStatus;

    // Status podejścia: NOT_STARTED, IN_PROGRESS, COMPLETED
    private String attemptStatus;

    // Dane podejścia (jeśli istnieje)
    private Long attemptId;
    private LocalDateTime attemptStartedAt;
    private LocalDateTime attemptFinishedAt;
    private Float score;
    private Float maxScore;

    // Informacje o twórcy testu
    private String createdByName;

    public enum TimeStatus {
        PAST,      // Test już się zakończył (endDate < now)
        ACTIVE,    // Test jest aktywny (startDate <= now <= endDate)
        UPCOMING   // Test jeszcze się nie rozpoczął (startDate > now)
    }

    public enum AttemptStatus {
        NOT_STARTED,  // Student nie rozpoczął testu
        IN_PROGRESS,  // Student jest w trakcie rozwiązywania
        COMPLETED     // Student ukończył test
    }
}
