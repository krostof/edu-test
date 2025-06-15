package com.edutest.domain.assignment.singlechoice;


import com.edutest.domain.assignment.common.AssignmentAnswer;
import com.edutest.domain.assignment.common.ChoiceOption;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Domain model dla odpowiedzi na zadanie jednokrotnego wyboru
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SingleChoiceAnswer extends AssignmentAnswer {

    private Long selectedOptionId;
    private ChoiceOption selectedOption; // Opcjonalnie - dla cache/performance

    @Builder(builderMethodName = "singleChoiceAnswerBuilder")
    public SingleChoiceAnswer(Long id, Long assignmentId, Long testAttemptId, Long studentId,
                              LocalDateTime answeredAt, Float score, boolean graded, String teacherFeedback,
                              Long selectedOptionId, ChoiceOption selectedOption) {
        super(id, assignmentId, testAttemptId, studentId, answeredAt, score, graded, teacherFeedback);
        this.selectedOptionId = selectedOptionId;
        this.selectedOption = selectedOption;
    }

    // Business methods
    @Override
    public boolean isCorrect() {
        return selectedOption != null && selectedOption.isCorrect();
    }

    @Override
    public Float calculateScore(Integer maxPoints) {
        if (isCorrect() && maxPoints != null) {
            return maxPoints.floatValue();
        }
        return 0.0f;
    }

    @Override
    public String getAnswerText() {
        return selectedOption != null ? selectedOption.getOptionText() :
                (selectedOptionId != null ? "Option ID: " + selectedOptionId : "No answer");
    }

    @Override
    public boolean hasAnswer() {
        return selectedOptionId != null;
    }

    // Single choice specific methods
    public void selectOption(ChoiceOption option) {
        if (option == null) {
            throw new IllegalArgumentException("Option cannot be null");
        }
        this.selectedOptionId = option.getId();
        this.selectedOption = option;
        setAnsweredAt(LocalDateTime.now());
    }

    public void selectOptionById(Long optionId) {
        if (optionId == null) {
            throw new IllegalArgumentException("Option ID cannot be null");
        }
        this.selectedOptionId = optionId;
        this.selectedOption = null; // Will be loaded separately
        setAnsweredAt(LocalDateTime.now());
    }

    public void clearSelection() {
        this.selectedOptionId = null;
        this.selectedOption = null;
    }

    public boolean hasSelection() {
        return selectedOptionId != null;
    }

    public boolean isOptionSelected(Long optionId) {
        return selectedOptionId != null && selectedOptionId.equals(optionId);
    }

    public boolean isOptionSelected(ChoiceOption option) {
        return option != null && isOptionSelected(option.getId());
    }

    // Validation
    @Override
    public boolean isValidAnswer() {
        return selectedOptionId != null;
    }

    @Override
    public String getValidationError() {
        if (selectedOptionId == null) {
            return "No option selected";
        }
        return null; // No error
    }

    // Auto-grading
    public void autoGrade(SingleChoiceAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }

        // Load selected option if not present
        if (selectedOption == null && selectedOptionId != null) {
            selectedOption = assignment.findOptionById(selectedOptionId);
        }

        // Calculate score
        Float calculatedScore = calculateScore(assignment.getPoints());
        setScore(calculatedScore);
        setGraded(true);
    }

    // Display methods
    public String getSelectedOptionText() {
        return selectedOption != null ? selectedOption.getOptionText() : "Unknown option";
    }

    public String getSelectedOptionExplanation() {
        return selectedOption != null ? selectedOption.getExplanation() : null;
    }

    public boolean hasExplanation() {
        return selectedOption != null && selectedOption.hasExplanation();
    }
}