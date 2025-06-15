package com.edutest.persistance.entity.assigment.multiplechoice;

import com.edutest.persistance.entity.assigment.common.AssignmentAnswer;
import com.edutest.persistance.entity.assigment.common.ChoiceOption;
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
public class MultipleChoiceAnswer extends AssignmentAnswer {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "multiple_choice_selected_options",
            joinColumns = @JoinColumn(name = "answer_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    private List<ChoiceOption> selectedOptions = new ArrayList<>();

    @Override
    public boolean isCorrect() {
        MultipleChoiceAssignment assignment = (MultipleChoiceAssignment) getAssignment();
        List<ChoiceOption> correctOptions = assignment.getCorrectOptions();

        // Check if selected options match exactly with correct options
        return selectedOptions.size() == correctOptions.size() &&
                selectedOptions.stream().allMatch(ChoiceOption::isCorrectAnswer);
    }

    @Override
    public float calculateScore() {
        MultipleChoiceAssignment assignment = (MultipleChoiceAssignment) getAssignment();

        // Convert selected options to comma-separated string format expected by assignment
        String answerString = selectedOptions.stream()
                .map(option -> option.getId().toString())
                .collect(Collectors.joining(","));

        return assignment.calculateScore(answerString);
    }

    @Override
    public String getAnswerText() {
        return selectedOptions.stream()
                .map(ChoiceOption::getOptionText)
                .collect(Collectors.joining("; "));
    }

    // Business methods
    public void addSelectedOption(ChoiceOption option) {
        if (option != null && !selectedOptions.contains(option)) {
            selectedOptions.add(option);
        }
    }

    public void removeSelectedOption(ChoiceOption option) {
        selectedOptions.remove(option);
    }

    public void setSelectedOptions(List<ChoiceOption> options) {
        this.selectedOptions.clear();
        if (options != null) {
            this.selectedOptions.addAll(options);
        }
    }

    public List<Long> getSelectedOptionIds() {
        return selectedOptions.stream()
                .map(ChoiceOption::getId)
                .collect(Collectors.toList());
    }

    public boolean isOptionSelected(ChoiceOption option) {
        return selectedOptions.contains(option);
    }

    public boolean isOptionSelected(Long optionId) {
        return selectedOptions.stream()
                .anyMatch(option -> option.getId().equals(optionId));
    }

    public int getSelectedCount() {
        return selectedOptions.size();
    }

    public List<ChoiceOption> getCorrectSelections() {
        return selectedOptions.stream()
                .filter(ChoiceOption::isCorrectAnswer)
                .collect(Collectors.toList());
    }

    public List<ChoiceOption> getIncorrectSelections() {
        return selectedOptions.stream()
                .filter(option -> !option.isCorrectAnswer())
                .collect(Collectors.toList());
    }

    public boolean hasAnyCorrectSelections() {
        return selectedOptions.stream().anyMatch(ChoiceOption::isCorrectAnswer);
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
