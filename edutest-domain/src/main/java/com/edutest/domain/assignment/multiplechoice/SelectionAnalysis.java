package com.edutest.domain.assignment.multiplechoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SelectionAnalysis {
    private int totalSelected;
    private int correctSelected;
    private int incorrectSelected;
    private int totalCorrectAvailable;
    private int missedCorrect;
    private boolean isPerfect;
    private boolean hasErrors;

    public float getSelectionAccuracy() {
        if (totalSelected == 0) return 0.0f;
        return (float) correctSelected / totalSelected * 100.0f;
    }

    public float getCompleteness() {
        if (totalCorrectAvailable == 0) return 100.0f;
        return (float) correctSelected / totalCorrectAvailable * 100.0f;
    }
}
