package com.edutest.persistance.entity.assigment;

import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.test.TestEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assignments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "assignment_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AssignmentEntity extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "points", nullable = false)
    private Integer points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private TestEntity testEntity;

    // Metoda abstrakcyjna do określenia typu zadania
    public abstract AssignmentType getType();

    // Metoda do walidacji odpowiedzi (implementowana w podklasach)
    public abstract boolean isValidAnswer(String answer);

    // Metoda do obliczania punktów za odpowiedź (implementowana w podklasach)
    public abstract float calculateScore(String answer);
}
