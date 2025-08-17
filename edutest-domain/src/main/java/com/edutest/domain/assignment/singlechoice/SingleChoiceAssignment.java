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
@Builder(toBuilder = true)  // Dla immutable operations
@NoArgsConstructor
@AllArgsConstructor
public class SingleChoiceAssignment extends Assignment {

    @Builder.Default
    private List<ChoiceOption> options = List.of();

    @Builder.Default
    private boolean randomizeOptions = false;

    @Builder(builderMethodName = "singleChoiceBuilder")
    public SingleChoiceAssignment(Long id, Long testId, String title, String description,
                                  Integer orderNumber, Integer points, LocalDateTime createdAt,
                                  LocalDateTime updatedAt, List<ChoiceOption> options,
                                  boolean randomizeOptions) {
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
}