package com.edutest.service.singlechoiceassigment;

import com.edutest.domain.assignment.ValidationResult;
import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SingleChoiceAssignmentService {

    public SingleChoiceAssignment addOption(SingleChoiceAssignment assignment, ChoiceOption option) {
        if (option == null) {
            throw new IllegalArgumentException("Option cannot be null");
        }

        List<ChoiceOption> newOptions = new ArrayList<>(assignment.getOptions());
        newOptions.add(option);

        return assignment.toBuilder()
                .options(newOptions)
                .build();
    }

    public SingleChoiceAssignment removeOption(SingleChoiceAssignment assignment, Long optionId) {
        List<ChoiceOption> newOptions = assignment.getOptions().stream()
                .filter(option -> !option.getId().equals(optionId))
                .collect(Collectors.toList());

        return assignment.toBuilder()
                .options(newOptions)
                .build();
    }

    public SingleChoiceAssignment updateOption(SingleChoiceAssignment assignment,
                                               Long optionId, ChoiceOption newOption) {
        List<ChoiceOption> newOptions = assignment.getOptions().stream()
                .map(option -> option.getId().equals(optionId) ? newOption : option)
                .collect(Collectors.toList());

        return assignment.toBuilder()
                .options(newOptions)
                .build();
    }

    public SingleChoiceAssignment setOptions(SingleChoiceAssignment assignment,
                                             List<ChoiceOption> options) {
        List<ChoiceOption> newOptions = options != null ? List.copyOf(options) : List.of();

        ValidationResult validation = validateSingleChoiceConfiguration(newOptions);
        if (validation.hasError()) {
            throw new IllegalStateException(validation.getErrorMessage());
        }

        return assignment.toBuilder()
                .options(newOptions)
                .build();
    }

    public ValidationResult validateSingleChoiceConfiguration(List<ChoiceOption> options) {
        if (options == null || options.isEmpty()) {
            return ValidationResult.valid();
        }

        long correctCount = options.stream()
                .filter(ChoiceOption::isCorrect)
                .count();

        if (correctCount != 1) {
            return ValidationResult.invalid("Single choice assignment must have exactly one correct answer");
        }

        if (options.size() < 2) {
            return ValidationResult.invalid("Single choice assignment must have at least 2 options");
        }

        return ValidationResult.valid();
    }
}
