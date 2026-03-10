package com.edutest.dto;

import lombok.Data;

@Data
public class TestCaseDto {
    private Long id;
    private String inputData;
    private String expectedOutput;
    private Boolean isPublic;
    private String description;
    private Integer weight;
}
