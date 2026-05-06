package com.edutest.codeexecution;

import com.edutest.codeexecution.docker.DockerCodeExecutor;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.code.CompilationStatusEnum;
import com.edutest.persistance.entity.code.ExecutionStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DockerCodeExecutionServiceTest {

    @Mock
    private DockerCodeExecutor executor;

    @Mock
    private CodeSubmissionResultMapper mapper;

    @InjectMocks
    private DockerCodeExecutionService service;

    private CodingAssignmentEntity assignment;
    private CodeSubmissionEntity submission;
    private TestCaseEntity tc1;

    @BeforeEach
    void setUp() {
        tc1 = new TestCaseEntity();
        tc1.setId(1L);

        assignment = new CodingAssignmentEntity();
        assignment.setPoints(10);
        assignment.setTimeLimitMs(2_000);
        assignment.setMemoryLimitMb(128);
        assignment.setTestCases(new ArrayList<>(List.of(tc1)));

        submission = CodeSubmissionEntity.builder()
                .assignment(assignment)
                .sourceCode("print(1)")
                .programmingLanguage("python")
                .testCaseResults(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Forwards submission fields and assignment limits to the executor")
    void forwardsArgsToExecutor() {
        ExecutionReport report = stubReport();
        when(executor.execute(any(), any(), any(), any(), any())).thenReturn(report);

        service.executeAndPersist(submission);

        verify(executor).execute(
                eq("print(1)"),
                eq("python"),
                eq(assignment.getTestCases()),
                eq(2_000),
                eq(128));
    }

    @Test
    @DisplayName("Calls mapper with the submission, assignment and executor's report")
    void callsMapperWithReport() {
        ExecutionReport report = stubReport();
        when(executor.execute(any(), any(), any(), any(), any())).thenReturn(report);

        service.executeAndPersist(submission);

        ArgumentCaptor<ExecutionReport> reportCaptor = ArgumentCaptor.forClass(ExecutionReport.class);
        verify(mapper).apply(eq(submission), eq(assignment), reportCaptor.capture());
        assertThat(reportCaptor.getValue()).isSameAs(report);
    }

    @Test
    @DisplayName("Passes null limits through when the assignment doesn't define them")
    void passesNullLimits() {
        assignment.setTimeLimitMs(null);
        assignment.setMemoryLimitMb(null);
        when(executor.execute(any(), any(), any(), any(), any())).thenReturn(stubReport());

        service.executeAndPersist(submission);

        verify(executor).execute(
                eq("print(1)"),
                eq("python"),
                eq(assignment.getTestCases()),
                eq((Integer) null),
                eq((Integer) null));
    }

    private static ExecutionReport stubReport() {
        return ExecutionReport.builder()
                .compilationStatus(CompilationStatusEnum.SUCCESS)
                .executionStatus(ExecutionStatusEnum.SUCCESS)
                .testCaseResults(List.of())
                .maxExecutionTimeMs(0L)
                .maxMemoryUsedMb(0)
                .build();
    }
}
