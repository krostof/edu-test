package com.edutest.domain.assignment.multiplechoice;



import com.edutest.domain.assignment.common.AssignmentAnswer;
import com.edutest.domain.assignment.common.ChoiceOption;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class MultipleChoiceAnswer extends AssignmentAnswer {

    private List<Long> selectedOptionIds;
    private List<ChoiceOption> selectedOptions; // Cache - opcjonalnie dla performance

    @Builder(builderMethodName = "multipleChoiceAnswerBuilder")
    public MultipleChoiceAnswer(Long id, Long assignmentId, Long testAttemptId, Long studentId,
                                LocalDateTime answeredAt, Float score, boolean graded, String teacherFeedback,
                                List<Long> selectedOptionIds, List<ChoiceOption> selectedOptions) {
        super(id, assignmentId, testAttemptId, studentId, answeredAt, score, graded, teacherFeedback);
        this.selectedOptionIds = selectedOptionIds != null ? new ArrayList<>(selectedOptionIds) : new ArrayList<>();
        this.selectedOptions = selectedOptions != null ? new ArrayList<>(selectedOptions) : new ArrayList<>();
    }

    // Business methods
    @Override
    public boolean isCorrect() {
        // For multiple choice, "correct" means all selected are correct AND all correct are selected
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return false;
        }

        return selectedOptions.stream().allMatch(ChoiceOption::isCorrect);
    }

    @Override
    public Float calculateScore(Integer maxPoints) {
        // Score calculation needs assignment context - will be done in autoGrade()
        return getScore(); // Return already calculated score
    }

    @Override
    public String getAnswerText() {
        if (selectedOptions != null && !selectedOptions.isEmpty()) {
            return selectedOptions.stream()
                    .map(ChoiceOption::getOptionText)
                    .collect(Collectors.joining("; "));
        } else if (selectedOptionIds != null && !selectedOptionIds.isEmpty()) {
            return "Selected option IDs: " + selectedOptionIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
        return "No options selected";
    }

    @Override
    public boolean hasAnswer() {
        return selectedOptionIds != null && !selectedOptionIds.isEmpty();
    }

    @Override
    public boolean isValidAnswer() {
        return hasAnswer();
    }

    @Override
    public String getValidationError() {
        if (!hasAnswer()) {
            return "No options selected";
        }
        return null; // No error
    }

    // Multiple choice specific methods
    public void selectOption(ChoiceOption option) {
        if (option == null) {
            throw new IllegalArgumentException("Option cannot be null");
        }

        if (!isOptionSelected(option.getId())) {
            selectedOptionIds.add(option.getId());
            selectedOptions.add(option);
            setAnsweredAt(LocalDateTime.now());
        }
    }

    public void selectOptionById(Long optionId) {
        if (optionId == null) {
            throw new IllegalArgumentException("Option ID cannot be null");
        }

        if (!isOptionSelected(optionId)) {
            selectedOptionIds.add(optionId);
            setAnsweredAt(LocalDateTime.now());
        }
    }

    public void deselectOption(ChoiceOption option) {
        if (option != null) {
            deselectOptionById(option.getId());
        }
    }

    public void deselectOptionById(Long optionId) {
        if (optionId != null) {
            selectedOptionIds.remove(optionId);
            selectedOptions.removeIf(option -> option.getId().equals(optionId));

            if (!hasAnswer()) {
                setAnsweredAt(null); // Reset if no selections remain
            }
        }
    }

    public void clearAllSelections() {
        selectedOptionIds.clear();
        selectedOptions.clear();
        setAnsweredAt(null);
    }

    public void setSelectedOptions(List<ChoiceOption> options) {
        clearAllSelections();
        if (options != null) {
            for (ChoiceOption option : options) {
                selectOption(option);
            }
        }
    }

    public void setSelectedOptionIds(List<Long> optionIds) {
        clearAllSelections();
        if (optionIds != null) {
            for (Long optionId : optionIds) {
                selectOptionById(optionId);
            }
        }
    }

    public boolean isOptionSelected(Long optionId) {
        return selectedOptionIds.contains(optionId);
    }

    public boolean isOptionSelected(ChoiceOption option) {
        return option != null && isOptionSelected(option.getId());
    }

    public int getSelectedCount() {
        return selectedOptionIds.size();
    }

    public List<ChoiceOption> getCorrectSelections() {
        return selectedOptions.stream()
                .filter(ChoiceOption::isCorrect)
                .collect(Collectors.toList());
    }

    public List<ChoiceOption> getIncorrectSelections() {
        return selectedOptions.stream()
                .filter(option -> !option.isCorrect())
                .collect(Collectors.toList());
    }

    public boolean hasAnyCorrectSelections() {
        return selectedOptions.stream().anyMatch(ChoiceOption::isCorrect);
    }

    public boolean hasAnyIncorrectSelections() {
        return selectedOptions.stream().anyMatch(option -> !option.isCorrect());
    }

    public float getAccuracyPercentage() {
        if (selectedOptions.isEmpty()) {
            return 0.0f;
        }

        long correctCount = getCorrectSelections().size();
        return (float) correctCount / selectedOptions.size() * 100.0f;
    }

    // Auto-grading with assignment context
    public void autoGrade(MultipleChoiceAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }

        // Load selected options if not present
        if (selectedOptions.isEmpty() && !selectedOptionIds.isEmpty()) {
            selectedOptions = selectedOptionIds.stream()
                    .map(assignment::findOptionById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // Calculate score using assignment's scoring logic
        String answerString = selectedOptionIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Float calculatedScore = assignment.calculateScore(answerString);
        setScore(calculatedScore);
        setGraded(true);
    }

    // Analysis methods
    public SelectionAnalysis analyzeSelections(MultipleChoiceAssignment assignment) {
        List<ChoiceOption> correctOptions = assignment.getCorrectOptions();
        List<ChoiceOption> correctSelections = getCorrectSelections();
        List<ChoiceOption> incorrectSelections = getIncorrectSelections();

        List<ChoiceOption> missedCorrect = correctOptions.stream()
                .filter(option -> !isOptionSelected(option))
                .collect(Collectors.toList());

        return SelectionAnalysis.builder()
                .totalSelected(getSelectedCount())
                .correctSelected(correctSelections.size())
                .incorrectSelected(incorrectSelections.size())
                .totalCorrectAvailable(correctOptions.size())
                .missedCorrect(missedCorrect.size())
                .isPerfect(correctSelections.size() == correctOptions.size() && incorrectSelections.isEmpty())
                .hasErrors(incorrectSelections.size() > 0 || missedCorrect.size() > 0)
                .build();
    }

    // Display methods
    public String getSelectedOptionsText() {
        return selectedOptions.stream()
                .map(ChoiceOption::getOptionText)
                .collect(Collectors.joining(", "));
    }

    public List<String> getSelectedOptionsExplanations() {
        return selectedOptions.stream()
                .filter(ChoiceOption::hasExplanation)
                .map(ChoiceOption::getExplanation)
                .collect(Collectors.toList());
    }
}