package com.edutest.service.answer;

import com.edutest.dto.AnswerDto;
import com.edutest.dto.RunStatusDto;
import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAssignmentEntityEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.AssignmentAnswerJpaRepository;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import com.edutest.persistance.repository.ChoiceOptionJpaRepository;
import com.edutest.persistance.repository.CodeSubmissionJpaRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.service.attempt.AttemptRandomizationService;
import com.edutest.service.codeexecution.AsyncCodeRunService;
import com.edutest.service.codeexecution.CodeExecutionService;
import com.edutest.service.codeexecution.CodeRunJobRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused tests for the async preview flow added to {@link AnswerSubmissionService}.
 * Pre-existing single/multiple/open submit paths are covered in {@link AnswerSubmissionServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnswerSubmissionServiceRunCodeTest {

    @Mock private TestAttemptJpaRepository testAttemptRepository;
    @Mock private AssignmentJpaRepository assignmentRepository;
    @Mock private AssignmentAnswerJpaRepository answerRepository;
    @Mock private CodeSubmissionJpaRepository codeSubmissionRepository;
    @Mock private ChoiceOptionJpaRepository choiceOptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AttemptRandomizationService randomizationService;
    @Mock private CodeExecutionService codeExecutionService;
    @Mock private AsyncCodeRunService asyncCodeRunService;

    /** Use a real registry so the kicked-off job's PENDING state is observable. */
    private CodeRunJobRegistry registry = new CodeRunJobRegistry();

    @InjectMocks
    private AnswerSubmissionService service;

    private TestEntity testEntity;
    private TestAttemptEntity attempt;
    private UserEntity student;
    private CodingAssignmentEntity codingAssignment;
    private CodeSubmissionEntity submission;

    @BeforeEach
    void setUp() {
        registry = new CodeRunJobRegistry();
        // Re-instantiate with the real registry (Mockito's @InjectMocks fills the rest with mocks)
        service = new AnswerSubmissionService(
                testAttemptRepository, assignmentRepository, answerRepository, codeSubmissionRepository,
                choiceOptionRepository, userRepository, randomizationService,
                codeExecutionService, asyncCodeRunService, registry);

        testEntity = new TestEntity();
        testEntity.setId(1L);
        testEntity.setAllowNavigation(true);

        student = new UserEntity();
        student.setId(100L);

        attempt = new TestAttemptEntity();
        attempt.setId(10L);
        attempt.setTestEntity(testEntity);
        attempt.setStudent(student);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setIsCompleted(false);

        codingAssignment = new CodingAssignmentEntity();
        codingAssignment.setId(5L);
        codingAssignment.setTestEntity(testEntity);
        codingAssignment.setPoints(10);

        submission = CodeSubmissionEntity.builder()
                .assignment(codingAssignment)
                .testAttempt(attempt)
                .student(student)
                .sourceCode("print(1)")
                .programmingLanguage("python")
                .build();
        // Set ID via reflection
        try {
            var f = submission.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(submission, 42L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("runCode")
    class RunCodeTests {

        @Test
        @DisplayName("Marks PENDING in registry, dispatches to async worker, returns PENDING status")
        void happyPath() {
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(codingAssignment));
            when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                    .thenReturn(Optional.of(submission));

            RunStatusDto result = service.runCode(1L, 10L, 5L, 100L);

            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getStartedAt()).isNotNull();
            // Worker dispatched
            verify(asyncCodeRunService).executeAsync(42L);
            // Registry has PENDING for this submission
            assertThat(registry.getStatus(42L).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Throws when attempt not found")
        void attemptNotFound() {
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.runCode(1L, 10L, 5L, 100L))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(asyncCodeRunService, never()).executeAsync(any());
        }

        @Test
        @DisplayName("Throws AccessDenied when student doesn't own attempt")
        void notOwner() {
            UserEntity other = new UserEntity();
            other.setId(999L);
            attempt.setStudent(other);
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));

            assertThatThrownBy(() -> service.runCode(1L, 10L, 5L, 100L))
                    .isInstanceOf(AccessDeniedException.class);
            verify(asyncCodeRunService, never()).executeAsync(any());
        }

        @Test
        @DisplayName("Throws when attempt is finished")
        void finishedAttempt() {
            attempt.setIsCompleted(true);
            attempt.setFinishedAt(LocalDateTime.now());
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));

            assertThatThrownBy(() -> service.runCode(1L, 10L, 5L, 100L))
                    .isInstanceOf(IllegalStateException.class);
            verify(asyncCodeRunService, never()).executeAsync(any());
        }

        @Test
        @DisplayName("Throws when assignment is not CODING type")
        void nonCodingAssignment() {
            OpenQuestionAssignmentEntityEntity openAssignment = new OpenQuestionAssignmentEntityEntity();
            openAssignment.setId(5L);
            openAssignment.setTestEntity(testEntity);
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.<AssignmentEntity>of(openAssignment));

            assertThatThrownBy(() -> service.runCode(1L, 10L, 5L, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CODING");
            verify(asyncCodeRunService, never()).executeAsync(any());
        }

        @Test
        @DisplayName("Throws when no submission saved yet (student must save code first)")
        void noSubmission() {
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(codingAssignment));
            when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.runCode(1L, 10L, 5L, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Save your code first");
            verify(asyncCodeRunService, never()).executeAsync(any());
        }
    }

    @Nested
    @DisplayName("getRunStatus")
    class GetRunStatusTests {

        @Test
        @DisplayName("Returns PENDING from registry while job is running")
        void returnsPending() {
            registry.markPending(42L);
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(codingAssignment));
            when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                    .thenReturn(Optional.of(submission));

            RunStatusDto status = service.getRunStatus(1L, 10L, 5L, 100L);

            assertThat(status.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Returns DONE with result after worker completes")
        void returnsDone() {
            AnswerDto preview = AnswerDto.builder()
                    .id(42L)
                    .testCasesPassed(3)
                    .testCasesTotal(5)
                    .build();
            registry.markPending(42L);
            registry.markDone(42L, preview);

            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(codingAssignment));
            when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                    .thenReturn(Optional.of(submission));

            RunStatusDto status = service.getRunStatus(1L, 10L, 5L, 100L);

            assertThat(status.getStatus()).isEqualTo("DONE");
            assertThat(status.getResult()).isSameAs(preview);
        }

        @Test
        @DisplayName("Returns NONE when no job ever ran for this submission")
        void returnsNone() {
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(codingAssignment));
            when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                    .thenReturn(Optional.of(submission));

            RunStatusDto status = service.getRunStatus(1L, 10L, 5L, 100L);

            assertThat(status.getStatus()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("Throws AccessDenied when student doesn't own attempt")
        void accessDenied() {
            UserEntity other = new UserEntity();
            other.setId(999L);
            attempt.setStudent(other);
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));

            assertThatThrownBy(() -> service.getRunStatus(1L, 10L, 5L, 100L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Throws when assignment is not CODING")
        void nonCoding() {
            OpenQuestionAssignmentEntityEntity openAssignment = new OpenQuestionAssignmentEntityEntity();
            openAssignment.setId(5L);
            openAssignment.setTestEntity(testEntity);
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.<AssignmentEntity>of(openAssignment));

            assertThatThrownBy(() -> service.getRunStatus(1L, 10L, 5L, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CODING");
        }
    }
}
