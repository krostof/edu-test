package com.edutest.domain.assignment.common;

import lombok.*;

@Data
@Builder
@With
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChoiceOption {

    private Long id;
    private Long assignmentId;
    private String optionText;
    private boolean correct;
    private Integer orderNumber;
    private String explanation;

    public static ChoiceOption create(String optionText, boolean correct, Integer orderNumber) {
        return ChoiceOption.builder()
                .optionText(optionText)
                .correct(correct)
                .orderNumber(orderNumber)
                .build();
    }

    public static ChoiceOption create(String optionText, boolean correct, Integer orderNumber,
                                      String explanation) {
        return ChoiceOption.builder()
                .optionText(optionText)
                .correct(correct)
                .orderNumber(orderNumber)
                .explanation(explanation)
                .build();
    }

    public boolean isCorrect() {
        return correct;
    }

    public boolean hasExplanation() {
        return explanation != null && !explanation.trim().isEmpty();
    }

    public String getDisplayText() {
        return optionText;
    }

    public ChoiceOption markAsCorrect() {
        return withCorrect(true);
    }

    public ChoiceOption markAsIncorrect() {
        return withCorrect(false);
    }

    public ChoiceOption updateText(String newText) {
        return withOptionText(newText);
    }

    public ChoiceOption updateExplanation(String newExplanation) {
        return withExplanation(newExplanation);
    }

    public ChoiceOption updateOrder(Integer newOrder) {
        return withOrderNumber(newOrder);
    }

    // Validation
    public boolean isValid() {
        return optionText != null &&
                !optionText.trim().isEmpty() &&
                orderNumber != null &&
                orderNumber > 0;
    }
}