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
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CodingAssignment extends Assignment {

    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private String allowedLanguages;
    private String starterCode;
    private String solutionTemplate;

    @Builder.Default
    private List<TestCase> testCases = List.of();

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
        ValidationResult validation = validateAnswer(answer);
        if (validation.hasError()) {
            return 0.0f;
        }
        return 0.0f;
    }

    @Override
    public boolean supportsAttachments() {
        return false;
    }

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

    public boolean hasTestCases() {
        return testCases != null && !testCases.isEmpty();
    }

    public int getTestCasesCount() {
        return testCases.size();
    }

    public List<String> getAllowedLanguagesList() {
        if (allowedLanguages == null || allowedLanguages.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(allowedLanguages.split(","));
    }

    public boolean isLanguageAllowed(String language) {
        return getAllowedLanguagesList().contains(language);
    }

    public List<TestCase> getPublicTestCases() {
        return testCases.stream()
                .filter(TestCase::getIsPublic)
                .collect(Collectors.toList());
    }

    public List<TestCase> getHiddenTestCases() {
        return testCases.stream()
                .filter(tc -> !tc.getIsPublic())
                .collect(Collectors.toList());
    }

    public TestCase findTestCaseById(Long testCaseId) {
        return testCases.stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElse(null);
    }

    public boolean isExecutionWithinLimits(long executionTimeMs, int memoryUsedMb) {
        boolean timeOk = !hasTimeLimit() || executionTimeMs <= timeLimitMs;
        boolean memoryOk = !hasMemoryLimit() || memoryUsedMb <= memoryLimitMb;
        return timeOk && memoryOk;
    }
}