package com.edutest.persistance.entity.test;


import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("SINGLE_CHOICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SingleChoiceAnswer extends AssignmentAnswer {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private ChoiceOption selectedOption;

    @Override
    public boolean isCorrect() {
        return selectedOption != null && selectedOption.isCorrectAnswer();
    }

    @Override
    public float calculateScore() {
        if (isCorrect()) {
            return getAssignment().getPoints();
        }
        return 0.0f;
    }

    @Override
    public String getAnswerText() {
        return selectedOption != null ? selectedOption.getOptionText() : null;
    }

    public Long getSelectedOptionId() {
        return selectedOption != null ? selectedOption.getId() : null;
    }
}
