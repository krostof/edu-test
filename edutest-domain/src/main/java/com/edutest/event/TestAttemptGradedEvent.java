package com.edutest.event;

/**
 * Published when a student's test attempt becomes fully graded — every assignment
 * has a score (auto-graded or teacher-graded).
 *
 * <p>Two trigger points:
 *  - {@code TestSubmissionService.submitTestAttempt}: student finishes the test and
 *    all assignments are auto-graded (single/multiple choice, CODING with successful
 *    execution). No OPEN_QUESTION blocks => fire immediately.
 *  - {@code ManualGradingService.gradeAnswer}: teacher grades the last pending
 *    answer (typically an OPEN_QUESTION), unblocking the attempt.
 *
 * <p>Only one email per attempt — never one per answer — even if the teacher grades
 * multiple answers in sequence (we publish only when the attempt transitions from
 * partially-graded to fully-graded).
 */
public record TestAttemptGradedEvent(
        Long attemptId,
        Long testId,
        Long studentId,
        Float totalScore,
        Float maxScore
) {
}
