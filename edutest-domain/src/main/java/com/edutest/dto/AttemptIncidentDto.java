package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttemptIncidentDto {
    private Long id;
    private Long attemptId;
    private String type;
    private LocalDateTime occurredAt;
    private String metadata;
}
