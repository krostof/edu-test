package com.edutest.codeexecution;

import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestCaseResultEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CodeSubmissionResultMapper {

    public void apply(CodeSubmissionEntity submission,
                      CodingAssignmentEntity assignment,
                      ExecutionReport report) {

        submission.setCompilationStatus(report.getCompilationStatus());
        submission.setCompilationError(report.getCompilationError());
        submission.setExecutionStatus(report.getExecutionStatus());
        submission.setMaxExecutionTimeMs(report.getMaxExecutionTimeMs());
        submission.setMaxMemoryUsedMb(report.getMaxMemoryUsedMb());

        Map<Long, TestCaseEntity> testCasesById = new HashMap<>();
        for (TestCaseEntity tc : assignment.getTestCases()) {
            testCasesById.put(tc.getId(), tc);
        }

        List<TestCaseResultEntity> resultEntities = new ArrayList<>();
        int passed = 0;
        int total = assignment.getTestCases().size();

        for (TestCaseRunResult run : report.getTestCaseResults()) {
            TestCaseEntity tc = testCasesById.get(run.getTestCaseId());
            if (tc == null) continue;

            TestCaseResultEntity entity = TestCaseResultEntity.builder()
                    .testCase(tc)
                    .submission(submission)
                    .actualOutput(run.getActualOutput())
                    .passed(run.isPassed())
                    .executionTimeMs(run.getExecutionTimeMs())
                    .memoryUsedMb(run.getMemoryUsedMb())
                    .errorMessage(run.getErrorMessage())
                    .build();
            resultEntities.add(entity);
            if (run.isPassed()) passed++;
        }

        submission.getTestCaseResults().clear();
        submission.getTestCaseResults().addAll(resultEntities);

        submission.setTotalScore(calculateScore(assignment, passed, total));
    }

    private static Float calculateScore(CodingAssignmentEntity assignment, int passed, int total) {
        if (total == 0 || assignment.getPoints() == null) return 0.0f;
        return ((float) passed / total) * assignment.getPoints();
    }
}
