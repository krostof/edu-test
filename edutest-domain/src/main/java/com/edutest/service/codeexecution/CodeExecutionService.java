package com.edutest.service.codeexecution;

import com.edutest.persistance.entity.code.CodeSubmissionEntity;

/**
 * Single seam between core domain and the {@code edutest-code-execution} module.
 *
 * <p>Implementations run the submitted source code against the assignment's
 * test cases (typically inside a sandboxed container) and mutate the passed-in
 * {@link CodeSubmissionEntity} with compilation status, execution status,
 * per-test-case results and the resulting score.
 *
 * <p>The submission is expected to be managed by the caller's transaction —
 * implementations only mutate fields and add {@code TestCaseResultEntity}
 * children; the caller persists the change.
 */
public interface CodeExecutionService {

    void executeAndPersist(CodeSubmissionEntity submission);
}
