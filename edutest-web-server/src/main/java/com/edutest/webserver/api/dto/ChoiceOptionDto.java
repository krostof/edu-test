package com.edutest.webserver.api.dto;

import lombok.Data;

@Data
public class ChoiceOptionDto {
    private Long id;
    private String optionText;
    private boolean correct;
    private Integer orderNumber;
    private String explanation;
}
