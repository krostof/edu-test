package com.edutest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviousAnswerDto {
    private Long selectedOptionId;
    private List<Long> selectedOptionIds;
    private String answerText;
    private String sourceCode;
    private String programmingLanguage;
    private LocalDateTime answeredAt;
}
