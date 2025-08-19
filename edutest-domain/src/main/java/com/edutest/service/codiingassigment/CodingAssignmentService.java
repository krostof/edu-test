package com.edutest.service.codiingassigment;

import com.edutest.domain.assignment.ValidationResult;
import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.assignment.coding.ScoringBreakdown;
import com.edutest.domain.assignment.coding.TestCase;
import com.edutest.domain.assignment.coding.TestCaseExecutionResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CodingAssignmentService {

    public ValidationResult validateConfiguration(CodingAssignment assignment) {
        if (!assignment.hasTestCases()) {
            return ValidationResult.invalid("Coding assignment must have at least one test case");
        }

        if (assignment.getAllowedLanguagesList().isEmpty()) {
            return ValidationResult.invalid("At least one programming language must be allowed");
        }

        if (assignment.getTimeLimitMs() != null && assignment.getTimeLimitMs() <= 0) {
            return ValidationResult.invalid("Time limit must be positive");
        }

        if (assignment.getMemoryLimitMb() != null && assignment.getMemoryLimitMb() <= 0) {
            return ValidationResult.invalid("Memory limit must be positive");
        }

        return ValidationResult.valid();
    }

    public Float calculateScoreFromTestResults(CodingAssignment assignment, List<TestCaseExecutionResult> results) {
        if (!assignment.hasTestCases() || results.isEmpty()) {
            return 0.0f;
        }

        int passedCount = (int) results.stream()
                .filter(TestCaseExecutionResult::isPassed)
                .count();

        float percentage = (float) passedCount / assignment.getTestCasesCount();
        return percentage * assignment.getPoints();
    }

    public ScoringBreakdown calculateDetailedScore(CodingAssignment assignment, List<TestCaseExecutionResult> results) {
        int totalTests = assignment.getTestCasesCount();
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

    public void addTestCase(CodingAssignment assignment, TestCase testCase) {
        if (testCase == null) {
            throw new IllegalArgumentException("Test case cannot be null");
        }

        List<TestCase> currentTestCases = assignment.getTestCases();
        List<TestCase> updatedTestCases = new java.util.ArrayList<>(currentTestCases);
        updatedTestCases.add(testCase);
        
        assignment.setTestCases(updatedTestCases);
        assignment.setUpdatedAt(LocalDateTime.now());
    }

    public void removeTestCase(CodingAssignment assignment, Long testCaseId) {
        List<TestCase> updatedTestCases = assignment.getTestCases().stream()
                .filter(tc -> !tc.getId().equals(testCaseId))
                .collect(Collectors.toList());
        
        assignment.setTestCases(updatedTestCases);
        assignment.setUpdatedAt(LocalDateTime.now());
    }

    public void updateTestCase(CodingAssignment assignment, Long testCaseId, TestCase newTestCase) {
        List<TestCase> updatedTestCases = assignment.getTestCases().stream()
                .map(tc -> tc.getId().equals(testCaseId) ? newTestCase : tc)
                .collect(Collectors.toList());
        
        assignment.setTestCases(updatedTestCases);
        assignment.setUpdatedAt(LocalDateTime.now());
    }

    public TestCase findTestCaseById(CodingAssignment assignment, Long testCaseId) {
        return assignment.getTestCases().stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElse(null);
    }

    public List<TestCase> getPublicTestCases(CodingAssignment assignment) {
        return assignment.getTestCases().stream()
                .filter(TestCase::getIsPublic)
                .collect(Collectors.toList());
    }

    public List<TestCase> getHiddenTestCases(CodingAssignment assignment) {
        return assignment.getTestCases().stream()
                .filter(tc -> !tc.getIsPublic())
                .collect(Collectors.toList());
    }

    public boolean isExecutionWithinLimits(CodingAssignment assignment, long executionTimeMs, int memoryUsedMb) {
        boolean timeOk = !assignment.hasTimeLimit() || executionTimeMs <= assignment.getTimeLimitMs();
        boolean memoryOk = !assignment.hasMemoryLimit() || memoryUsedMb <= assignment.getMemoryLimitMb();
        return timeOk && memoryOk;
    }

    public ExecutionValidationResult validateExecution(CodingAssignment assignment, String code, 
                                                       long executionTimeMs, int memoryUsedMb) {
        ValidationResult codeValidation = assignment.validateAnswer(code);
        if (codeValidation.hasError()) {
            return ExecutionValidationResult.invalid(codeValidation.getErrorMessage());
        }

        if (!isExecutionWithinLimits(assignment, executionTimeMs, memoryUsedMb)) {
            String message = buildLimitExceededMessage(assignment, executionTimeMs, memoryUsedMb);
            return ExecutionValidationResult.limitExceeded(message);
        }

        return ExecutionValidationResult.valid();
    }

    private String buildLimitExceededMessage(CodingAssignment assignment, long executionTimeMs, int memoryUsedMb) {
        StringBuilder message = new StringBuilder("Execution limits exceeded: ");
        
        if (assignment.hasTimeLimit() && executionTimeMs > assignment.getTimeLimitMs()) {
            message.append(String.format("Time limit (%dms) exceeded by %dms. ", 
                    assignment.getTimeLimitMs(), executionTimeMs - assignment.getTimeLimitMs()));
        }
        
        if (assignment.hasMemoryLimit() && memoryUsedMb > assignment.getMemoryLimitMb()) {
            message.append(String.format("Memory limit (%dMB) exceeded by %dMB.", 
                    assignment.getMemoryLimitMb(), memoryUsedMb - assignment.getMemoryLimitMb()));
        }
        
        return message.toString();
    }

    public static class ExecutionValidationResult {
        private boolean isValid;
        private boolean isLimitExceeded;
        private String message;

        public static ExecutionValidationResult valid() {
            ExecutionValidationResult result = new ExecutionValidationResult();
            result.isValid = true;
            result.isLimitExceeded = false;
            return result;
        }

        public static ExecutionValidationResult invalid(String message) {
            ExecutionValidationResult result = new ExecutionValidationResult();
            result.isValid = false;
            result.isLimitExceeded = false;
            result.message = message;
            return result;
        }

        public static ExecutionValidationResult limitExceeded(String message) {
            ExecutionValidationResult result = new ExecutionValidationResult();
            result.isValid = false;
            result.isLimitExceeded = true;
            result.message = message;
            return result;
        }

        public boolean isValid() { return isValid; }
        public boolean isLimitExceeded() { return isLimitExceeded; }
        public String getMessage() { return message; }
    }
}