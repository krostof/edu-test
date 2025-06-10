package com.edutest.persistance.entity.test;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@DiscriminatorValue("SINGLE_CHOICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SingleChoiceAssignment extends Assignment {

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderNumber ASC")
    private List<ChoiceOption> options = new ArrayList<>();

    @Column(name = "randomize_options")
    @Builder.Default
    private Boolean randomizeOptions = false;

    @Override
    public AssignmentType getType() {
        return AssignmentType.SINGLE_CHOICE;
    }

    @Override
    public boolean isValidAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return false;
        }

        try {
            Long optionId = Long.parseLong(answer.trim());
            return options.stream()
                    .anyMatch(option -> option.getId().equals(optionId));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public float calculateScore(String answer) {
        if (!isValidAnswer(answer)) {
            return 0.0f;
        }

        try {
            Long selectedOptionId = Long.parseLong(answer.trim());
            ChoiceOption selectedOption = options.stream()
                    .filter(option -> option.getId().equals(selectedOptionId))
                    .findFirst()
                    .orElse(null);

            if (selectedOption != null && selectedOption.isCorrectAnswer()) {
                return getPoints();
            }
        } catch (NumberFormatException e) {
            // Invalid format
        }

        return 0.0f;
    }

    // Business methods
    public void addOption(String optionText, boolean isCorrect) {
        addOption(optionText, isCorrect, null);
    }

    public void addOption(String optionText, boolean isCorrect, String explanation) {
        ChoiceOption option = ChoiceOption.builder()
                .assignment(this)
                .optionText(optionText)
                .isCorrect(isCorrect)
                .orderNumber(options.size() + 1)
                .explanation(explanation)
                .build();

        options.add(option);
    }

    public ChoiceOption getCorrectOption() {
        return options.stream()
                .filter(ChoiceOption::isCorrectAnswer)
                .findFirst()
                .orElse(null);
    }

    public List<ChoiceOption> getCorrectOptions() {
        return options.stream()
                .filter(ChoiceOption::isCorrectAnswer)
                .toList();
    }

    public boolean hasCorrectAnswer() {
        return options.stream().anyMatch(ChoiceOption::isCorrectAnswer);
    }

    public int getOptionsCount() {
        return options.size();
    }

    @PrePersist
    @PreUpdate
    private void validateSingleChoice() {
        long correctCount = options.stream()
                .filter(ChoiceOption::isCorrectAnswer)
                .count();

        if (correctCount != 1) {
            throw new IllegalStateException("Single choice assignment must have exactly one correct answer");
        }
    }
}
