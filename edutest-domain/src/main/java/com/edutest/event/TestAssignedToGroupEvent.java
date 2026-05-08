package com.edutest.event;

/**
 * Published when a teacher/admin attaches a student group to a test.
 *
 * Listeners (e.g., email notifier) load whatever data they need by ID — the event
 * itself is intentionally minimal so it survives serialization (future async queues)
 * and doesn't carry stale references to JPA entities outside their session.
 */
public record TestAssignedToGroupEvent(Long testId, Long groupId) {
}
