package com.edutest.service.codiingassigment;

import com.edutest.domain.assignment.ValidationResult;
import com.edutest.domain.assignment.coding.CodingAssignment;
import org.springframework.stereotype.Service;

@Service
public class CodingAssignmentValidationService {

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
}
