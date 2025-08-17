package com.edutest.service.singlechoiceassigment;

import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAnswer;

import java.util.List;

public class SingleChoiceAnswerFacade {

    private final SingleChoiceSelectionService selectionService;
    private final SingleChoiceGradingService gradingService;

    public SingleChoiceAnswerFacade(SingleChoiceSelectionService selectionService,
                                    SingleChoiceGradingService gradingService) {
        this.selectionService = selectionService;
        this.gradingService = gradingService;
    }

    public String getAnswerText(SingleChoiceAnswer answer, List<ChoiceOption> allOptions) {
        if (!answer.hasAnswer()) {
            return "No answer";
        }

        ChoiceOption selectedOption = findOptionById(allOptions, answer.getSelectedOptionId());
        return selectedOption != null ? selectedOption.getOptionText() :
                "Option ID: " + answer.getSelectedOptionId();
    }

    public boolean isValidAnswer(SingleChoiceAnswer answer) {
        return answer.hasAnswer();
    }

    public String getValidationError(SingleChoiceAnswer answer) {
        return answer.hasAnswer() ? null : "No option selected";
    }

    public String getSelectedOptionExplanation(SingleChoiceAnswer answer, List<ChoiceOption> allOptions) {
        if (!answer.hasAnswer()) {
            return null;
        }

        ChoiceOption selectedOption = findOptionById(allOptions, answer.getSelectedOptionId());
        return selectedOption != null ? selectedOption.getExplanation() : null;
    }

    private ChoiceOption findOptionById(List<ChoiceOption> options, Long optionId) {
        return options.stream()
                .filter(option -> option.getId().equals(optionId))
                .findFirst()
                .orElse(null);
    }
}
