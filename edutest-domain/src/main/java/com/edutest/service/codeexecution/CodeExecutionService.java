package com.edutest.service.codeexecution;

import com.edutest.dto.AnswerDto;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;

/**
 * Single seam between core domain and the {@code edutest-code-execution} module.
 *
 * <p>Implementations run the submitted source code against the assignment's
 * test cases (typically inside a sandboxed container).
 *
 * <p>{@link #executeAndPersist} runs ALL test cases and mutates the passed-in
 * {@link CodeSubmissionEntity} with the final results — used at test submission
 * for grading.
 *
 * <p>{@link #runPreview} runs only test cases marked public ({@code isPublic=true})
 * and returns a transient {@link AnswerDto} without persisting any changes —
 * used for the student's "Run tests" button during the attempt.
 */
public interface CodeExecutionService {

    void executeAndPersist(CodeSubmissionEntity submission);

    AnswerDto runPreview(CodeSubmissionEntity submission);
}
