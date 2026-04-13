package com.edutest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChoiceOptionDto {
    private Long id;
    private String optionText;
    private boolean correct;
    private Integer orderNumber;
    private String explanation;
}
