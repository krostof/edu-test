package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RunStatusDto {
    /** PENDING | DONE | FAILED | NONE (no job ever started for this submission) */
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    /** Populated when status = DONE */
    private AnswerDto result;
    /** Populated when status = FAILED */
    private String error;
}
