package com.edutest.service.answer;

import com.edutest.dto.AnswerDto;
import com.edutest.dto.SubmitAnswerRequestDto;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAnswerEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.*;
import com.edutest.service.attempt.AttemptRandomizationService;
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
class AnswerSubmissionServiceTest {

    @Mock
    private TestAttemptJpaRepository testAttemptRepository;

    @Mock
    private AssignmentJpaRepository assignmentRepository;

    @Mock
    private AssignmentAnswerJpaRepository answerRepository;

    @Mock
    private CodeSubmissionJpaRepository codeSubmissionRepository;

    @Mock
    private ChoiceOptionJpaRepository choiceOptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttemptRandomizationService randomizationService;

    @InjectMocks
    private AnswerSubmissionService answerSubmissionService;

    private TestEntity testEntity;
    private TestAttemptEntity attemptEntity;
    private UserEntity studentEntity;
    private SingleChoiceAssignmentEntityEntity assignmentEntity;
    private ChoiceOptionEntity optionEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TestEntity();
        testEntity.setId(1L);
        testEntity.setTitle("Test Exam");
        testEntity.setAllowNavigation(true);

        studentEntity = new UserEntity();
        studentEntity.setId(100L);
        studentEntity.setUsername("student1");

        attemptEntity = new TestAttemptEntity();
        attemptEntity.setId(10L);
        attemptEntity.setTestEntity(testEntity);
        attemptEntity.setStudent(studentEntity);
        attemptEntity.setStartedAt(LocalDateTime.now());
        attemptEntity.setFinishedAt(null);

        assignmentEntity = new SingleChoiceAssignmentEntityEntity();
        assignmentEntity.setId(5L);
        assignmentEntity.setTestEntity(testEntity);
        assignmentEntity.setTitle("Question 1");
        assignmentEntity.setPoints(10);

        optionEntity = new ChoiceOptionEntity();
        optionEntity.setId(50L);
        optionEntity.setOptionText("Option A");
        optionEntity.setIsCorrect(true);
    }

    @Nested
    @DisplayName("submitAnswer validation tests")
    class SubmitAnswerValidationTests {

        @Test
        @DisplayName("Should throw exception when test attempt not found")
        void shouldThrowWhenAttemptNotFound() {
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.empty());

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(50L)
                    .build();

            assertThatThrownBy(() ->
                    answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test attempt not found");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when student doesn't own attempt")
        void shouldThrowWhenStudentDoesNotOwnAttempt() {
            UserEntity otherStudent = new UserEntity();
            otherStudent.setId(999L);
            attemptEntity.setStudent(otherStudent);

            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(50L)
                    .build();

            assertThatThrownBy(() ->
                    answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("Should throw exception when assignment not found")
        void shouldThrowWhenAssignmentNotFound() {
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.empty());

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(50L)
                    .build();

            assertThatThrownBy(() ->
                    answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Assignment not found");
        }

        @Test
        @DisplayName("Should throw exception when assignment doesn't belong to test")
        void shouldThrowWhenAssignmentNotInTest() {
            TestEntity otherTest = new TestEntity();
            otherTest.setId(999L);
            assignmentEntity.setTestEntity(otherTest);

            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignmentEntity));

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(50L)
                    .build();

            assertThatThrownBy(() ->
                    answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to this test");
        }

        @Test
        @DisplayName("Should throw exception when test attempt is already completed")
        void shouldThrowWhenAttemptCompleted() {
            attemptEntity.setFinishedAt(LocalDateTime.now());

            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignmentEntity));

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(50L)
                    .build();

            assertThatThrownBy(() ->
                    answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already completed");
        }
    }

    @Nested
    @DisplayName("Navigation restriction tests")
    class NavigationRestrictionTests {

        @Test
        @DisplayName("Should allow any navigation when allowNavigation is true")
        void shouldAllowNavigationWhenEnabled() {
            testEntity.setAllowNavigation(true);

            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignmentEntity));
            when(userRepository.findById(100L)).thenReturn(Optional.of(studentEntity));
            when(answerRepository.findByTestAttemptIdAndAssignmentId(10L, 5L)).thenReturn(Optional.empty());
            when(choiceOptionRepository.findById(50L)).thenReturn(Optional.of(optionEntity));
            when(answerRepository.save(any())).thenAnswer(invocation -> {
                SingleChoiceAnswerEntity answer = invocation.getArgument(0);
                answer.setId(1L);
                return answer;
            });

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(50L)
                    .build();

            AnswerDto result = answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request);

            assertThat(result).isNotNull();
            assertThat(result.getSelectedOptionId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("Should throw when trying to go back with navigation disabled")
        void shouldThrowWhenGoingBackWithNavigationDisabled() {
            testEntity.setAllowNavigation(false);
            attemptEntity.setCurrentQuestionIndex(2);

            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignmentEntity));
            when(randomizationService.getAssignmentOrder(attemptEntity)).thenReturn(List.of(5L, 6L, 7L));

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(50L)
                    .build();

            assertThatThrownBy(() ->
                    answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Navigation to previous questions is not allowed");
        }
    }

    @Nested
    @DisplayName("Single choice answer tests")
    class SingleChoiceAnswerTests {

        @Test
        @DisplayName("Should successfully submit single choice answer")
        void shouldSubmitSingleChoiceAnswer() {
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignmentEntity));
            when(userRepository.findById(100L)).thenReturn(Optional.of(studentEntity));
            when(answerRepository.findByTestAttemptIdAndAssignmentId(10L, 5L)).thenReturn(Optional.empty());
            when(choiceOptionRepository.findById(50L)).thenReturn(Optional.of(optionEntity));
            when(answerRepository.save(any())).thenAnswer(invocation -> {
                SingleChoiceAnswerEntity answer = invocation.getArgument(0);
                answer.setId(1L);
                return answer;
            });

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(50L)
                    .build();

            AnswerDto result = answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request);

            assertThat(result).isNotNull();
            assertThat(result.getAssignmentId()).isEqualTo(5L);
            assertThat(result.getSelectedOptionId()).isEqualTo(50L);
            assertThat(result.getAssignmentType()).isEqualTo(AssignmentType.SINGLE_CHOICE.name());

            verify(answerRepository).save(any(SingleChoiceAnswerEntity.class));
        }

        @Test
        @DisplayName("Should update existing single choice answer")
        void shouldUpdateExistingSingleChoiceAnswer() {
            SingleChoiceAnswerEntity existingAnswer = new SingleChoiceAnswerEntity();
            existingAnswer.setId(1L);
            existingAnswer.setAssignmentEntity(assignmentEntity);
            existingAnswer.setTestAttemptEntity(attemptEntity);
            existingAnswer.setStudent(studentEntity);

            ChoiceOptionEntity newOption = new ChoiceOptionEntity();
            newOption.setId(51L);
            newOption.setOptionText("Option B");

            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignmentEntity));
            when(userRepository.findById(100L)).thenReturn(Optional.of(studentEntity));
            when(answerRepository.findByTestAttemptIdAndAssignmentId(10L, 5L)).thenReturn(Optional.of(existingAnswer));
            when(choiceOptionRepository.findById(51L)).thenReturn(Optional.of(newOption));
            when(answerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            SubmitAnswerRequestDto request = SubmitAnswerRequestDto.builder()
                    .selectedOptionId(51L)
                    .build();

            AnswerDto result = answerSubmissionService.submitAnswer(1L, 10L, 5L, 100L, request);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSelectedOptionId()).isEqualTo(51L);
        }
    }

    @Nested
    @DisplayName("getAnswer tests")
    class GetAnswerTests {

        @Test
        @DisplayName("Should return answer when found")
        void shouldReturnAnswerWhenFound() {
            SingleChoiceAnswerEntity existingAnswer = new SingleChoiceAnswerEntity();
            existingAnswer.setId(1L);
            existingAnswer.setAssignmentEntity(assignmentEntity);
            existingAnswer.setSelectedOption(optionEntity);
            existingAnswer.setAnsweredAt(LocalDateTime.now());

            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignmentEntity));
            when(answerRepository.findByTestAttemptIdAndAssignmentId(10L, 5L)).thenReturn(Optional.of(existingAnswer));

            Optional<AnswerDto> result = answerSubmissionService.getAnswer(1L, 10L, 5L, 100L);

            assertThat(result).isPresent();
            assertThat(result.get().getSelectedOptionId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("Should return empty when answer not found")
        void shouldReturnEmptyWhenNotFound() {
            when(testAttemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignmentEntity));
            when(answerRepository.findByTestAttemptIdAndAssignmentId(10L, 5L)).thenReturn(Optional.empty());

            Optional<AnswerDto> result = answerSubmissionService.getAnswer(1L, 10L, 5L, 100L);

            assertThat(result).isEmpty();
        }
    }
}
