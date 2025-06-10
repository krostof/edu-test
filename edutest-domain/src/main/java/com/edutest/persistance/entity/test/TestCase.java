package com.edutest.persistance.entity.test;


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
public class TestCase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CodingAssignment assignment;

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

    public boolean matches(String actualOutput) {
        if (expectedOutput == null && actualOutput == null) {
            return true;
        }
        if (expectedOutput == null || actualOutput == null) {
            return false;
        }

        String normalizedExpected = expectedOutput.trim();
        String normalizedActual = actualOutput.trim();

        return normalizedExpected.equals(normalizedActual);
    }
}
