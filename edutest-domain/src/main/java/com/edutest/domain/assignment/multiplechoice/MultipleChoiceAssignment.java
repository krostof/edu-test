package com.edutest.domain.assignment.multiplechoice;


import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.assignment.AssignmentType;
import com.edutest.domain.assignment.ValidationResult;
import com.edutest.domain.assignment.common.ChoiceOption;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain model dla zadania wielokrotnego wyboru
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class MultipleChoiceAssignment extends Assignment {

    private List<ChoiceOption> options;
    private boolean randomizeOptions;
    private boolean partialScoring;
    private boolean penaltyForWrong;

    @Builder(builderMethodName = "multipleChoiceBuilder")
    public MultipleChoiceAssignment(Long id, Long testId, String title, String description,
                                    Integer orderNumber, Integer points, LocalDateTime createdAt,
                                    LocalDateTime updatedAt, List<ChoiceOption> options,
                                    boolean randomizeOptions, boolean partialScoring, boolean penaltyForWrong) {
        super(id, testId, title, description, orderNumber, points, createdAt, updatedAt);
        this.options = options != null ? List.copyOf(options) : List.of();
        this.randomizeOptions = randomizeOptions;
        this.partialScoring = partialScoring;
        this.penaltyForWrong = penaltyForWrong;
    }

    @Override
    public AssignmentType getType() {
        return AssignmentType.MULTIPLE_CHOICE;
    }

    @Override
    public ValidationResult validateAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return ValidationResult.invalid("Answer cannot be empty");
        }

        try {
            List<Long> selectedIds = parseSelectedOptions(answer);
            if (selectedIds.isEmpty()) {
                return ValidationResult.invalid("At least one option must be selected");
            }

            boolean allValidOptions = selectedIds.stream()
                    .allMatch(id -> options.stream()
                            .anyMatch(option -> option.getId().equals(id)));

            return allValidOptions ? ValidationResult.valid() :
                    ValidationResult.invalid("Invalid options selected");
        } catch (Exception e) {
            return ValidationResult.invalid("Invalid answer format. Use comma-separated option IDs");
        }
    }

    @Override
    public Float calculateScore(String answer) {
        ValidationResult validation = validateAnswer(answer);
        if (validation.hasError()) {
            return 0.0f;
        }

        try {
            List<Long> selectedIds = parseSelectedOptions(answer);
            List<ChoiceOption> correctOptions = getCorrectOptions();
            List<ChoiceOption> selectedOptions = options.stream()
                    .filter(option -> selectedIds.contains(option.getId()))
                    .collect(Collectors.toList());

            if (!partialScoring) {
                boolean allCorrect = selectedOptions.size() == correctOptions.size() &&
                        selectedOptions.stream().allMatch(ChoiceOption::isCorrect);
                return allCorrect ? getPoints().floatValue() : 0.0f;
            } else {
                return calculatePartialScore(selectedOptions, correctOptions);
            }
        } catch (Exception e) {
            return 0.0f;
        }
    }

    @Override
    public boolean supportsAttachments() {
        return true;
    }

    private Float calculatePartialScore(List<ChoiceOption> selectedOptions, List<ChoiceOption> correctOptions) {
        int correctCount = correctOptions.size();
        if (correctCount == 0) {
            return 0.0f;
        }

        long correctSelections = selectedOptions.stream()
                .filter(ChoiceOption::isCorrect)
                .count();

        long wrongSelections = selectedOptions.stream()
                .filter(option -> !option.isCorrect())
                .count();

        float score = (float) correctSelections / correctCount;

        if (penaltyForWrong) {
            float penalty = (float) wrongSelections / options.size();
            score = Math.max(0, score - penalty);
        }

        return score * getPoints();
    }

    private List<Long> parseSelectedOptions(String answer) {
        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

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
                .collect(Collectors.toList());
        setUpdatedAt(LocalDateTime.now());
    }

    public void updateOption(Long optionId, ChoiceOption newOption) {
        this.options = options.stream()
                .map(option -> option.getId().equals(optionId) ? newOption : option)
                .collect(Collectors.toList());
        setUpdatedAt(LocalDateTime.now());
    }

    public List<ChoiceOption> getCorrectOptions() {
        return options.stream()
                .filter(ChoiceOption::isCorrect)
                .collect(Collectors.toList());
    }

    public List<ChoiceOption> getIncorrectOptions() {
        return options.stream()
                .filter(option -> !option.isCorrect())
                .collect(Collectors.toList());
    }

    public boolean hasCorrectAnswers() {
        return options.stream().anyMatch(ChoiceOption::isCorrect);
    }

    public int getCorrectOptionsCount() {
        return getCorrectOptions().size();
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
        validateMultipleChoice();
    }

    // Scoring configuration
    public void enablePartialScoring() {
        this.partialScoring = true;
        setUpdatedAt(LocalDateTime.now());
    }

    public void disablePartialScoring() {
        this.partialScoring = false;
        setUpdatedAt(LocalDateTime.now());
    }

    public void enablePenaltyForWrong() {
        this.penaltyForWrong = true;
        setUpdatedAt(LocalDateTime.now());
    }

    public void disablePenaltyForWrong() {
        this.penaltyForWrong = false;
        setUpdatedAt(LocalDateTime.now());
    }

    public ValidationResult validateConfiguration() {
        if (!hasCorrectAnswers()) {
            return ValidationResult.invalid("Multiple choice assignment must have at least one correct answer");
        }

        if (getCorrectOptionsCount() >= getOptionsCount()) {
            return ValidationResult.invalid("Multiple choice assignment cannot have all options as correct");
        }

        if (getOptionsCount() < 2) {
            return ValidationResult.invalid("Multiple choice assignment must have at least 2 options");
        }

        return ValidationResult.valid();
    }

    private void validateMultipleChoice() {
        if (options == null || options.isEmpty()) {
            return;
        }

        ValidationResult result = validateConfiguration();
        if (result.hasError()) {
            throw new IllegalStateException(result.getErrorMessage());
        }
    }

    public ScoringAnalysis analyzeScoringImpact(List<Long> selectedIds) {
        List<ChoiceOption> selectedOptions = options.stream()
                .filter(option -> selectedIds.contains(option.getId()))
                .collect(Collectors.toList());

        List<ChoiceOption> correctOptions = getCorrectOptions();

        long correctSelections = selectedOptions.stream().filter(ChoiceOption::isCorrect).count();
        long wrongSelections = selectedOptions.stream().filter(o -> !o.isCorrect()).count();
        long missedCorrect = correctOptions.size() - correctSelections;

        float score = calculatePartialScore(selectedOptions, correctOptions);

        return ScoringAnalysis.builder()
                .correctSelections((int) correctSelections)
                .wrongSelections((int) wrongSelections)
                .missedCorrect((int) missedCorrect)
                .totalCorrect(correctOptions.size())
                .score(score)
                .maxScore(getPoints().floatValue())
                .percentage(score / getPoints() * 100)
                .build();
    }
}