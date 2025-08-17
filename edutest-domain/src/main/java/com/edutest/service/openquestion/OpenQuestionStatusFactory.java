package com.edutest.service.openquestion;

import com.edutest.domain.assignment.openquestion.LengthStatus;
import com.edutest.domain.assignment.openquestion.OpenQuestionAnswer;

public class OpenQuestionStatusFactory {

    public LengthStatus createLengthStatus(OpenQuestionAnswer answer,
                                           Integer minLength, Integer maxLength) {
        if (answer.isEmpty()) {
            return LengthStatus.noAnswer();
        }

        int currentLength = answer.getCharacterCount() != null ? answer.getCharacterCount() : 0;

        if (minLength != null && currentLength < minLength) {
            return LengthStatus.tooShort(currentLength, minLength);
        }

        if (maxLength != null && currentLength > maxLength) {
            return LengthStatus.tooLong(currentLength, maxLength);
        }

        return LengthStatus.valid(currentLength);
    }
}
