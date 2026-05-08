package com.edutest.service.grading;

import com.edutest.event.AnswerGradedEvent;
import com.edutest.event.TestAttemptGradedEvent;
import com.edutest.event.TestAttemptSubmittedEvent;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.repository.AssignmentAnswerJpaRepository;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import com.edutest.persistance.repository.CodeSubmissionJpaRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects when a test attempt transitions from "partially graded" to "fully graded"
 * and emits a derived {@link TestAttemptGradedEvent}.
 *
 * <p>Only place in the codebase that knows the rule "an attempt is fully graded
 * iff every answer has a score." Both submission ({@link TestAttemptSubmittedEvent})
 * and manual grading ({@link AnswerGradedEvent}) feed into this tracker.
 *
 * <p><b>Why a tracker, not a service-level check?</b> The original design had this
 * logic duplicated in {@code TestSubmissionService} and {@code ManualGradingService},
 * forcing both to know "is fully graded" semantics. Now each service only publishes
 * its own primitive fact — this tracker derives the higher-order event.
 *
 * <p><b>Anti-double-fire:</b> in-memory {@link Set} of already-notified attempts.
 * Survives single JVM lifetime; on restart, an attempt that was fully-graded before
 * the restart could re-emit if a new {@code AnswerGradedEvent} fires for it. We accept
 * this — the only realistic trigger after restart is the teacher re-grading something,
 * which already implies the student should be informed of the new score.
 *
 * <p>Listeners use {@code @EventListener} (not transactional) because we want to react
 * within the same transaction as the publishing service — if the publishing rolls back,
 * the event is simply not delivered, which is what we want. The downstream
 * {@link TestAttemptGradedEvent} is then fired and consumed by
 * {@code EmailNotificationListener} after AFTER_COMMIT in its own transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttemptGradingStateTracker {

    private final TestAttemptJpaRepository attemptRepository;
    private final AssignmentAnswerJpaRepository answerRepository;
    private final CodeSubmissionJpaRepository codeSubmissionRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** AttemptIds for which TestAttemptGradedEvent already fired. Best-effort dedup. */
    private final Set<Long> notifiedAttempts = ConcurrentHashMap.newKeySet();

    @EventListener
    @Transactional(readOnly = true)
    public void onTestAttemptSubmitted(TestAttemptSubmittedEvent event) {
        evaluate(event.attemptId());
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onAnswerGraded(AnswerGradedEvent event) {
        evaluate(event.attemptId());
    }

    private void evaluate(Long attemptId) {
        TestAttemptEntity attempt = attemptRepository.findByIdWithTestAndStudent(attemptId)
                .orElse(null);
        if (attempt == null) {
            log.debug("Attempt {} not found while evaluating grading state", attemptId);
            return;
        }
        // Don't notify mid-test — only after student submits
        if (!Boolean.TRUE.equals(attempt.getIsCompleted())) {
            return;
        }
        if (!isFullyGraded(attemptId)) {
            return;
        }
        // Anti-double-fire: only emit on the transition
        if (!notifiedAttempts.add(attemptId)) {
            log.debug("Attempt {} already notified as fully graded; skipping", attemptId);
            return;
        }

        Float maxScore = assignmentRepository.sumPointsByTestId(attempt.getTestEntity().getId());
        eventPublisher.publishEvent(new TestAttemptGradedEvent(
                attempt.getId(),
                attempt.getTestEntity().getId(),
                attempt.getStudent().getId(),
                attempt.getScore(),
                maxScore != null ? maxScore : 0f
        ));
        log.info("Attempt {} transitioned to fully-graded — derived event fired", attemptId);
    }

    private boolean isFullyGraded(Long attemptId) {
        long ungradedAnswers = answerRepository.countUngradedByTestAttemptId(attemptId);
        if (ungradedAnswers > 0) {
            return false;
        }
        return codeSubmissionRepository.findByTestAttemptId(attemptId).stream()
                .allMatch(s -> s.getTotalScore() != null);
    }
}
