package com.edutest.dto;

import lombok.Data;

@Data
public class UpdateAssignmentRequest {
    private String title;
    private String description;
    private Float points;
}
