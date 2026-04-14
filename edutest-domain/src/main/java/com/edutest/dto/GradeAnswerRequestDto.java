package com.edutest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeAnswerRequestDto {
    @NotNull(message = "Score is required")
    @PositiveOrZero(message = "Score must be zero or positive")
    private Float score;

    @Size(max = 5000, message = "Feedback must not exceed 5000 characters")
    private String feedback;
}
