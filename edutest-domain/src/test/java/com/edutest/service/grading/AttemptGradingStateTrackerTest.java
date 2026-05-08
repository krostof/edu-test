package com.edutest.service.grading;

import com.edutest.event.AnswerGradedEvent;
import com.edutest.event.TestAttemptGradedEvent;
import com.edutest.event.TestAttemptSubmittedEvent;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.AssignmentAnswerJpaRepository;
import com.edutest.persistance.repository.AssignmentJpaRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttemptGradingStateTrackerTest {

    @Mock private TestAttemptJpaRepository attemptRepository;
    @Mock private AssignmentAnswerJpaRepository answerRepository;
    @Mock private CodeSubmissionJpaRepository codeSubmissionRepository;
    @Mock private AssignmentJpaRepository assignmentRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AttemptGradingStateTracker tracker;

    private TestEntity testEntity;
    private TestAttemptEntity attempt;
    private CodeSubmissionEntity codeSubmissionScored;
    private CodeSubmissionEntity codeSubmissionPending;

    @BeforeEach
    void setUp() {
        testEntity = new TestEntity();
        testEntity.setId(1L);

        UserEntity student = new UserEntity();
        student.setId(100L);

        attempt = new TestAttemptEntity();
        attempt.setId(10L);
        attempt.setTestEntity(testEntity);
        attempt.setStudent(student);
        attempt.setIsCompleted(true);
        attempt.setScore(7.5f);

        codeSubmissionScored = CodeSubmissionEntity.builder().totalScore(5f).build();
        codeSubmissionPending = CodeSubmissionEntity.builder().totalScore(null).build();
    }

    @Test
    @DisplayName("Submitted event with everything graded → fires derived TestAttemptGradedEvent")
    void submittedAndFullyGraded() {
        when(attemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(0L);
        when(codeSubmissionRepository.findByTestAttemptId(10L))
                .thenReturn(List.of(codeSubmissionScored));
        when(assignmentRepository.sumPointsByTestId(1L)).thenReturn(10f);

        tracker.onTestAttemptSubmitted(new TestAttemptSubmittedEvent(10L, 1L, 100L));

        ArgumentCaptor<TestAttemptGradedEvent> captor = ArgumentCaptor.forClass(TestAttemptGradedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        TestAttemptGradedEvent fired = captor.getValue();
        assertThat(fired.attemptId()).isEqualTo(10L);
        assertThat(fired.testId()).isEqualTo(1L);
        assertThat(fired.studentId()).isEqualTo(100L);
        assertThat(fired.totalScore()).isEqualTo(7.5f);
        assertThat(fired.maxScore()).isEqualTo(10f);
    }

    @Test
    @DisplayName("AnswerGraded event after last pending → fires derived event")
    void answerGradedTransitionsAttemptToFullyGraded() {
        when(attemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(0L);
        when(codeSubmissionRepository.findByTestAttemptId(10L)).thenReturn(List.of(codeSubmissionScored));
        when(assignmentRepository.sumPointsByTestId(1L)).thenReturn(10f);

        tracker.onAnswerGraded(new AnswerGradedEvent(10L, 1L, 100L, 5L));

        verify(eventPublisher).publishEvent(any(TestAttemptGradedEvent.class));
    }

    @Test
    @DisplayName("Anti-double-fire: second event for same attempt does not re-fire")
    void doesNotFireTwiceForSameAttempt() {
        when(attemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(0L);
        when(codeSubmissionRepository.findByTestAttemptId(10L)).thenReturn(List.of(codeSubmissionScored));
        when(assignmentRepository.sumPointsByTestId(1L)).thenReturn(10f);

        tracker.onTestAttemptSubmitted(new TestAttemptSubmittedEvent(10L, 1L, 100L));
        tracker.onAnswerGraded(new AnswerGradedEvent(10L, 1L, 100L, 5L));
        tracker.onAnswerGraded(new AnswerGradedEvent(10L, 1L, 100L, 6L));

        verify(eventPublisher, times(1)).publishEvent(any(TestAttemptGradedEvent.class));
    }

    @Test
    @DisplayName("Pending non-code answers → no derived event")
    void pendingAnswersBlockEvent() {
        when(attemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(2L);

        tracker.onTestAttemptSubmitted(new TestAttemptSubmittedEvent(10L, 1L, 100L));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Code submission with totalScore=null (e.g. SYSTEM_ERROR) → no derived event")
    void pendingCodeSubmissionBlocksEvent() {
        when(attemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));
        when(answerRepository.countUngradedByTestAttemptId(10L)).thenReturn(0L);
        when(codeSubmissionRepository.findByTestAttemptId(10L))
                .thenReturn(List.of(codeSubmissionScored, codeSubmissionPending));

        tracker.onTestAttemptSubmitted(new TestAttemptSubmittedEvent(10L, 1L, 100L));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Unfinished attempt (student hasn't submitted) → no derived event")
    void unfinishedAttemptIgnored() {
        attempt.setIsCompleted(false);
        when(attemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.of(attempt));

        tracker.onAnswerGraded(new AnswerGradedEvent(10L, 1L, 100L, 5L));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Missing attempt (concurrent delete) → no derived event, no exception")
    void missingAttemptHandled() {
        when(attemptRepository.findByIdWithTestAndStudent(10L)).thenReturn(Optional.empty());

        tracker.onTestAttemptSubmitted(new TestAttemptSubmittedEvent(10L, 1L, 100L));

        verify(eventPublisher, never()).publishEvent(any());
    }
}
