package com.edutest.service.multiplechoiceanswear;

import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.service.SelectionAnalysis;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MultipleChoiceAnswerService {

    public boolean isCorrect(List<ChoiceOption> selectedOptions, List<ChoiceOption> correctOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return false;
        }

        // All selected must be correct AND all correct must be selected
        boolean allSelectedCorrect = selectedOptions.stream().allMatch(ChoiceOption::isCorrect);
        boolean allCorrectSelected = correctOptions.size() == selectedOptions.stream()
                .mapToLong(option -> correctOptions.stream()
                        .filter(correct -> correct.getId().equals(option.getId()))
                        .count())
                .sum();

        return allSelectedCorrect && allCorrectSelected;
    }

    public List<ChoiceOption> getCorrectSelections(List<ChoiceOption> selectedOptions) {
        return selectedOptions.stream()
                .filter(ChoiceOption::isCorrect)
                .collect(Collectors.toList());
    }

    public List<ChoiceOption> getIncorrectSelections(List<ChoiceOption> selectedOptions) {
        return selectedOptions.stream()
                .filter(option -> !option.isCorrect())
                .collect(Collectors.toList());
    }

    public boolean hasAnyCorrectSelections(List<ChoiceOption> selectedOptions) {
        return selectedOptions.stream().anyMatch(ChoiceOption::isCorrect);
    }

    public boolean hasAnyIncorrectSelections(List<ChoiceOption> selectedOptions) {
        return selectedOptions.stream().anyMatch(option -> !option.isCorrect());
    }

    public float calculateAccuracyPercentage(List<ChoiceOption> selectedOptions) {
        if (selectedOptions.isEmpty()) {
            return 0.0f;
        }

        long correctCount = getCorrectSelections(selectedOptions).size();
        return (float) correctCount / selectedOptions.size() * 100.0f;
    }

    public SelectionAnalysis analyzeSelections(List<ChoiceOption> selectedOptions,
                                               List<ChoiceOption> correctOptions) {
        List<ChoiceOption> correctSelections = getCorrectSelections(selectedOptions);
        List<ChoiceOption> incorrectSelections = getIncorrectSelections(selectedOptions);

        List<ChoiceOption> missedCorrect = correctOptions.stream()
                .filter(option -> selectedOptions.stream()
                        .noneMatch(selected -> selected.getId().equals(option.getId())))
                .collect(Collectors.toList());

        return SelectionAnalysis.builder()
                .totalSelected(selectedOptions.size())
                .correctSelected(correctSelections.size())
                .incorrectSelected(incorrectSelections.size())
                .totalCorrectAvailable(correctOptions.size())
                .missedCorrect(missedCorrect.size())
                .isPerfect(correctSelections.size() == correctOptions.size() && incorrectSelections.isEmpty())
                .hasErrors(incorrectSelections.size() > 0 || missedCorrect.size() > 0)
                .accuracyPercentage(calculateAccuracyPercentage(selectedOptions))
                .build();
    }
}
