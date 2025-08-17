package com.edutest.persistance.entity.assigment.common;

import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "choice_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoiceOptionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AssignmentEntity assignmentEntity;

    @Column(name = "option_text", nullable = false, length = 1000)
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "explanation", length = 500)
    private String explanation; // Wyjaśnienie dlaczego opcja jest poprawna/niepoprawna

    // Business methods
    public boolean isCorrectAnswer() {
        return Boolean.TRUE.equals(isCorrect);
    }

    public String getDisplayText() {
        return optionText;
    }
}
