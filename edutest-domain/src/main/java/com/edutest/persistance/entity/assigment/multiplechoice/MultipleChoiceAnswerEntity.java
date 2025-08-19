package com.edutest.persistance.entity.assigment.multiplechoice;

import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("MULTIPLE_CHOICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MultipleChoiceAnswerEntity extends AssignmentAnswerEntity {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "multiple_choice_selected_options",
            joinColumns = @JoinColumn(name = "answer_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    private List<ChoiceOptionEntity> selectedOptions = new ArrayList<>();

    @Override
    public boolean isCorrect() {
        MultipleChoiceAssignmentEntityEntity assignment = (MultipleChoiceAssignmentEntityEntity) getAssignmentEntity();
        List<ChoiceOptionEntity> correctOptions = assignment.getCorrectOptions();

        return selectedOptions.size() == correctOptions.size() &&
                selectedOptions.stream().allMatch(ChoiceOptionEntity::isCorrectAnswer);
    }

    @Override
    public float calculateScore() {
        MultipleChoiceAssignmentEntityEntity assignment = (MultipleChoiceAssignmentEntityEntity) getAssignmentEntity();

        String answerString = selectedOptions.stream()
                .map(option -> option.getId().toString())
                .collect(Collectors.joining(","));

        return assignment.calculateScore(answerString);
    }

    @Override
    public String getAnswerText() {
        return selectedOptions.stream()
                .map(ChoiceOptionEntity::getOptionText)
                .collect(Collectors.joining("; "));
    }

    public void addSelectedOption(ChoiceOptionEntity option) {
        if (option != null && !selectedOptions.contains(option)) {
            selectedOptions.add(option);
        }
    }

    public void removeSelectedOption(ChoiceOptionEntity option) {
        selectedOptions.remove(option);
    }

    public void setSelectedOptions(List<ChoiceOptionEntity> options) {
        this.selectedOptions.clear();
        if (options != null) {
            this.selectedOptions.addAll(options);
        }
    }

    public List<Long> getSelectedOptionIds() {
        return selectedOptions.stream()
                .map(ChoiceOptionEntity::getId)
                .collect(Collectors.toList());
    }

    public boolean isOptionSelected(ChoiceOptionEntity option) {
        return selectedOptions.contains(option);
    }

    public boolean isOptionSelected(Long optionId) {
        return selectedOptions.stream()
                .anyMatch(option -> option.getId().equals(optionId));
    }

    public int getSelectedCount() {
        return selectedOptions.size();
    }

    public List<ChoiceOptionEntity> getCorrectSelections() {
        return selectedOptions.stream()
                .filter(ChoiceOptionEntity::isCorrectAnswer)
                .collect(Collectors.toList());
    }

    public List<ChoiceOptionEntity> getIncorrectSelections() {
        return selectedOptions.stream()
                .filter(option -> !option.isCorrectAnswer())
                .collect(Collectors.toList());
    }

    public boolean hasAnyCorrectSelections() {
        return selectedOptions.stream().anyMatch(ChoiceOptionEntity::isCorrectAnswer);
    }

    public boolean hasAnyIncorrectSelections() {
        return selectedOptions.stream().anyMatch(option -> !option.isCorrectAnswer());
    }

    public float getAccuracyPercentage() {
        if (selectedOptions.isEmpty()) {
            return 0.0f;
        }

        long correctCount = getCorrectSelections().size();
        return (float) correctCount / selectedOptions.size() * 100.0f;
    }
}
