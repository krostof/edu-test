package com.edutest.domain.assignment.singlechoice;

import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.assignment.AssignmentType;
import com.edutest.domain.assignment.ValidationResult;
import com.edutest.domain.assignment.common.ChoiceOption;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SingleChoiceAssignment extends Assignment {

    private List<ChoiceOption> options;
    private boolean randomizeOptions;

    @Builder(builderMethodName = "singleChoiceBuilder")
    public SingleChoiceAssignment(Long id, Long testId, String title, String description,
                                  Integer orderNumber, Integer points, LocalDateTime createdAt,
                                  LocalDateTime updatedAt, List<ChoiceOption> options, boolean randomizeOptions) {
        super(id, testId, title, description, orderNumber, points, createdAt, updatedAt);
        this.options = options != null ? List.copyOf(options) : List.of();
        this.randomizeOptions = randomizeOptions;
    }

    @Override
    public AssignmentType getType() {
        return AssignmentType.SINGLE_CHOICE;
    }

    @Override
    public ValidationResult validateAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return ValidationResult.invalid("Answer cannot be empty");
        }

        try {
            Long optionId = Long.parseLong(answer.trim());
            boolean validOption = options.stream()
                    .anyMatch(option -> option.getId().equals(optionId));

            return validOption ? ValidationResult.valid() :
                    ValidationResult.invalid("Invalid option selected");
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Answer must be a valid option ID");
        }
    }

    @Override
    public Float calculateScore(String answer) {
        ValidationResult validation = validateAnswer(answer);
        if (validation.hasError()) {
            return 0.0f;
        }

        try {
            Long selectedOptionId = Long.parseLong(answer.trim());
            ChoiceOption selectedOption = findOptionById(selectedOptionId);

            if (selectedOption != null && selectedOption.isCorrect()) {
                return getPoints().floatValue();
            }
        } catch (NumberFormatException e) {
            // Invalid format
        }

        return 0.0f;
    }

    @Override
    public boolean supportsAttachments() {
        return true;
    }

    // Business methods
    public void addOption(ChoiceOption option) {
        if (option == null) {
            throw new IllegalArgumentException("Option cannot be null");
        }

        this.options = new java.util.ArrayList<>(options);
        this.options.add(option);
        setUpdatedAt(LocalDateTime.now());
    }

    public void removeOption(Long optionId) {
        this.options = options.stream()
                .filter(option -> !option.getId().equals(optionId))
                .collect(java.util.stream.Collectors.toList());
        setUpdatedAt(LocalDateTime.now());
    }

    public void updateOption(Long optionId, ChoiceOption newOption) {
        this.options = options.stream()
                .map(option -> option.getId().equals(optionId) ? newOption : option)
                .collect(java.util.stream.Collectors.toList());
        setUpdatedAt(LocalDateTime.now());
    }

    public ChoiceOption getCorrectOption() {
        return options.stream()
                .filter(ChoiceOption::isCorrect)
                .findFirst()
                .orElse(null);
    }

    public List<ChoiceOption> getCorrectOptions() {
        return options.stream()
                .filter(ChoiceOption::isCorrect)
                .toList();
    }

    public boolean hasCorrectAnswer() {
        return options.stream().anyMatch(ChoiceOption::isCorrect);
    }

    public int getOptionsCount() {
        return options.size();
    }

    public ChoiceOption findOptionById(Long optionId) {
        return options.stream()
                .filter(option -> option.getId().equals(optionId))
                .findFirst()
                .orElse(null);
    }

    public void setOptions(List<ChoiceOption> newOptions) {
        this.options = newOptions != null ? List.copyOf(newOptions) : List.of();
        setUpdatedAt(LocalDateTime.now());
        validateSingleChoice();
    }

    private void validateSingleChoice() {
        if (options == null || options.isEmpty()) {
            return; // Skip validation during construction
        }

        long correctCount = options.stream()
                .filter(ChoiceOption::isCorrect)
                .count();

        if (correctCount != 1) {
            throw new IllegalStateException("Single choice assignment must have exactly one correct answer");
        }

        if (options.size() < 2) {
            throw new IllegalStateException("Single choice assignment must have at least 2 options");
        }
    }
}