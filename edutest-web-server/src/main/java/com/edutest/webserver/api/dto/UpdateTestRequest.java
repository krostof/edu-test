package com.edutest.webserver.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UpdateTestRequest {
    @Size(min = 3, max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    @Min(1) @Max(480)
    private Integer timeLimit;

    private Boolean allowNavigation;

    private Boolean randomizeOrder;
}
