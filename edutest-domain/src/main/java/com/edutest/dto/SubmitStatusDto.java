package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmitStatusDto {
    /** PENDING | DONE | FAILED | NONE (no submit job started for this attempt) */
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    /** Populated when status = DONE */
    private TestSubmissionResultDto result;
    /** Populated when status = FAILED */
    private String error;
}
