package com.edutest.codeexecution;

import com.edutest.codeexecution.docker.DockerCodeExecutor;
import com.edutest.dto.AnswerDto;
import com.edutest.dto.TestCaseResultDto;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.service.codeexecution.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerCodeExecutionService implements CodeExecutionService {

    private final DockerCodeExecutor executor;
    private final CodeSubmissionResultMapper mapper;

    @Override
    public void executeAndPersist(CodeSubmissionEntity submission) {
        CodingAssignmentEntity assignment = submission.getAssignment();

        ExecutionReport report = executor.execute(
                submission.getSourceCode(),
                submission.getProgrammingLanguage(),
                assignment.getTestCases(),
                assignment.getTimeLimitMs(),
                assignment.getMemoryLimitMb()
        );

        mapper.apply(submission, assignment, report);
        log.debug("Execution finished for submission {}: {} / {} test cases passed",
                submission.getId(),
                report.getTestCaseResults().stream().filter(TestCaseRunResult::isPassed).count(),
                report.getTestCaseResults().size());
    }

    @Override
    public AnswerDto runPreview(CodeSubmissionEntity submission) {
        CodingAssignmentEntity assignment = submission.getAssignment();

        List<TestCaseEntity> publicTestCases = assignment.getTestCases().stream()
                .filter(tc -> Boolean.TRUE.equals(tc.getIsPublic()))
                .collect(Collectors.toList());

        if (publicTestCases.isEmpty()) {
            log.warn("Preview requested for submission {} but no public test cases configured", submission.getId());
        }

        ExecutionReport report = executor.execute(
                submission.getSourceCode(),
                submission.getProgrammingLanguage(),
                publicTestCases,
                assignment.getTimeLimitMs(),
                assignment.getMemoryLimitMb()
        );

        return buildPreviewDto(submission, publicTestCases, report);
    }

    private AnswerDto buildPreviewDto(CodeSubmissionEntity submission,
                                       List<TestCaseEntity> publicTestCases,
                                       ExecutionReport report) {
        Map<Long, TestCaseEntity> byId = new HashMap<>();
        for (TestCaseEntity tc : publicTestCases) {
            byId.put(tc.getId(), tc);
        }

        List<TestCaseResultDto> resultDtos = report.getTestCaseResults().stream()
                .map(run -> {
                    TestCaseEntity tc = byId.get(run.getTestCaseId());
                    return TestCaseResultDto.builder()
                            .testCaseId(run.getTestCaseId())
                            .isPublic(true)
                            .description(tc != null ? tc.getDescription() : null)
                            .inputData(tc != null ? tc.getInputData() : null)
                            .expectedOutput(tc != null ? tc.getExpectedOutput() : null)
                            .actualOutput(run.getActualOutput())
                            .passed(run.isPassed())
                            .executionTimeMs(run.getExecutionTimeMs())
                            .memoryUsedMb(run.getMemoryUsedMb())
                            .errorMessage(run.getErrorMessage())
                            .build();
                })
                .collect(Collectors.toList());

        int passed = (int) report.getTestCaseResults().stream()
                .filter(TestCaseRunResult::isPassed)
                .count();

        return AnswerDto.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentType(AssignmentType.CODING.name())
                .answeredAt(submission.getSubmittedAt())
                .sourceCode(submission.getSourceCode())
                .programmingLanguage(submission.getProgrammingLanguage())
                .compilationStatus(report.getCompilationStatus() != null
                        ? report.getCompilationStatus().name() : null)
                .compilationError(report.getCompilationError())
                .executionStatus(report.getExecutionStatus() != null
                        ? report.getExecutionStatus().name() : null)
                .testCaseResults(resultDtos)
                .testCasesPassed(passed)
                .testCasesTotal(resultDtos.size())
                .isGraded(false)
                .build();
    }
}
