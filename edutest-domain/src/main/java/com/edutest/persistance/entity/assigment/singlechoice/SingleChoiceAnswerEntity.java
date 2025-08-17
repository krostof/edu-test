package com.edutest.persistance.entity.assigment.singlechoice;


import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("SINGLE_CHOICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SingleChoiceAnswerEntity extends AssignmentAnswerEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private ChoiceOptionEntity selectedOption;

    @Override
    public boolean isCorrect() {
        return selectedOption != null && selectedOption.isCorrectAnswer();
    }

    @Override
    public float calculateScore() {
        if (isCorrect()) {
            return getAssignmentEntity().getPoints();
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
