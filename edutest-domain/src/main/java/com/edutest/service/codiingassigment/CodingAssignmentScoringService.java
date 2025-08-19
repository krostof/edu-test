package com.edutest.service.codiingassigment;

import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.assignment.coding.ScoringBreakdown;
import com.edutest.domain.assignment.coding.TestCaseExecutionResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodingAssignmentScoringService {

    public Float calculateScoreFromTestResults(CodingAssignment assignment,
                                               List<TestCaseExecutionResult> results) {
        if (assignment.getTestCases().isEmpty() || results.isEmpty()) {
            return 0.0f;
        }

        int passedCount = (int) results.stream()
                .filter(TestCaseExecutionResult::isPassed)
                .count();

        float percentage = (float) passedCount / assignment.getTestCases().size();
        return percentage * assignment.getPoints();
    }

    public ScoringBreakdown calculateDetailedScore(CodingAssignment assignment,
                                                   List<TestCaseExecutionResult> results) {
        int totalTests = assignment.getTestCases().size();
        int passedTests = (int) results.stream().filter(TestCaseExecutionResult::isPassed).count();
        int failedTests = totalTests - passedTests;

        float score = calculateScoreFromTestResults(assignment, results);
        float percentage = totalTests > 0 ? (float) passedTests / totalTests * 100 : 0;

        return ScoringBreakdown.builder()
                .totalTestCases(totalTests)
                .passedTestCases(passedTests)
                .failedTestCases(failedTests)
                .score(score)
                .maxScore(assignment.getPoints().floatValue())
                .percentage(percentage)
                .isPerfect(passedTests == totalTests)
                .build();
    }
}
