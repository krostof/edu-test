package com.edutest.service.codiingassigment;

import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.assignment.coding.TestCase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodingAssignmentTestCaseService {

    public CodingAssignment addTestCase(CodingAssignment assignment, TestCase testCase) {
        if (testCase == null) {
            throw new IllegalArgumentException("Test case cannot be null");
        }

        List<TestCase> newTestCases = new ArrayList<>(assignment.getTestCases());
        newTestCases.add(testCase);

        return assignment.toBuilder()
                .testCases(newTestCases)
                .build();
    }

    public CodingAssignment removeTestCase(CodingAssignment assignment, Long testCaseId) {
        List<TestCase> newTestCases = assignment.getTestCases().stream()
                .filter(tc -> !tc.getId().equals(testCaseId))
                .collect(Collectors.toList());

        return assignment.toBuilder()
                .testCases(newTestCases)
                .build();
    }

    public CodingAssignment updateTestCase(CodingAssignment assignment, Long testCaseId, TestCase newTestCase) {
        List<TestCase> newTestCases = assignment.getTestCases().stream()
                .map(tc -> tc.getId().equals(testCaseId) ? newTestCase : tc)
                .collect(Collectors.toList());

        return assignment.toBuilder()
                .testCases(newTestCases)
                .build();
    }

    public CodingAssignment setTestCases(CodingAssignment assignment, List<TestCase> testCases) {
        List<TestCase> newTestCases = testCases != null ? List.copyOf(testCases) : List.of();

        return assignment.toBuilder()
                .testCases(newTestCases)
                .build();
    }
}
