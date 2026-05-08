package com.edutest.service.codeexecution;

import com.edutest.dto.AnswerDto;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.repository.CodeSubmissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncCodeRunServiceTest {

    @Mock
    private CodeExecutionService codeExecutionService;

    @Mock
    private CodeSubmissionJpaRepository submissionRepository;

    @InjectMocks
    private AsyncCodeRunService asyncCodeRunService;

    private CodeRunJobRegistry registry;

    @BeforeEach
    void setUp() {
        // Use real registry — its behavior is exercised through this service
        registry = new CodeRunJobRegistry();
        asyncCodeRunService = new AsyncCodeRunService(codeExecutionService, submissionRepository, registry);
    }

    @Test
    @DisplayName("Happy path: marks DONE with executor's preview result")
    void happyPath() {
        CodingAssignmentEntity assignment = new CodingAssignmentEntity();
        assignment.setTestCases(new ArrayList<>(List.of(new TestCaseEntity())));

        CodeSubmissionEntity submission = CodeSubmissionEntity.builder()
                .sourceCode("print('hi')")
                .programmingLanguage("python")
                .assignment(assignment)
                .build();
        // Simulate JPA-set ID
        setId(submission, 42L);

        AnswerDto preview = AnswerDto.builder()
                .id(42L)
                .testCasesPassed(2)
                .testCasesTotal(3)
                .build();

        when(submissionRepository.findById(42L)).thenReturn(Optional.of(submission));
        when(codeExecutionService.runPreview(submission)).thenReturn(preview);

        registry.markPending(42L);
        asyncCodeRunService.executeAsync(42L);

        assertThat(registry.getStatus(42L).getStatus()).isEqualTo("DONE");
        assertThat(registry.getStatus(42L).getResult()).isSameAs(preview);
    }

    @Test
    @DisplayName("Submission not found → marks FAILED with descriptive message")
    void submissionNotFound() {
        when(submissionRepository.findById(99L)).thenReturn(Optional.empty());

        registry.markPending(99L);
        asyncCodeRunService.executeAsync(99L);

        assertThat(registry.getStatus(99L).getStatus()).isEqualTo("FAILED");
        assertThat(registry.getStatus(99L).getError()).contains("Submission not found");
        verify(codeExecutionService, never()).runPreview(any());
    }

    @Test
    @DisplayName("Executor exception → marks FAILED with exception message")
    void executorThrows() {
        CodingAssignmentEntity assignment = new CodingAssignmentEntity();
        assignment.setTestCases(new ArrayList<>());
        CodeSubmissionEntity submission = CodeSubmissionEntity.builder()
                .assignment(assignment)
                .build();
        setId(submission, 7L);

        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission));
        when(codeExecutionService.runPreview(submission))
                .thenThrow(new RuntimeException("Image pull failed"));

        registry.markPending(7L);
        asyncCodeRunService.executeAsync(7L);

        assertThat(registry.getStatus(7L).getStatus()).isEqualTo("FAILED");
        assertThat(registry.getStatus(7L).getError()).isEqualTo("Image pull failed");
    }

    @Test
    @DisplayName("Exception with null message → uses fallback string")
    void executorThrowsNullMessage() {
        CodingAssignmentEntity assignment = new CodingAssignmentEntity();
        assignment.setTestCases(new ArrayList<>());
        CodeSubmissionEntity submission = CodeSubmissionEntity.builder().assignment(assignment).build();
        setId(submission, 8L);

        when(submissionRepository.findById(8L)).thenReturn(Optional.of(submission));
        when(codeExecutionService.runPreview(submission))
                .thenThrow(new RuntimeException((String) null));

        registry.markPending(8L);
        asyncCodeRunService.executeAsync(8L);

        assertThat(registry.getStatus(8L).getStatus()).isEqualTo("FAILED");
        assertThat(registry.getStatus(8L).getError()).isEqualTo("Unknown execution error");
    }

    /** Sets the inherited BaseEntity id via reflection (constructor doesn't allow it). */
    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
