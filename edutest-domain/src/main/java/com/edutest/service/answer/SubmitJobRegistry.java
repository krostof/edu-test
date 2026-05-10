package com.edutest.service.answer;

import com.edutest.dto.SubmitStatusDto;
import com.edutest.dto.TestSubmissionResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of running and completed test-submission jobs, keyed by attemptId.
 *
 * Kept separate from {@link com.edutest.service.codeexecution.CodeRunJobRegistry} —
 * different ownership (attempt vs. single submission), different payload type, and
 * different lifecycle (a submit job represents the final grading of an attempt and
 * should not race with preview run jobs).
 *
 * Loss of state on restart is tolerable: the attempt is persisted as finished in DB
 * by {@code TestSubmissionService}, so worst case the student polls and sees NONE,
 * then frontend redirects to the results page (which reads from DB).
 */
@Slf4j
@Component
public class SubmitJobRegistry {

    /** How long a completed/failed job stays readable. After this, status becomes NONE. */
    private static final long READ_TTL_SECONDS = 600; // 10 minutes — students may take time on the spinner page

    /** How long a PENDING job stays before being considered "stuck" and orphaned. */
    private static final long PENDING_TIMEOUT_SECONDS = 600; // 10 minutes — full submit can be slow with many CODING tasks

    private final Map<Long, SubmitJob> jobs = new ConcurrentHashMap<>();

    public enum Status { PENDING, DONE, FAILED }

    public static class SubmitJob {
        public final Status status;
        public final LocalDateTime startedAt;
        public final LocalDateTime completedAt;
        public final TestSubmissionResultDto result;
        public final String error;

        private SubmitJob(Status status, LocalDateTime startedAt, LocalDateTime completedAt,
                          TestSubmissionResultDto result, String error) {
            this.status = status;
            this.startedAt = startedAt;
            this.completedAt = completedAt;
            this.result = result;
            this.error = error;
        }

        static SubmitJob pending() {
            return new SubmitJob(Status.PENDING, LocalDateTime.now(), null, null, null);
        }

        SubmitJob done(TestSubmissionResultDto result) {
            return new SubmitJob(Status.DONE, this.startedAt, LocalDateTime.now(), result, null);
        }

        SubmitJob failed(String error) {
            return new SubmitJob(Status.FAILED, this.startedAt, LocalDateTime.now(), null, error);
        }
    }

    /** Mark this attempt as having a pending submit. Replaces any existing job. */
    public void markPending(Long attemptId) {
        jobs.put(attemptId, SubmitJob.pending());
        log.debug("Submit job PENDING for attempt {}", attemptId);
    }

    public void markDone(Long attemptId, TestSubmissionResultDto result) {
        jobs.computeIfPresent(attemptId, (id, existing) -> existing.done(result));
        log.debug("Submit job DONE for attempt {}", attemptId);
    }

    public void markFailed(Long attemptId, String error) {
        jobs.computeIfPresent(attemptId, (id, existing) -> existing.failed(error));
        log.warn("Submit job FAILED for attempt {}: {}", attemptId, error);
    }

    public SubmitStatusDto getStatus(Long attemptId) {
        SubmitJob job = jobs.get(attemptId);
        if (job == null) {
            return SubmitStatusDto.builder().status("NONE").build();
        }

        // Clean up stuck/expired entries lazily
        if (job.status == Status.PENDING && isOlderThan(job.startedAt, PENDING_TIMEOUT_SECONDS)) {
            jobs.remove(attemptId);
            return SubmitStatusDto.builder().status("NONE").build();
        }
        if ((job.status == Status.DONE || job.status == Status.FAILED)
                && isOlderThan(job.completedAt, READ_TTL_SECONDS)) {
            jobs.remove(attemptId);
            return SubmitStatusDto.builder().status("NONE").build();
        }

        return SubmitStatusDto.builder()
                .status(job.status.name())
                .startedAt(job.startedAt)
                .completedAt(job.completedAt)
                .result(job.result)
                .error(job.error)
                .build();
    }

    private static boolean isOlderThan(LocalDateTime ts, long seconds) {
        return ts != null && ts.isBefore(LocalDateTime.now().minusSeconds(seconds));
    }
}
