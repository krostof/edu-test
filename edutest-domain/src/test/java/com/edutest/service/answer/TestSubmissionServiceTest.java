package com.edutest.service.answer;

import com.edutest.dto.TestSubmissionResultDto;
import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAnswerEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.*;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestSubmissionServiceTest {

    @Mock
    private TestAttemptJpaRepository testAttemptRepository;

    @Mock
    private AssignmentAnswerJpaRepository answerRepository;

    @Mock
    private CodeSubmissionJpaRepository codeSubmissionRepository;

    @Mock
    private AssignmentJpaRepository assignmentRepository;

    @InjectMocks
    private TestSubmissionService testSubmissionService;

    private TestEntity testEntity;
    private TestAttemptEntity attemptEntity;
    private UserEntity studentEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TestEntity();
        testEntity.setId(1L);
        testEntity.setTitle("Test Exam");

        studentEntity = new UserEntity();
        studentEntity.setId(100L);
        studentEntity.setUsername("student1");

        attemptEntity = new TestAttemptEntity();
        attemptEntity.setId(10L);
        attemptEntity.setTestEntity(testEntity);
        attemptEntity.setStudent(studentEntity);
        attemptEntity.setStartedAt(LocalDateTime.now());
        attemptEntity.setFinishedAt(null);
    }

    @Nested
    @DisplayName("submitTestAttempt validation tests")
    class SubmitTestAttemptValidationTests {

        @Test
        @DisplayName("Should throw exception when attempt not found")
        void shouldThrowWhenAttemptNotFound() {
            when(testAttemptRepository.findByIdWithTest(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    testSubmissionService.submitTestAttempt(1L, 10L, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test attempt not found");
        }

        @Test
        @DisplayName("Should throw exception when attempt doesn't belong to test")
        void shouldThrowWhenAttemptNotInTest() {
            TestEntity otherTest = new TestEntity();
            otherTest.setId(999L);
            attemptEntity.setTestEntity(otherTest);

            when(testAttemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attemptEntity));

            assertThatThrownBy(() ->
                    testSubmissionService.submitTestAttempt(1L, 10L, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to this test");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when student doesn't own attempt")
        void shouldThrowWhenStudentDoesNotOwnAttempt() {
            UserEntity otherStudent = new UserEntity();
            otherStudent.setId(999L);
            attemptEntity.setStudent(otherStudent);

            when(testAttemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attemptEntity));

            assertThatThrownBy(() ->
                    testSubmissionService.submitTestAttempt(1L, 10L, 100L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("Should throw exception when attempt already completed")
        void shouldThrowWhenAttemptAlreadyCompleted() {
            attemptEntity.setFinishedAt(LocalDateTime.now());

            when(testAttemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attemptEntity));

            assertThatThrownBy(() ->
                    testSubmissionService.submitTestAttempt(1L, 10L, 100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already completed");
        }
    }

    @Nested
    @DisplayName("Auto-grading tests")
    class AutoGradingTests {

        @Test
        @DisplayName("Should auto-grade choice answers on submission")
        void shouldAutoGradeChoiceAnswers() {
            SingleChoiceAssignmentEntityEntity singleChoiceAssignment = new SingleChoiceAssignmentEntityEntity();
            singleChoiceAssignment.setId(1L);
            singleChoiceAssignment.setPoints(10);

            SingleChoiceAnswerEntity singleChoiceAnswer = mock(SingleChoiceAnswerEntity.class);
            when(singleChoiceAnswer.getAssignmentEntity()).thenReturn(singleChoiceAssignment);
            when(singleChoiceAnswer.getIsGraded()).thenReturn(false);

            setupSuccessfulSubmission(List.of(singleChoiceAnswer));

            testSubmissionService.submitTestAttempt(1L, 10L, 100L);

            verify(singleChoiceAnswer).autoGrade();
            verify(answerRepository).save(singleChoiceAnswer);
        }

        @Test
        @DisplayName("Should not re-grade already graded answers")
        void shouldNotRegradeAlreadyGradedAnswers() {
            SingleChoiceAssignmentEntityEntity assignment = new SingleChoiceAssignmentEntityEntity();
            assignment.setId(1L);
            assignment.setPoints(10);

            SingleChoiceAnswerEntity answer = mock(SingleChoiceAnswerEntity.class);
            when(answer.getAssignmentEntity()).thenReturn(assignment);
            when(answer.getIsGraded()).thenReturn(true);

            setupSuccessfulSubmission(List.of(answer));

            testSubmissionService.submitTestAttempt(1L, 10L, 100L);

            verify(answer, never()).autoGrade();
        }
    }

    @Nested
    @DisplayName("Score calculation tests")
    class ScoreCalculationTests {

        @Test
        @DisplayName("Should calculate total score from answers")
        void shouldCalculateTotalScore() {
            when(testAttemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attemptEntity));
            when(answerRepository.findByTestAttemptId(10L)).thenReturn(List.of());
            when(codeSubmissionRepository.findByTestAttemptId(10L)).thenReturn(List.of());
            when(assignmentRepository.sumPointsByTestId(1L)).thenReturn(100f);
            when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(45f);
            when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(30f);
            when(answerRepository.countGradedByTestAttemptId(10L)).thenReturn(3L);
            when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(1L);
            when(assignmentRepository.findByTestEntityIdOrderByOrderNumber(1L)).thenReturn(List.of());
            when(testAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            TestSubmissionResultDto result = testSubmissionService.submitTestAttempt(1L, 10L, 100L);

            assertThat(result.getTotalScore()).isEqualTo(75f);
            assertThat(result.getMaxPossibleScore()).isEqualTo(100f);
        }

        @Test
        @DisplayName("Should handle null scores gracefully")
        void shouldHandleNullScores() {
            when(testAttemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attemptEntity));
            when(answerRepository.findByTestAttemptId(10L)).thenReturn(List.of());
            when(codeSubmissionRepository.findByTestAttemptId(10L)).thenReturn(List.of());
            when(assignmentRepository.sumPointsByTestId(1L)).thenReturn(null);
            when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(null);
            when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(null);
            when(answerRepository.countGradedByTestAttemptId(10L)).thenReturn(0L);
            when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(0L);
            when(assignmentRepository.findByTestEntityIdOrderByOrderNumber(1L)).thenReturn(List.of());
            when(testAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            TestSubmissionResultDto result = testSubmissionService.submitTestAttempt(1L, 10L, 100L);

            assertThat(result.getTotalScore()).isEqualTo(0f);
            assertThat(result.getMaxPossibleScore()).isEqualTo(0f);
        }
    }

    @Nested
    @DisplayName("Result DTO tests")
    class ResultDtoTests {

        @Test
        @DisplayName("Should return correct submission result")
        void shouldReturnCorrectSubmissionResult() {
            setupSuccessfulSubmission(List.of());
            when(assignmentRepository.sumPointsByTestId(1L)).thenReturn(50f);
            when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(40f);
            when(answerRepository.countGradedByTestAttemptId(10L)).thenReturn(4L);
            when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(1L);

            TestSubmissionResultDto result = testSubmissionService.submitTestAttempt(1L, 10L, 100L);

            assertThat(result.getAttemptId()).isEqualTo(10L);
            assertThat(result.getTestId()).isEqualTo(1L);
            assertThat(result.getSubmittedAt()).isNotNull();
            assertThat(result.getTotalScore()).isEqualTo(40f);
            assertThat(result.getMaxPossibleScore()).isEqualTo(50f);
            assertThat(result.getGradedCount()).isEqualTo(4);
            assertThat(result.getPendingGradingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should mark attempt as finished after submission")
        void shouldMarkAttemptAsFinished() {
            setupSuccessfulSubmission(List.of());
            when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(80f);

            testSubmissionService.submitTestAttempt(1L, 10L, 100L);

            assertThat(attemptEntity.getFinishedAt()).isNotNull();
            assertThat(attemptEntity.getScore()).isEqualTo(80f);
            verify(testAttemptRepository).save(attemptEntity);
        }
    }

    @Nested
    @DisplayName("Helper method tests")
    class HelperMethodTests {

        @Test
        @DisplayName("isAttemptCompleted should return true for finished attempt")
        void isAttemptCompletedShouldReturnTrueForFinished() {
            attemptEntity.setFinishedAt(LocalDateTime.now());
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));

            boolean result = testSubmissionService.isAttemptCompleted(10L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isAttemptCompleted should return false for in-progress attempt")
        void isAttemptCompletedShouldReturnFalseForInProgress() {
            attemptEntity.setFinishedAt(null);
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));

            boolean result = testSubmissionService.isAttemptCompleted(10L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("canAccessResults should return true for owner of finished attempt")
        void canAccessResultsShouldReturnTrueForOwner() {
            attemptEntity.setFinishedAt(LocalDateTime.now());
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));

            boolean result = testSubmissionService.canAccessResults(10L, 100L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("canAccessResults should return false for non-owner")
        void canAccessResultsShouldReturnFalseForNonOwner() {
            attemptEntity.setFinishedAt(LocalDateTime.now());
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));

            boolean result = testSubmissionService.canAccessResults(10L, 999L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("canAccessResults should return false for in-progress attempt")
        void canAccessResultsShouldReturnFalseForInProgress() {
            attemptEntity.setFinishedAt(null);
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));

            boolean result = testSubmissionService.canAccessResults(10L, 100L);

            assertThat(result).isFalse();
        }
    }

    private void setupSuccessfulSubmission(List<AssignmentAnswerEntity> answers) {
        when(testAttemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attemptEntity));
        when(answerRepository.findByTestAttemptId(10L)).thenReturn(answers);
        when(codeSubmissionRepository.findByTestAttemptId(10L)).thenReturn(List.of());
        when(assignmentRepository.sumPointsByTestId(1L)).thenReturn(100f);
        when(answerRepository.sumScoresByTestAttemptId(10L)).thenReturn(0f);
        when(codeSubmissionRepository.sumScoresByTestAttemptId(10L)).thenReturn(null);
        when(answerRepository.countGradedByTestAttemptId(10L)).thenReturn(0L);
        when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(0L);
        when(assignmentRepository.findByTestEntityIdOrderByOrderNumber(1L)).thenReturn(List.of());
        when(testAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
