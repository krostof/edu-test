package com.edutest.domain.assignment.multiplechoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ScoringAnalysis {
    private int correctSelections;
    private int wrongSelections;
    private int missedCorrect;
    private int totalCorrect;
    private float score;
    private float maxScore;
    private float percentage;

    public boolean isPerfect() {
        return correctSelections == totalCorrect && wrongSelections == 0;
    }

    public boolean hasErrors() {
        return wrongSelections > 0 || missedCorrect > 0;
    }
}
