package com.edutest.codeexecution;

import com.edutest.codeexecution.docker.DockerCodeExecutor;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.service.codeexecution.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
