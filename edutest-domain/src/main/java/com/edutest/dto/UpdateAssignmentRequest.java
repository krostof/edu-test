package com.edutest.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAssignmentRequest {
    @Size(min = 3, max = 500, message = "Title must be between 3 and 500 characters")
    private String title;

    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    private String description;

    @Positive(message = "Points must be positive")
    private Float points;
}
