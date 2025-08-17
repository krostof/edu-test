package com.edutest.domain.assignment.openquestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletionInfo {
    private float percentage;
    private boolean isComplete;
    private int currentLength;
    private Integer requiredLength;

    public static CompletionInfo calculate(int currentLength, Integer minLength) {
        if (minLength == null || minLength == 0) {
            boolean complete = currentLength > 0;
            return CompletionInfo.builder()
                    .percentage(complete ? 100.0f : 0.0f)
                    .isComplete(complete)
                    .currentLength(currentLength)
                    .build();
        }

        float percentage = Math.min(100.0f, (float) currentLength / minLength * 100.0f);
        boolean complete = currentLength >= minLength;

        return CompletionInfo.builder()
                .percentage(percentage)
                .isComplete(complete)
                .currentLength(currentLength)
                .requiredLength(minLength)
                .build();
    }
}
