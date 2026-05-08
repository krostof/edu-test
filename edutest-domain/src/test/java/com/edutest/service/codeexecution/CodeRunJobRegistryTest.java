package com.edutest.service.codeexecution;

import com.edutest.dto.AnswerDto;
import com.edutest.dto.RunStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeRunJobRegistryTest {

    private CodeRunJobRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CodeRunJobRegistry();
    }

    @Nested
    @DisplayName("Initial state")
    class InitialStateTests {

        @Test
        @DisplayName("Returns NONE for unknown submission")
        void returnsNoneForUnknown() {
            RunStatusDto status = registry.getStatus(999L);

            assertThat(status.getStatus()).isEqualTo("NONE");
            assertThat(status.getResult()).isNull();
            assertThat(status.getError()).isNull();
        }

        @Test
        @DisplayName("peek() returns empty for unknown submission")
        void peekEmpty() {
            assertThat(registry.peek(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("markPending sets status to PENDING")
        void pendingSetsStatus() {
            registry.markPending(1L);

            RunStatusDto status = registry.getStatus(1L);
            assertThat(status.getStatus()).isEqualTo("PENDING");
            assertThat(status.getStartedAt()).isNotNull();
            assertThat(status.getCompletedAt()).isNull();
            assertThat(status.getResult()).isNull();
        }

        @Test
        @DisplayName("PENDING → DONE preserves startedAt and adds result + completedAt")
        void pendingThenDone() {
            registry.markPending(1L);
            RunStatusDto pending = registry.getStatus(1L);

            AnswerDto result = AnswerDto.builder()
                    .id(1L)
                    .testCasesPassed(3)
                    .testCasesTotal(5)
                    .build();
            registry.markDone(1L, result);

            RunStatusDto done = registry.getStatus(1L);
            assertThat(done.getStatus()).isEqualTo("DONE");
            assertThat(done.getResult()).isSameAs(result);
            assertThat(done.getError()).isNull();
            assertThat(done.getStartedAt()).isEqualTo(pending.getStartedAt());
            assertThat(done.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING → FAILED sets error message and clears result")
        void pendingThenFailed() {
            registry.markPending(2L);

            registry.markFailed(2L, "Docker daemon unreachable");

            RunStatusDto failed = registry.getStatus(2L);
            assertThat(failed.getStatus()).isEqualTo("FAILED");
            assertThat(failed.getError()).isEqualTo("Docker daemon unreachable");
            assertThat(failed.getResult()).isNull();
            assertThat(failed.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("markPending replaces an existing job (last-click-wins)")
        void pendingReplacesExisting() {
            registry.markPending(3L);
            registry.markDone(3L, AnswerDto.builder().id(3L).build());

            // New click — should clear DONE and go back to PENDING
            registry.markPending(3L);

            RunStatusDto status = registry.getStatus(3L);
            assertThat(status.getStatus()).isEqualTo("PENDING");
            assertThat(status.getResult()).isNull();
            assertThat(status.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("markDone is no-op when no PENDING entry exists (avoids stale leak)")
        void markDoneWithoutPendingNoOp() {
            // Worker completes after job was evicted — must not resurrect it
            registry.markDone(99L, AnswerDto.builder().id(99L).build());

            assertThat(registry.getStatus(99L).getStatus()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("markFailed is no-op when no PENDING entry exists")
        void markFailedWithoutPendingNoOp() {
            registry.markFailed(99L, "boom");

            assertThat(registry.getStatus(99L).getStatus()).isEqualTo("NONE");
        }
    }

    @Nested
    @DisplayName("Concurrent jobs for different submissions are isolated")
    class IsolationTests {

        @Test
        @DisplayName("Different submission IDs maintain independent state")
        void independentJobs() {
            registry.markPending(10L);
            registry.markPending(20L);

            registry.markDone(10L, AnswerDto.builder().id(10L).testCasesPassed(2).build());
            registry.markFailed(20L, "Compile error");

            assertThat(registry.getStatus(10L).getStatus()).isEqualTo("DONE");
            assertThat(registry.getStatus(10L).getResult().getTestCasesPassed()).isEqualTo(2);

            assertThat(registry.getStatus(20L).getStatus()).isEqualTo("FAILED");
            assertThat(registry.getStatus(20L).getError()).isEqualTo("Compile error");
        }
    }
}
