package com.edutest.persistance.entity.assigment.coding;


import com.edutest.persistance.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CodingAssignmentEntity assignment;

    @Column(name = "input_data", length = 2000)
    private String inputData;

    @Column(name = "expected_output", length = 2000)
    private String expectedOutput;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Integer weight = 1;
}
