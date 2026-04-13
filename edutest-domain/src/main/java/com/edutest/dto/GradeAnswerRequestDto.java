package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeAnswerRequestDto {
    private Float score;
    private String feedback;
}
