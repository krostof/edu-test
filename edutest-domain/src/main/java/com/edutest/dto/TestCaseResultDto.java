package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestCaseResultDto {
    private Long testCaseId;
    private Boolean isPublic;
    private String description;
    private String inputData;
    private String expectedOutput;
    private String actualOutput;
    private Boolean passed;
    private Long executionTimeMs;
    private Integer memoryUsedMb;
    private String errorMessage;
}
