package com.edutest.domain.assignment.coding;


import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.assignment.AssignmentType;
import com.edutest.domain.assignment.ValidationResult;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CodingAssignment extends Assignment {

    private Integer timeLimitMs;        // Limit czasowy wykonania w milisekundach
    private Integer memoryLimitMb;      // Limit pamięci w MB
    private String allowedLanguages;    // Języki jako string CSV
    private String starterCode;         // Kod startowy dla studenta
    private String solutionTemplate;    // Template rozwiązania
    private List<TestCase> testCases;   // Przypadki testowe

    @Builder(builderMethodName = "codingBuilder")
    public CodingAssignment(Long id, Long testId, String title, String description,
                            Integer orderNumber, Integer points, LocalDateTime createdAt,
                            LocalDateTime updatedAt, Integer timeLimitMs, Integer memoryLimitMb,
                            String allowedLanguages, String starterCode, String solutionTemplate,
                            List<TestCase> testCases) {
        super(id, testId, title, description, orderNumber, points, createdAt, updatedAt);
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitMb = memoryLimitMb;
        this.allowedLanguages = allowedLanguages;
        this.starterCode = starterCode;
        this.solutionTemplate = solutionTemplate;
        this.testCases = testCases != null ? List.copyOf(testCases) : List.of();
    }

    @Override
    public AssignmentType getType() {
        return AssignmentType.CODING;
    }

    @Override
    public ValidationResult validateAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return ValidationResult.invalid("Code cannot be empty");
        }

        if (answer.length() > 50000) {
            return ValidationResult.invalid("Code is too long (max 50,000 characters)");
        }

        return ValidationResult.valid();
    }

    @Override
    public Float calculateScore(String answer) {
        // W przypadku zadań programistycznych, punkty są obliczane
        // na podstawie przejścia testów - implementacja w domain service
        ValidationResult validation = validateAnswer(answer);
        if (validation.hasError()) {
            return 0.0f;
        }

        // Rzeczywiste obliczenie punktów na podstawie testów
        // będzie w CodeExecutionService
        return 0.0f;
    }

    @Override
    public boolean supportsAttachments() {
        return false; // Coding assignments nie potrzebują załączników
    }

    // Business methods for allowed languages
    public List<String> getAllowedLanguagesList() {
        if (allowedLanguages == null || allowedLanguages.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(allowedLanguages.split(","));
    }

    public void setAllowedLanguagesList(List<String> languages) {
        if (languages == null || languages.isEmpty()) {
            this.allowedLanguages = null;
        } else {
            this.allowedLanguages = String.join(",", languages);
        }
        setUpdatedAt(LocalDateTime.now());
    }

    public boolean isLanguageAllowed(String language) {
        return getAllowedLanguagesList().contains(language);
    }

    public void addAllowedLanguage(String language) {
        List<String> current = new java.util.ArrayList<>(getAllowedLanguagesList());
        if (!current.contains(language)) {
            current.add(language);
            setAllowedLanguagesList(current);
        }
    }

    public void removeAllowedLanguage(String language) {
        List<String> current = new java.util.ArrayList<>(getAllowedLanguagesList());
        current.remove(language);
        setAllowedLanguagesList(current);
    }

    // Business methods for limits
    public boolean hasTimeLimit() {
        return timeLimitMs != null && timeLimitMs > 0;
    }

    public boolean hasMemoryLimit() {
        return memoryLimitMb != null && memoryLimitMb > 0;
    }

    public boolean hasStarterCode() {
        return starterCode != null && !starterCode.trim().isEmpty();
    }

    public boolean hasSolutionTemplate() {
        return solutionTemplate != null && !solutionTemplate.trim().isEmpty();
    }

    // Test cases management
    public boolean hasTestCases() {
        return testCases != null && !testCases.isEmpty();
    }

    public int getTestCasesCount() {
        return testCases != null ? testCases.size() : 0;
    }

    public List<TestCase> getPublicTestCases() {
        return testCases.stream()
                .filter(TestCase::isPublic)
                .collect(Collectors.toList());
    }

    public List<TestCase> getHiddenTestCases() {
        return testCases.stream()
                .filter(tc -> !tc.isPublic())
                .collect(Collectors.toList());
    }

    public void addTestCase(TestCase testCase) {
        if (testCase == null) {
            throw new IllegalArgumentException("Test case cannot be null");
        }

        this.testCases = new java.util.ArrayList<>(testCases);
        this.testCases.add(testCase);
        setUpdatedAt(LocalDateTime.now());
    }

    public void removeTestCase(Long testCaseId) {
        this.testCases = testCases.stream()
                .filter(tc -> !tc.getId().equals(testCaseId))
                .collect(Collectors.toList());
        setUpdatedAt(LocalDateTime.now());
    }

    public void updateTestCase(Long testCaseId, TestCase newTestCase) {
        this.testCases = testCases.stream()
                .map(tc -> tc.getId().equals(testCaseId) ? newTestCase : tc)
                .collect(Collectors.toList());
        setUpdatedAt(LocalDateTime.now());
    }

    public TestCase findTestCaseById(Long testCaseId) {
        return testCases.stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElse(null);
    }

    public void setTestCases(List<TestCase> newTestCases) {
        this.testCases = newTestCases != null ? List.copyOf(newTestCases) : List.of();
        setUpdatedAt(LocalDateTime.now());
    }

    // Execution validation
    public boolean isExecutionWithinLimits(long executionTimeMs, int memoryUsedMb) {
        boolean timeOk = !hasTimeLimit() || executionTimeMs <= timeLimitMs;
        boolean memoryOk = !hasMemoryLimit() || memoryUsedMb <= memoryLimitMb;
        return timeOk && memoryOk;
    }

    public ExecutionLimitCheck checkExecutionLimits(long executionTimeMs, int memoryUsedMb) {
        boolean timeExceeded = hasTimeLimit() && executionTimeMs > timeLimitMs;
        boolean memoryExceeded = hasMemoryLimit() && memoryUsedMb > memoryLimitMb;

        return ExecutionLimitCheck.builder()
                .timeExceeded(timeExceeded)
                .memoryExceeded(memoryExceeded)
                .withinLimits(!timeExceeded && !memoryExceeded)
                .executionTime(executionTimeMs)
                .memoryUsed(memoryUsedMb)
                .timeLimit(timeLimitMs)
                .memoryLimit(memoryLimitMb)
                .build();
    }

    // Scoring with test results
    public Float calculateScoreFromTestResults(List<TestCaseExecutionResult> results) {
        if (testCases.isEmpty() || results.isEmpty()) {
            return 0.0f;
        }

        int passedCount = (int) results.stream()
                .filter(TestCaseExecutionResult::isPassed)
                .count();

        float percentage = (float) passedCount / testCases.size();
        return percentage * getPoints();
    }

    public ScoringBreakdown calculateDetailedScore(List<TestCaseExecutionResult> results) {
        int totalTests = testCases.size();
        int passedTests = (int) results.stream().filter(TestCaseExecutionResult::isPassed).count();
        int failedTests = totalTests - passedTests;

        float score = calculateScoreFromTestResults(results);
        float percentage = totalTests > 0 ? (float) passedTests / totalTests * 100 : 0;

        return ScoringBreakdown.builder()
                .totalTestCases(totalTests)
                .passedTestCases(passedTests)
                .failedTestCases(failedTests)
                .score(score)
                .maxScore(getPoints().floatValue())
                .percentage(percentage)
                .isPerfect(passedTests == totalTests)
                .build();
    }

    // Configuration validation
    public ValidationResult validateConfiguration() {
        if (!hasTestCases()) {
            return ValidationResult.invalid("Coding assignment must have at least one test case");
        }

        if (getAllowedLanguagesList().isEmpty()) {
            return ValidationResult.invalid("At least one programming language must be allowed");
        }

        if (timeLimitMs != null && timeLimitMs <= 0) {
            return ValidationResult.invalid("Time limit must be positive");
        }

        if (memoryLimitMb != null && memoryLimitMb <= 0) {
            return ValidationResult.invalid("Memory limit must be positive");
        }

        return ValidationResult.valid();
    }

    // Difficulty assessment
    public DifficultyLevel assessDifficulty() {
        int factors = 0;

        if (hasTimeLimit() && timeLimitMs < 5000) factors++; // < 5 seconds
        if (hasMemoryLimit() && memoryLimitMb < 64) factors++; // < 64MB
        if (getTestCasesCount() > 10) factors++; // Many test cases
        if (getHiddenTestCases().size() > getPublicTestCases().size()) factors++; // More hidden than public

        return switch (factors) {
            case 0, 1 -> DifficultyLevel.EASY;
            case 2 -> DifficultyLevel.MEDIUM;
            default -> DifficultyLevel.HARD;
        };
    }
}
