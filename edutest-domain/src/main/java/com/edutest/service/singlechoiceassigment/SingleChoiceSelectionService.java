package com.edutest.service.singlechoiceassigment;

import com.edutest.domain.assignment.singlechoice.SingleChoiceAnswer;

import java.time.LocalDateTime;

public class SingleChoiceSelectionService {

    public SingleChoiceAnswer selectOption(SingleChoiceAnswer answer, Long optionId) {
        if (optionId == null) {
            throw new IllegalArgumentException("Option ID cannot be null");
        }

        return answer.toBuilder()
                .selectedOptionId(optionId)
                .answeredAt(LocalDateTime.now())
                .build();
    }

    public SingleChoiceAnswer clearSelection(SingleChoiceAnswer answer) {
        return answer.toBuilder()
                .selectedOptionId(null)
                .answeredAt(null)
                .build();
    }
}
