package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradingResultDto {
    private boolean valid;
    private String errorMessage;
    private Float score;
    private Float maxScore;
    private Float percentage;
    private boolean perfectScore;
    private Object details;

    public static GradingResultDto invalid(String errorMessage) {
        return GradingResultDto.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }
}
