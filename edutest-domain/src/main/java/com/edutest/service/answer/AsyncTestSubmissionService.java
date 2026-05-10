package com.edutest.service.answer;

import com.edutest.dto.TestSubmissionResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Bridges synchronous {@link TestSubmissionService} into the async + polling flow
 * so a submit with many CODING questions doesn't block the HTTP request thread for
 * minutes (which would risk LB / proxy 504 timeouts).
 *
 * <p>Why a separate bean? {@code @Async} only takes effect through Spring proxies —
 * methods annotated with {@code @Async} must be called from a different bean than
 * the one defining them. The controller calls {@link #executeAsync} on this bean,
 * which Spring proxies and dispatches to the {@code submitGradingExecutor} pool.
 *
 * <p>Failures inside the worker are routed to {@link SubmitJobRegistry#markFailed}
 * — they do NOT propagate to the request thread (which has long since returned 202).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTestSubmissionService {

    private final TestSubmissionService testSubmissionService;
    private final SubmitJobRegistry registry;

    @Async("submitGradingExecutor")
    public void executeAsync(Long testId, Long attemptId, Long studentId) {
        try {
            TestSubmissionResultDto result = testSubmissionService.submitTestAttempt(testId, attemptId, studentId);
            registry.markDone(attemptId, result);
        } catch (Exception e) {
            log.error("Async submit failed for attempt {}: {}", attemptId, e.getMessage(), e);
            registry.markFailed(attemptId,
                    e.getMessage() != null ? e.getMessage() : "Unknown grading error");
        }
    }
}
