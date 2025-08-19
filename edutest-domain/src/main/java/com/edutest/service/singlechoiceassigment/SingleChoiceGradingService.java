package com.edutest.service.singlechoiceassigment;

import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAnswer;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAssignment;

import java.util.List;

public class SingleChoiceGradingService {

    public SingleChoiceAnswer autoGrade(SingleChoiceAnswer answer,
                                        SingleChoiceAssignment assignment,
                                        List<ChoiceOption> allOptions) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }

        if (!answer.hasAnswer()) {
            return answer.toBuilder()
                    .score(0.0f)
                    .graded(true)
                    .build();
        }

        ChoiceOption selectedOption = findOptionById(allOptions, answer.getSelectedOptionId());

        Float score = (selectedOption != null && selectedOption.isCorrect()) ?
                assignment.getPoints().floatValue() : 0.0f;

        return answer.toBuilder()
                .score(score)
                .graded(true)
                .build();
    }

    public boolean isCorrect(SingleChoiceAnswer answer, List<ChoiceOption> allOptions) {
        if (!answer.hasAnswer()) {
            return false;
        }

        ChoiceOption selectedOption = findOptionById(allOptions, answer.getSelectedOptionId());
        return selectedOption != null && selectedOption.isCorrect();
    }

    private ChoiceOption findOptionById(List<ChoiceOption> options, Long optionId) {
        return options.stream()
                .filter(option -> option.getId().equals(optionId))
                .findFirst()
                .orElse(null);
    }
}
