package com.edutest.service.multiplechoiceanswear;

import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAnswer;
import com.edutest.service.SelectionAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MultipleChoiceAnswerFacade {

    private final MultipleChoiceAnswerService answerService;
    private final MultipleChoiceSelectionService selectionService;
    private final MultipleChoiceGradingService gradingService;

    @Autowired
    public MultipleChoiceAnswerFacade(MultipleChoiceAnswerService answerService,
                                      MultipleChoiceSelectionService selectionService,
                                      MultipleChoiceGradingService gradingService) {
        this.answerService = answerService;
        this.selectionService = selectionService;
        this.gradingService = gradingService;
    }

    public String getAnswerText(MultipleChoiceAnswer answer, List<ChoiceOption> allOptions) {
        if (!answer.hasAnswer()) {
            return "No options selected";
        }

        List<ChoiceOption> selectedOptions = getSelectedOptions(answer, allOptions);

        return selectedOptions.stream()
                .map(ChoiceOption::getOptionText)
                .collect(Collectors.joining("; "));
    }

    public boolean isValidAnswer(MultipleChoiceAnswer answer) {
        return answer.hasAnswer();
    }

    public String getValidationError(MultipleChoiceAnswer answer) {
        return answer.hasAnswer() ? null : "No options selected";
    }

    public SelectionAnalysis analyzeSelections(MultipleChoiceAnswer answer,
                                               List<ChoiceOption> allOptions,
                                               List<ChoiceOption> correctOptions) {
        List<ChoiceOption> selectedOptions = getSelectedOptions(answer, allOptions);
        return answerService.analyzeSelections(selectedOptions, correctOptions);
    }

    public List<String> getSelectedOptionsExplanations(MultipleChoiceAnswer answer,
                                                       List<ChoiceOption> allOptions) {
        return getSelectedOptions(answer, allOptions).stream()
                .filter(ChoiceOption::hasExplanation)
                .map(ChoiceOption::getExplanation)
                .collect(Collectors.toList());
    }

    private List<ChoiceOption> getSelectedOptions(MultipleChoiceAnswer answer,
                                                  List<ChoiceOption> allOptions) {
        return answer.getSelectedOptionIds().stream()
                .map(id -> allOptions.stream()
                        .filter(option -> option.getId().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(option -> option != null)
                .collect(Collectors.toList());
    }
}
