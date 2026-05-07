package com.edutest.service.codeexecution;

import com.edutest.dto.AnswerDto;
import com.edutest.dto.RunStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of running and completed code run jobs.
 *
 * Preview is transient by design — losing job state on app restart is acceptable
 * (student re-clicks "Run tests" if needed). Persisting would require new tables
 * and complicate cleanup.
 *
 * Old completed/failed entries are evicted on read after {@link #READ_TTL_SECONDS}
 * to prevent unbounded growth. Scheduled cleanup happens lazily — no separate thread.
 */
@Slf4j
@Component
public class CodeRunJobRegistry {

    /** How long a completed/failed job stays readable. After this, status becomes NONE. */
    private static final long READ_TTL_SECONDS = 300; // 5 minutes

    /** How long a PENDING job stays before being considered "stuck" and orphaned. */
    private static final long PENDING_TIMEOUT_SECONDS = 180; // 3 minutes

    private final Map<Long, RunJob> jobs = new ConcurrentHashMap<>();

    public enum Status { PENDING, DONE, FAILED }

    public static class RunJob {
        public final Status status;
        public final LocalDateTime startedAt;
        public final LocalDateTime completedAt;
        public final AnswerDto result;
        public final String error;

        private RunJob(Status status, LocalDateTime startedAt, LocalDateTime completedAt,
                       AnswerDto result, String error) {
            this.status = status;
            this.startedAt = startedAt;
            this.completedAt = completedAt;
            this.result = result;
            this.error = error;
        }

        static RunJob pending() {
            return new RunJob(Status.PENDING, LocalDateTime.now(), null, null, null);
        }

        RunJob done(AnswerDto result) {
            return new RunJob(Status.DONE, this.startedAt, LocalDateTime.now(), result, null);
        }

        RunJob failed(String error) {
            return new RunJob(Status.FAILED, this.startedAt, LocalDateTime.now(), null, error);
        }
    }

    /** Mark this submission as having a pending run. Replaces any existing job. */
    public void markPending(Long submissionId) {
        jobs.put(submissionId, RunJob.pending());
        log.debug("Run job PENDING for submission {}", submissionId);
    }

    public void markDone(Long submissionId, AnswerDto result) {
        jobs.computeIfPresent(submissionId, (id, existing) -> existing.done(result));
        log.debug("Run job DONE for submission {}", submissionId);
    }

    public void markFailed(Long submissionId, String error) {
        jobs.computeIfPresent(submissionId, (id, existing) -> existing.failed(error));
        log.warn("Run job FAILED for submission {}: {}", submissionId, error);
    }

    public RunStatusDto getStatus(Long submissionId) {
        RunJob job = jobs.get(submissionId);
        if (job == null) {
            return RunStatusDto.builder().status("NONE").build();
        }

        // Clean up stuck/expired entries lazily
        if (job.status == Status.PENDING && isOlderThan(job.startedAt, PENDING_TIMEOUT_SECONDS)) {
            jobs.remove(submissionId);
            return RunStatusDto.builder().status("NONE").build();
        }
        if ((job.status == Status.DONE || job.status == Status.FAILED)
                && isOlderThan(job.completedAt, READ_TTL_SECONDS)) {
            jobs.remove(submissionId);
            return RunStatusDto.builder().status("NONE").build();
        }

        return RunStatusDto.builder()
                .status(job.status.name())
                .startedAt(job.startedAt)
                .completedAt(job.completedAt)
                .result(job.result)
                .error(job.error)
                .build();
    }

    public Optional<RunJob> peek(Long submissionId) {
        return Optional.ofNullable(jobs.get(submissionId));
    }

    private static boolean isOlderThan(LocalDateTime ts, long seconds) {
        return ts != null && ts.isBefore(LocalDateTime.now().minusSeconds(seconds));
    }
}
