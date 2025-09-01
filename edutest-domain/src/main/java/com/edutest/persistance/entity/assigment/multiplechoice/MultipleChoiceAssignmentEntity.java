package com.edutest.persistance.entity.assigment.multiplechoice;

import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Entity
@DiscriminatorValue("MULTIPLE_CHOICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MultipleChoiceAssignmentEntity extends AssignmentEntity {

    @OneToMany(mappedBy = "assignmentEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderNumber ASC")
    @Builder.Default
    private List<ChoiceOptionEntity> options = new ArrayList<>();

    @Column(name = "randomize_options")
    @Builder.Default
    private Boolean randomizeOptions = false;

    @Column(name = "partial_scoring")
    @Builder.Default
    private Boolean partialScoring = true;

    @Column(name = "penalty_for_wrong")
    @Builder.Default
    private Boolean penaltyForWrong = false;

    @Override
    public AssignmentType getType() {
        return AssignmentType.MULTIPLE_CHOICE;
    }

    @Override
    public boolean isValidAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return false;
        }

        try {
            List<Long> selectedIds = parseSelectedOptions(answer);
            return selectedIds.stream()
                    .allMatch(id -> options.stream()
                            .anyMatch(option -> option.getId().equals(id)));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public float calculateScore(String answer) {
        if (!isValidAnswer(answer)) {
            return 0.0f;
        }

        try {
            List<Long> selectedIds = parseSelectedOptions(answer);
            List<ChoiceOptionEntity> correctOptions = getCorrectOptions();
            List<ChoiceOptionEntity> selectedOptions = options.stream()
                    .filter(option -> selectedIds.contains(option.getId()))
                    .collect(Collectors.toList());

            if (!partialScoring) {
                boolean allCorrect = selectedOptions.size() == correctOptions.size() &&
                        selectedOptions.stream().allMatch(ChoiceOptionEntity::isCorrectAnswer);
                return allCorrect ? getPoints() : 0.0f;
            } else {
                return calculatePartialScore(selectedOptions, correctOptions);
            }
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private float calculatePartialScore(List<ChoiceOptionEntity> selectedOptions, List<ChoiceOptionEntity> correctOptions) {
        int correctCount = correctOptions.size();
        if (correctCount == 0) {
            return 0.0f;
        }

        long correctSelections = selectedOptions.stream()
                .filter(ChoiceOptionEntity::isCorrectAnswer)
                .count();

        long wrongSelections = selectedOptions.stream()
                .filter(option -> !option.isCorrectAnswer())
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

    public void addOption(String optionText, boolean isCorrect) {
        addOption(optionText, isCorrect, null);
    }

    public void addOption(String optionText, boolean isCorrect, String explanation) {
        ChoiceOptionEntity option = ChoiceOptionEntity.builder()
                .assignmentEntity(this)
                .optionText(optionText)
                .isCorrect(isCorrect)
                .orderNumber(options.size() + 1)
                .explanation(explanation)
                .build();

        options.add(option);
    }

    public List<ChoiceOptionEntity> getCorrectOptions() {
        return options.stream()
                .filter(ChoiceOptionEntity::isCorrectAnswer)
                .collect(Collectors.toList());
    }

    public List<ChoiceOptionEntity> getIncorrectOptions() {
        return options.stream()
                .filter(option -> !option.isCorrectAnswer())
                .collect(Collectors.toList());
    }

    public boolean hasCorrectAnswers() {
        return options.stream().anyMatch(ChoiceOptionEntity::isCorrectAnswer);
    }

    public int getCorrectOptionsCount() {
        return getCorrectOptions().size();
    }

    public int getOptionsCount() {
        return options.size();
    }

    @PrePersist
    @PreUpdate
    private void validateMultipleChoice() {
        long correctCount = options.stream()
                .filter(ChoiceOptionEntity::isCorrectAnswer)
                .count();

        if (correctCount < 1) {
            throw new IllegalStateException("Multiple choice assignment must have at least one correct answer");
        }

        if (correctCount >= options.size()) {
            throw new IllegalStateException("Multiple choice assignment cannot have all options as correct");
        }
    }
}
