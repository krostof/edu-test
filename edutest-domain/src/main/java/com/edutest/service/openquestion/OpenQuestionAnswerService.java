package com.edutest.service.openquestion;

import com.edutest.domain.assignment.openquestion.CompletionInfo;
import com.edutest.domain.assignment.openquestion.OpenQuestionAnswer;

public class OpenQuestionAnswerService {

    public boolean isCorrect(OpenQuestionAnswer answer) {
        return answer.getScore() != null && answer.getScore() > 0;
    }

    public boolean meetsMinimumLength(OpenQuestionAnswer answer, Integer minLength) {
        if (minLength == null) return true;

        Integer characterCount = answer.getCharacterCount();
        return characterCount != null && characterCount >= minLength;
    }

    public boolean exceedsMaximumLength(OpenQuestionAnswer answer, Integer maxLength) {
        if (maxLength == null) return false;

        Integer characterCount = answer.getCharacterCount();
        return characterCount != null && characterCount > maxLength;
    }

    public boolean isWithinLengthLimits(OpenQuestionAnswer answer, Integer minLength, Integer maxLength) {
        return meetsMinimumLength(answer, minLength) &&
                !exceedsMaximumLength(answer, maxLength);
    }

    public int getRemainingCharacters(OpenQuestionAnswer answer, Integer maxLength) {
        if (maxLength == null) return -1;

        int currentLength = answer.getCharacterCount() != null ? answer.getCharacterCount() : 0;
        return Math.max(0, maxLength - currentLength);
    }

    public int getRequiredCharacters(OpenQuestionAnswer answer, Integer minLength) {
        if (minLength == null) return 0;

        int currentLength = answer.getCharacterCount() != null ? answer.getCharacterCount() : 0;
        return Math.max(0, minLength - currentLength);
    }

    public CompletionInfo calculateCompletion(OpenQuestionAnswer answer, Integer minLength) {
        int currentLength = answer.getCharacterCount() != null ? answer.getCharacterCount() : 0;
        return CompletionInfo.calculate(currentLength, minLength);
    }
}