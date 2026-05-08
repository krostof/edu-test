package com.edutest.event;

/**
 * Published when a student finishes a test attempt — auto-grading has just run, but the
 * attempt may or may not be fully graded depending on whether OPEN_QUESTION assignments
 * are present (those need manual grading).
 *
 * <p>Primitive fact only: "this student submitted this test." Whether downstream effects
 * fire (notifications, analytics) is not this service's concern. {@code AttemptGradingStateTracker}
 * is one such listener — it derives a "fully-graded" event from this primitive plus
 * {@link AnswerGradedEvent}.
 */
public record TestAttemptSubmittedEvent(Long attemptId, Long testId, Long studentId) {
}
