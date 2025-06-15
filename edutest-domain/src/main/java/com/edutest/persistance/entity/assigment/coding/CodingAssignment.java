package com.edutest.persistance.entity.assigment.coding;

import com.edutest.persistance.entity.assigment.Assignment;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.test.TestCaseResult;
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
public class CodingAssignment extends Assignment {

    @Column(name = "time_limit_ms")
    private Integer timeLimitMs; // Limit czasowy wykonania w milisekundach

    @Column(name = "memory_limit_mb")
    private Integer memoryLimitMb; // Limit pamięci w MB

    @Column(name = "allowed_languages", length = 500)
    private String allowedLanguagesStr; // Zapisane jako string rozdzielony przecinkami

    @Column(name = "starter_code", length = 5000)
    private String starterCode; // Kod startowy dla studenta

    @Column(name = "solution_template", length = 5000)
    private String solutionTemplate; // Template rozwiązania

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestCase> testCases = new ArrayList<>();

    @Override
    public AssignmentType getType() {
        return AssignmentType.CODING;
    }

    @Override
    public boolean isValidAnswer(String answer) {
        // Podstawowa walidacja - czy kod nie jest pusty
        return answer != null && !answer.trim().isEmpty();
    }

    @Override
    public float calculateScore(String answer) {
        // W przypadku zadań programistycznych, punkty są obliczane
        // na podstawie przejścia testów - implementacja w serwisie
        return 0.0f;
    }

    // Getter i setter dla allowed languages jako List
    public List<String> getAllowedLanguages() {
        if (allowedLanguagesStr == null || allowedLanguagesStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(allowedLanguagesStr.split(","));
    }

    public void setAllowedLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty()) {
            this.allowedLanguagesStr = null;
        } else {
            this.allowedLanguagesStr = String.join(",", languages);
        }
    }

    public void addAllowedLanguage(String language) {
        List<String> current = getAllowedLanguages();
        if (!current.contains(language)) {
            current.add(language);
            setAllowedLanguages(current);
        }
    }

    public boolean isLanguageAllowed(String language) {
        return getAllowedLanguages().contains(language);
    }

    public void addTestCase(TestCase testCase) {
        if (testCase != null) {
            testCase.setAssignment(this);
            testCases.add(testCase);
        }
    }

    public int getPassingTestCasesCount(List<TestCaseResult> results) {
        return (int) results.stream()
                .filter(TestCaseResult::isPassed)
                .count();
    }

    public float calculateScoreFromResults(List<TestCaseResult> results) {
        if (testCases.isEmpty()) {
            return 0.0f;
        }

        int passedCount = getPassingTestCasesCount(results);
        float percentage = (float) passedCount / testCases.size();
        return percentage * getPoints();
    }
}
