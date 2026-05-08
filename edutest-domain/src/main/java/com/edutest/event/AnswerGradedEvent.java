package com.edutest.event;

/**
 * Published when a teacher manually grades a single student answer (CODING with score
 * override, OPEN_QUESTION text grade, or — rarely — overriding an auto-graded choice).
 *
 * <p>Primitive fact only: "this answer just got a grade." {@code ManualGradingService}
 * does not know whether this was the last pending answer, whether to email the student,
 * or anything beyond persisting the score.
 *
 * <p>{@code AttemptGradingStateTracker} listens to this and decides whether the attempt
 * has now transitioned to fully-graded.
 */
public record AnswerGradedEvent(Long attemptId, Long testId, Long studentId, Long assignmentId) {
}
