package com.edutest.persistance.entity.assigment.coding;

import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.test.TestCaseResultEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@DiscriminatorValue("CODING")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodingAssignmentEntity extends AssignmentEntity {

    @Column(name = "time_limit_ms")
    private Integer timeLimitMs;

    @Column(name = "memory_limit_mb")
    private Integer memoryLimitMb;

    @Column(name = "allowed_languages", length = 500)
    private String allowedLanguagesStr;

    @Column(name = "starter_code", length = 5000)
    private String starterCode;

    @Column(name = "solution_template", length = 5000)
    private String solutionTemplate;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestCaseEntity> testCases = new ArrayList<>();


    @Override
    public AssignmentType getType() {
        return AssignmentType.CODING;
    }

    @Override
    public boolean isValidAnswer(String answer) {
        return answer != null && !answer.trim().isEmpty();
    }

    @Override
    public float calculateScore(String answer) {
        return 0.0f;
    }
}
