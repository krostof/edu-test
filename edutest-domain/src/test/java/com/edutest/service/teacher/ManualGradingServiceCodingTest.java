package com.edutest.service.teacher;

import com.edutest.dto.AnswerReviewDto;
import com.edutest.dto.GradeAnswerRequestDto;
import com.edutest.event.TestAttemptGradedEvent;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.AssignmentAnswerJpaRepository;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import com.edutest.persistance.repository.ChoiceOptionJpaRepository;
import com.edutest.persistance.repository.CodeSubmissionJpaRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the CODING-specific path of {@link ManualGradingService#gradeAnswer}.
 * The non-CODING path is exercised by integration tests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ManualGradingServiceCodingTest {

    @Mock private TestAttemptJpaRepository testAttemptRepository;
    @Mock private AssignmentJpaRepository assignmentRepository;
    @Mock private AssignmentAnswerJpaRepository answerRepository;
    @Mock private CodeSubmissionJpaRepository codeSubmissionRepository;
    @Mock private ChoiceOptionJpaRepository choiceOptionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ManualGradingService service;

    private TestEntity testEntity;
    private TestAttemptEntity attempt;
    private CodingAssignmentEntity assignment;
    private CodeSubmissionEntity submission;
    private UserEntity student;

    @BeforeEach
    void setUp() {
        testEntity = new TestEntity();
        testEntity.setId(1L);

        student = new UserEntity();
        student.setId(100L);
        student.setFirstName("Bob");
        student.setLastName("Builder");

        attempt = new TestAttemptEntity();
        attempt.setId(10L);
        attempt.setTestEntity(testEntity);
        attempt.setStudent(student);

        assignment = new CodingAssignmentEntity();
        assignment.setId(5L);
        assignment.setTestEntity(testEntity);
        assignment.setTitle("Reverse a string");
        assignment.setPoints(10);

        submission = CodeSubmissionEntity.builder()
                .assignment(assignment)
                .testAttempt(attempt)
                .student(student)
                .sourceCode("def f(s): return s[::-1]")
                .programmingLanguage("python")
                .totalScore(6.0f)
                .testCaseResults(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Happy path: persists score + feedback on CodeSubmissionEntity, recalculates attempt score")
    void happyPath() {
        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                .thenReturn(Optional.of(submission));
        when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(0f);
        when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(8.5f);

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder()
                .score(8.5f)
                .feedback("Solid solution, but watch edge cases for empty input.")
                .build();

        AnswerReviewDto result = service.gradeAnswer(1L, 10L, 5L, request);

        // Submission updated
        assertThat(submission.getTotalScore()).isEqualTo(8.5f);
        assertThat(submission.getTeacherFeedback())
                .isEqualTo("Solid solution, but watch edge cases for empty input.");
        verify(codeSubmissionRepository).save(submission);

        // Attempt score recalculated
        assertThat(attempt.getScore()).isEqualTo(8.5f);
        verify(testAttemptRepository).save(attempt);

        // Review DTO reflects the new state
        assertThat(result.getAssignmentType()).isEqualTo(AssignmentType.CODING.name());
        assertThat(result.getScore()).isEqualTo(8.5f);
        assertThat(result.getTeacherFeedback())
                .isEqualTo("Solid solution, but watch edge cases for empty input.");
        assertThat(result.getStudentName()).isEqualTo("Bob Builder");
    }

    @Test
    @DisplayName("Publishes TestAttemptGradedEvent when grading the LAST pending answer of a finished attempt")
    void publishesEventWhenAttemptBecomesFullyGraded() {
        attempt.setIsCompleted(true);
        attempt.setScore(7.5f);

        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                .thenReturn(Optional.of(submission));
        when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(0f);
        when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(7.5f);

        // Simulate "everything graded after this call":
        when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(0L);
        // submission has totalScore != null after grading (set by service)
        when(codeSubmissionRepository.findByTestAttemptId(10L)).thenReturn(java.util.List.of(submission));

        when(assignmentRepository.sumPointsByTestId(1L)).thenReturn(10f);

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder()
                .score(7.5f)
                .feedback("good")
                .build();

        service.gradeAnswer(1L, 10L, 5L, request);

        ArgumentCaptor<TestAttemptGradedEvent> captor = ArgumentCaptor.forClass(TestAttemptGradedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        TestAttemptGradedEvent event = captor.getValue();
        assertThat(event.attemptId()).isEqualTo(10L);
        assertThat(event.testId()).isEqualTo(1L);
        assertThat(event.studentId()).isEqualTo(100L);
        assertThat(event.totalScore()).isEqualTo(7.5f);
        assertThat(event.maxScore()).isEqualTo(10f);
    }

    @Test
    @DisplayName("Does NOT publish event when other answers are still ungraded (avoid email spam)")
    void noEventWhilePendingAnswersExist() {
        attempt.setIsCompleted(true);

        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                .thenReturn(Optional.of(submission));
        when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(0f);
        when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(5f);
        // Still has pending OPEN_QUESTION etc.
        when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(1L);

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder().score(5f).build();
        service.gradeAnswer(1L, 10L, 5L, request);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Does NOT publish event for an unfinished attempt (student hasn't submitted yet)")
    void noEventBeforeStudentSubmits() {
        attempt.setIsCompleted(false);

        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                .thenReturn(Optional.of(submission));
        when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(0f);
        when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(5f);

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder().score(5f).build();
        service.gradeAnswer(1L, 10L, 5L, request);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Does not publish event when grading fails validation")
    void noEventOnValidationFailure() {
        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder()
                .score(15f)
                .feedback("over max")
                .build();

        assertThatThrownBy(() -> service.gradeAnswer(1L, 10L, 5L, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Negative score is rejected before any DB write")
    void rejectsNegativeScore() {
        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder()
                .score(-1f)
                .feedback("nope")
                .build();

        assertThatThrownBy(() -> service.gradeAnswer(1L, 10L, 5L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
        verify(codeSubmissionRepository, never()).save(any());
        verify(testAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Score exceeding maxPoints is rejected with informative message")
    void rejectsTooHighScore() {
        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder()
                .score(15f)
                .feedback("over")
                .build();

        assertThatThrownBy(() -> service.gradeAnswer(1L, 10L, 5L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max points (10)");
        verify(codeSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Null score is rejected")
    void rejectsNullScore() {
        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder()
                .score(null)
                .feedback("x")
                .build();

        assertThatThrownBy(() -> service.gradeAnswer(1L, 10L, 5L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("Missing CodeSubmissionEntity throws (student never submitted)")
    void missingSubmission() {
        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                .thenReturn(Optional.empty());

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder()
                .score(5f)
                .feedback("missed")
                .build();

        assertThatThrownBy(() -> service.gradeAnswer(1L, 10L, 5L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Code submission not found");
    }

    @Test
    @DisplayName("Re-grading overwrites previous feedback and score")
    void reGrading() {
        submission.setTotalScore(3.0f);
        submission.setTeacherFeedback("First pass — too rushed.");
        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                .thenReturn(Optional.of(submission));
        when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(0f);
        when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(7f);

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder()
                .score(7f)
                .feedback("Looked again — actually nice work.")
                .build();

        service.gradeAnswer(1L, 10L, 5L, request);

        assertThat(submission.getTotalScore()).isEqualTo(7f);
        assertThat(submission.getTeacherFeedback()).isEqualTo("Looked again — actually nice work.");
    }

    @Test
    @DisplayName("Null feedback is allowed (teacher leaves only score)")
    void nullFeedbackAllowed() {
        when(testAttemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(10L, 5L))
                .thenReturn(Optional.of(submission));
        when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(0f);
        when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(5f);

        GradeAnswerRequestDto request = GradeAnswerRequestDto.builder().score(5f).build();
        service.gradeAnswer(1L, 10L, 5L, request);

        assertThat(submission.getTeacherFeedback()).isNull();
        assertThat(submission.getTotalScore()).isEqualTo(5f);
    }
}
