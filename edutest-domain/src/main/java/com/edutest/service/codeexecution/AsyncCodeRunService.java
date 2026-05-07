package com.edutest.service.codeexecution;

import com.edutest.dto.AnswerDto;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.repository.CodeSubmissionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges synchronous {@link CodeExecutionService} into the async + polling flow.
 *
 * <p>Why a separate bean? {@code @Async} only takes effect through Spring proxies —
 * methods annotated with {@code @Async} must be called from a different bean than
 * the one defining them. {@link com.edutest.service.answer.AnswerSubmissionService#runCode}
 * calls {@link #executeAsync} on this bean, which Spring proxies and dispatches to
 * the {@code codeRunExecutor} thread pool.
 *
 * <p>Loads its own copy of the submission inside the worker — the request's transaction
 * is already committed by the time the async method runs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCodeRunService {

    private final CodeExecutionService codeExecutionService;
    private final CodeSubmissionJpaRepository submissionRepository;
    private final CodeRunJobRegistry registry;

    @Async("codeRunExecutor")
    @Transactional(readOnly = true)
    public void executeAsync(Long submissionId) {
        try {
            CodeSubmissionEntity submission = submissionRepository.findById(submissionId)
                    .orElse(null);
            if (submission == null) {
                registry.markFailed(submissionId, "Submission not found");
                return;
            }
            // Trigger lazy collections inside the worker's transaction
            submission.getAssignment().getTestCases().size();

            AnswerDto result = codeExecutionService.runPreview(submission);
            registry.markDone(submissionId, result);
        } catch (Exception e) {
            log.error("Async preview failed for submission {}: {}", submissionId, e.getMessage(), e);
            registry.markFailed(submissionId,
                    e.getMessage() != null ? e.getMessage() : "Unknown execution error");
        }
    }
}
