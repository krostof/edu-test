package com.edutest.codeexecution;

import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.code.CompilationStatusEnum;
import com.edutest.persistance.entity.code.ExecutionStatusEnum;
import com.edutest.persistance.entity.test.TestCaseResultEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CodeSubmissionResultMapperTest {

    private CodeSubmissionResultMapper mapper;
    private CodingAssignmentEntity assignment;
    private TestCaseEntity tc1;
    private TestCaseEntity tc2;
    private TestCaseEntity tc3;
    private TestCaseEntity tc4;

    @BeforeEach
    void setUp() {
        mapper = new CodeSubmissionResultMapper();

        assignment = new CodingAssignmentEntity();
        assignment.setPoints(10);

        tc1 = newTestCase(1L);
        tc2 = newTestCase(2L);
        tc3 = newTestCase(3L);
        tc4 = newTestCase(4L);
        assignment.setTestCases(new ArrayList<>(List.of(tc1, tc2, tc3, tc4)));
    }

    @Test
    @DisplayName("All passing test cases yield full score")
    void fullScoreWhenAllPass() {
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = reportWith(
                List.of(
                        result(1L, true, "ok"),
                        result(2L, true, "ok"),
                        result(3L, true, "ok"),
                        result(4L, true, "ok")
                ),
                ExecutionStatusEnum.SUCCESS,
                CompilationStatusEnum.SUCCESS);

        mapper.apply(submission, assignment, report);

        assertThat(submission.getTotalScore()).isEqualTo(10.0f);
        assertThat(submission.getTestCaseResults()).hasSize(4);
    }

    @Test
    @DisplayName("Partial pass scales score linearly to assignment points")
    void partialScoreScalesLinearly() {
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = reportWith(
                List.of(
                        result(1L, true, "ok"),
                        result(2L, false, "wrong"),
                        result(3L, true, "ok"),
                        result(4L, false, "wrong")
                ),
                ExecutionStatusEnum.SUCCESS,
                CompilationStatusEnum.SUCCESS);

        mapper.apply(submission, assignment, report);

        assertThat(submission.getTotalScore()).isEqualTo(5.0f, within(0.001f));
    }

    @Test
    @DisplayName("Zero score when no test cases passed")
    void zeroScoreOnAllFailed() {
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = reportWith(
                List.of(result(1L, false, ""), result(2L, false, ""),
                        result(3L, false, ""), result(4L, false, "")),
                ExecutionStatusEnum.RUNTIME_ERROR,
                CompilationStatusEnum.SUCCESS);

        mapper.apply(submission, assignment, report);

        assertThat(submission.getTotalScore()).isEqualTo(0.0f);
        assertThat(submission.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Empty test case list yields zero score without division by zero")
    void emptyTestCasesYieldsZeroScore() {
        assignment.setTestCases(new ArrayList<>());
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = reportWith(List.of(),
                ExecutionStatusEnum.SUCCESS, CompilationStatusEnum.SUCCESS);

        mapper.apply(submission, assignment, report);

        assertThat(submission.getTotalScore()).isEqualTo(0.0f);
        assertThat(submission.getTestCaseResults()).isEmpty();
    }

    @Test
    @DisplayName("Score is zero when assignment points is null")
    void nullPointsYieldsZeroScore() {
        assignment.setPoints(null);
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = reportWith(
                List.of(result(1L, true, "ok")),
                ExecutionStatusEnum.SUCCESS, CompilationStatusEnum.SUCCESS);

        mapper.apply(submission, assignment, report);

        assertThat(submission.getTotalScore()).isEqualTo(0.0f);
    }

    @Test
    @DisplayName("Compilation failure propagates status and error to submission")
    void compilationFailurePropagates() {
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = ExecutionReport.compilationFailed("syntax error on line 5");

        mapper.apply(submission, assignment, report);

        assertThat(submission.getCompilationStatus()).isEqualTo(CompilationStatusEnum.ERROR);
        assertThat(submission.getCompilationError()).isEqualTo("syntax error on line 5");
        assertThat(submission.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.NOT_EXECUTED);
        assertThat(submission.getTotalScore()).isEqualTo(0.0f);
        assertThat(submission.getTestCaseResults()).isEmpty();
    }

    @Test
    @DisplayName("System error report yields SYSTEM_ERROR status and zero score")
    void systemErrorPropagates() {
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = ExecutionReport.systemError("Docker daemon unreachable");

        mapper.apply(submission, assignment, report);

        assertThat(submission.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.SYSTEM_ERROR);
        assertThat(submission.getTotalScore()).isEqualTo(0.0f);
    }

    @Test
    @DisplayName("Per-test-case fields are mapped onto TestCaseResultEntity")
    void testCaseFieldsAreMapped() {
        CodeSubmissionEntity submission = newSubmission();
        TestCaseRunResult run = TestCaseRunResult.builder()
                .testCaseId(1L)
                .passed(false)
                .actualOutput("4")
                .errorMessage("expected 5")
                .executionTimeMs(123L)
                .memoryUsedMb(42)
                .timedOut(false)
                .outOfMemory(false)
                .build();
        ExecutionReport report = reportWith(List.of(run),
                ExecutionStatusEnum.SUCCESS, CompilationStatusEnum.SUCCESS);

        mapper.apply(submission, assignment, report);

        TestCaseResultEntity entity = submission.getTestCaseResults().get(0);
        assertThat(entity.getTestCase()).isSameAs(tc1);
        assertThat(entity.getSubmission()).isSameAs(submission);
        assertThat(entity.getPassed()).isFalse();
        assertThat(entity.getActualOutput()).isEqualTo("4");
        assertThat(entity.getErrorMessage()).isEqualTo("expected 5");
        assertThat(entity.getExecutionTimeMs()).isEqualTo(123L);
        assertThat(entity.getMemoryUsedMb()).isEqualTo(42);
    }

    @Test
    @DisplayName("Stale test case results are cleared before applying new ones")
    void clearsStaleResults() {
        CodeSubmissionEntity submission = newSubmission();
        TestCaseResultEntity stale = TestCaseResultEntity.builder()
                .testCase(tc1)
                .submission(submission)
                .passed(true)
                .actualOutput("stale")
                .build();
        submission.getTestCaseResults().add(stale);

        ExecutionReport report = reportWith(
                List.of(result(1L, false, "fresh")),
                ExecutionStatusEnum.RUNTIME_ERROR,
                CompilationStatusEnum.SUCCESS);

        mapper.apply(submission, assignment, report);

        assertThat(submission.getTestCaseResults()).hasSize(1);
        assertThat(submission.getTestCaseResults().get(0).getActualOutput()).isEqualTo("fresh");
    }

    @Test
    @DisplayName("Run result for unknown testCaseId is silently dropped")
    void unknownTestCaseIdIsDropped() {
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = reportWith(
                List.of(result(1L, true, "ok"), result(999L, true, "ghost")),
                ExecutionStatusEnum.SUCCESS, CompilationStatusEnum.SUCCESS);

        mapper.apply(submission, assignment, report);

        assertThat(submission.getTestCaseResults()).hasSize(1);
        assertThat(submission.getTestCaseResults().get(0).getTestCase().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Max execution time and memory propagate to submission")
    void maxLimitsPropagate() {
        CodeSubmissionEntity submission = newSubmission();
        ExecutionReport report = ExecutionReport.builder()
                .compilationStatus(CompilationStatusEnum.SUCCESS)
                .executionStatus(ExecutionStatusEnum.SUCCESS)
                .testCaseResults(List.of())
                .maxExecutionTimeMs(1234L)
                .maxMemoryUsedMb(99)
                .build();

        mapper.apply(submission, assignment, report);

        assertThat(submission.getMaxExecutionTimeMs()).isEqualTo(1234L);
        assertThat(submission.getMaxMemoryUsedMb()).isEqualTo(99);
    }

    private static TestCaseEntity newTestCase(Long id) {
        TestCaseEntity tc = new TestCaseEntity();
        tc.setId(id);
        return tc;
    }

    private static CodeSubmissionEntity newSubmission() {
        return CodeSubmissionEntity.builder()
                .testCaseResults(new ArrayList<>())
                .build();
    }

    private static TestCaseRunResult result(Long testCaseId, boolean passed, String actualOutput) {
        return TestCaseRunResult.builder()
                .testCaseId(testCaseId)
                .passed(passed)
                .actualOutput(actualOutput)
                .executionTimeMs(0L)
                .memoryUsedMb(0)
                .build();
    }

    private static ExecutionReport reportWith(List<TestCaseRunResult> results,
                                              ExecutionStatusEnum execStatus,
                                              CompilationStatusEnum compStatus) {
        return ExecutionReport.builder()
                .compilationStatus(compStatus)
                .executionStatus(execStatus)
                .testCaseResults(results)
                .maxExecutionTimeMs(0L)
                .maxMemoryUsedMb(0)
                .build();
    }
}
