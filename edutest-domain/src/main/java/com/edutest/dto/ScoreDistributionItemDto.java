package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreDistributionItemDto {
    private String rangeLabel;
    private int count;
    private float percentage;

    public static ScoreDistributionItemDto of(String rangeLabel, int count, int total) {
        float percentage = total > 0 ? (float) count / total * 100f : 0f;
        return ScoreDistributionItemDto.builder()
                .rangeLabel(rangeLabel)
                .count(count)
                .percentage(percentage)
                .build();
    }
}
