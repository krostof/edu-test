package com.edutest.codeexecution.docker;

import com.edutest.codeexecution.ExecutionReport;
import com.edutest.codeexecution.TestCaseRunResult;
import com.edutest.codeexecution.config.CodeExecutionProperties;
import com.edutest.codeexecution.runners.LanguageRunner;
import com.edutest.codeexecution.runners.LanguageRunnerRegistry;
import com.edutest.codeexecution.runners.RunCommand;
import com.edutest.codeexecution.runners.UnsupportedLanguageException;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.code.CompilationStatusEnum;
import com.edutest.persistance.entity.code.ExecutionStatusEnum;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerCodeExecutor {

    private static final String CONTAINER_LABEL_KEY = "edutest";
    private static final String CONTAINER_LABEL_VALUE = "code-submission";
    private static final String WORKSPACE_DIR = "/workspace";

    private final DockerClient dockerClient;
    private final LanguageRunnerRegistry runnerRegistry;
    private final CodeExecutionProperties properties;

    public ExecutionReport execute(String sourceCode,
                                   String language,
                                   List<TestCaseEntity> testCases,
                                   Integer timeLimitMs,
                                   Integer memoryLimitMb) {

        if (!properties.isEnabled()) {
            return ExecutionReport.systemError("Code execution is disabled by configuration");
        }

        long perTestTimeout = timeLimitMs != null && timeLimitMs > 0
                ? timeLimitMs : properties.getDefaultTimeMs();
        int memMb = memoryLimitMb != null && memoryLimitMb > 0
                ? memoryLimitMb : properties.getDefaultMemoryMb();

        LanguageRunner runner;
        try {
            runner = runnerRegistry.resolve(language);
        } catch (UnsupportedLanguageException e) {
            return ExecutionReport.systemError(e.getMessage());
        }

        RunCommand runCommand = runner.buildRunCommand(memMb);
        String containerId = null;

        try {
            containerId = createContainer(runCommand, memMb);
            dockerClient.startContainerCmd(containerId).exec();
            writeSourceFile(containerId, runCommand.getSourceFilename(), sourceCode);

            if (runCommand.requiresCompilation()) {
                ExecResult compile = execInContainer(
                        containerId, runCommand.getCompileCmd(), null, properties.getGlobalTimeoutMs());
                if (compile.timedOut) {
                    return ExecutionReport.builder()
                            .compilationStatus(CompilationStatusEnum.TIMEOUT)
                            .compilationError("Compilation timed out")
                            .executionStatus(ExecutionStatusEnum.NOT_EXECUTED)
                            .testCaseResults(List.of())
                            .maxExecutionTimeMs(0L)
                            .maxMemoryUsedMb(0)
                            .build();
                }
                if (compile.exitCode != 0) {
                    String err = truncate(compile.stderr.isEmpty() ? compile.stdout : compile.stderr,
                            properties.getOutputLimitChars());
                    return ExecutionReport.compilationFailed(err);
                }
            }

            return runTestCases(containerId, runCommand, testCases, perTestTimeout);

        } catch (Exception e) {
            log.error("Docker execution failed", e);
            return ExecutionReport.systemError("Docker error: " + e.getMessage());
        } finally {
            if (containerId != null) {
                removeContainer(containerId);
            }
        }
    }

    private String createContainer(RunCommand runCommand, int memoryLimitMb) {
        long memBytes = memoryLimitMb * 1024L * 1024L;
        Map<String, String> tmpfs = new HashMap<>();
        tmpfs.put("/tmp", "rw,size=64m,exec");
        tmpfs.put(WORKSPACE_DIR, "rw,size=32m,exec");

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(memBytes)
                .withMemorySwap(memBytes)
                .withCpuCount(1L)
                .withPidsLimit(64L)
                .withNetworkMode("none")
                .withReadonlyRootfs(properties.isReadonlyRootfs())
                .withTmpFs(tmpfs)
                .withAutoRemove(false);

        CreateContainerResponse response = dockerClient.createContainerCmd(runCommand.getImage())
                .withHostConfig(hostConfig)
                .withWorkingDir(WORKSPACE_DIR)
                .withUser("nobody")
                .withLabels(Map.of(CONTAINER_LABEL_KEY, CONTAINER_LABEL_VALUE))
                .withName("edutest-exec-" + UUID.randomUUID())
                .withCmd("sleep", String.valueOf(Math.max(60, properties.getGlobalTimeoutMs() / 1000)))
                .withTty(false)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        return response.getId();
    }

    private void writeSourceFile(String containerId, String filename, String sourceCode) throws Exception {
        // Why base64 + shell arg, not exec stdin or copyArchiveToContainerCmd:
        //  - copyArchiveToContainerCmd silently no-ops on Docker Desktop / WSL2 when the target
        //    is a tmpfs mount (file never appears inside the container).
        //  - exec with stdin pipe hangs: docker-java does not signal EOF on the input stream,
        //    so `cat > target` blocks until the global timeout (`Potok został zakończony`).
        //  - base64 chars are URL-safe shell-safe ([A-Za-z0-9+/=]), so embedding them inside
        //    single quotes is unambiguous and avoids any escaping minefield.
        //
        // Run as root because Docker mounts tmpfs at /workspace with default 0755 owned by
        // root; the container's default user (`nobody`) cannot write there. Compile/run still
        // execute as nobody — student code itself stays unprivileged.
        String target = WORKSPACE_DIR + "/" + filename;
        String b64 = Base64.getEncoder().encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));
        ExecResult result = execInContainer(
                containerId,
                new String[]{"sh", "-c", "echo '" + b64 + "' | base64 -d > " + target},
                null,
                properties.getGlobalTimeoutMs(),
                "root");
        if (result.exitCode != 0) {
            throw new RuntimeException(
                    "Failed to write source file (exit " + result.exitCode + "): " + result.stderr);
        }
    }

    private ExecutionReport runTestCases(String containerId,
                                         RunCommand runCommand,
                                         List<TestCaseEntity> testCases,
                                         long perTestTimeoutMs) {
        List<TestCaseRunResult> results = new ArrayList<>();
        long maxExecMs = 0L;
        int maxMemMb = 0;
        ExecutionStatusEnum overall = ExecutionStatusEnum.SUCCESS;

        for (TestCaseEntity tc : testCases) {
            ExecResult run = execInContainer(containerId, runCommand.getRunCmd(), tc.getInputData(), perTestTimeoutMs);

            boolean timedOut = run.timedOut;
            boolean oom = run.oomKilled;
            String stdout = run.stdout;
            String stderr = run.stderr;
            int exit = run.exitCode;
            long execMs = run.durationMs;

            String actualOutput = truncate(stdout, properties.getOutputLimitChars());
            String errorMessage = null;
            boolean passed = false;

            if (timedOut) {
                errorMessage = "Time limit exceeded (" + perTestTimeoutMs + " ms)";
                if (overall == ExecutionStatusEnum.SUCCESS) overall = ExecutionStatusEnum.TIME_LIMIT_EXCEEDED;
            } else if (oom) {
                errorMessage = "Memory limit exceeded";
                if (overall == ExecutionStatusEnum.SUCCESS) overall = ExecutionStatusEnum.MEMORY_LIMIT_EXCEEDED;
            } else if (exit != 0) {
                errorMessage = truncate(stderr.isEmpty() ? "Exit code " + exit : stderr,
                        properties.getOutputLimitChars());
                if (overall == ExecutionStatusEnum.SUCCESS) overall = ExecutionStatusEnum.RUNTIME_ERROR;
            } else {
                passed = matches(tc.getExpectedOutput(), stdout);
            }

            results.add(TestCaseRunResult.builder()
                    .testCaseId(tc.getId())
                    .passed(passed)
                    .actualOutput(actualOutput)
                    .errorMessage(errorMessage)
                    .executionTimeMs(execMs)
                    .memoryUsedMb(0)
                    .timedOut(timedOut)
                    .outOfMemory(oom)
                    .build());

            if (execMs > maxExecMs) maxExecMs = execMs;
        }

        return ExecutionReport.builder()
                .compilationStatus(runCommand.requiresCompilation()
                        ? CompilationStatusEnum.SUCCESS : CompilationStatusEnum.NOT_COMPILED)
                .executionStatus(overall)
                .testCaseResults(results)
                .maxExecutionTimeMs(maxExecMs)
                .maxMemoryUsedMb(maxMemMb)
                .build();
    }

    private ExecResult execInContainer(String containerId, String[] cmd, String stdin, long timeoutMs) {
        return execInContainer(containerId, cmd, stdin, timeoutMs, null);
    }

    private ExecResult execInContainer(String containerId, String[] cmd, String stdin, long timeoutMs,
                                       String userOverride) {
        // docker-java's stdin pipe (withStdIn) doesn't close on stream exhaustion, so any
        // process that calls input()/read() blocks forever waiting for EOF. Wrap the command
        // in a shell pipe instead — the in-container shell closes the pipe properly when
        // base64 -d finishes, giving the runner a clean EOF.
        String[] effectiveCmd = cmd;
        if (stdin != null) {
            String b64 = Base64.getEncoder().encodeToString(stdin.getBytes(StandardCharsets.UTF_8));
            String shellCmd = "echo '" + b64 + "' | base64 -d | " + joinForShell(cmd);
            effectiveCmd = new String[]{"sh", "-c", shellCmd};
        }

        var execCmd = dockerClient.execCreateCmd(containerId)
                .withCmd(effectiveCmd)
                .withAttachStdout(true)
                .withAttachStderr(true);
        if (userOverride != null) {
            execCmd = execCmd.withUser(userOverride);
        }
        ExecCreateCmdResponse exec = execCmd.exec();

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        long start = System.currentTimeMillis();
        boolean finished;
        boolean timedOut = false;
        boolean oom = false;

        ExecStartResultCallback callback = new ExecStartResultCallback(stdout, stderr) {
            @Override
            public void onNext(Frame frame) {
                try {
                    if (frame.getStreamType() == StreamType.STDERR) {
                        stderr.write(frame.getPayload());
                    } else {
                        stdout.write(frame.getPayload());
                    }
                } catch (Exception ignore) {
                }
            }
        };

        try {
            dockerClient.execStartCmd(exec.getId())
                    .exec(callback);

            finished = callback.awaitCompletion(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                timedOut = true;
                // Closing the callback closes the response stream; the daemon then SIGKILLs
                // the exec process. We deliberately do NOT kill the container itself —
                // subsequent test cases need it to keep running.
                try {
                    callback.close();
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            return new ExecResult(stdout.toString(StandardCharsets.UTF_8),
                    stderr.toString(StandardCharsets.UTF_8),
                    -1, System.currentTimeMillis() - start, false, false);
        }

        long duration = System.currentTimeMillis() - start;
        Long exitCode = null;

        if (!timedOut) {
            try {
                exitCode = dockerClient.inspectExecCmd(exec.getId()).exec().getExitCodeLong();
            } catch (Exception ignore) {
            }
        }

        try {
            InspectContainerResponse.ContainerState state =
                    dockerClient.inspectContainerCmd(containerId).exec().getState();
            if (Boolean.TRUE.equals(state.getOOMKilled())) {
                oom = true;
            }
        } catch (Exception ignore) {
        }

        return new ExecResult(
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8),
                exitCode != null ? exitCode.intValue() : -1,
                duration, timedOut, oom);
    }

    private void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (NotFoundException ignore) {
        } catch (Exception e) {
            log.warn("Failed to remove container {}: {}", containerId, e.getMessage());
        }
    }

    private static boolean matches(String expected, String actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        return expected.trim().equals(actual.trim());
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** POSIX-shell-quote each arg and join with spaces — preserves whitespace inside args. */
    private static String joinForShell(String[] cmd) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cmd.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append('\'').append(cmd[i].replace("'", "'\\''")).append('\'');
        }
        return sb.toString();
    }

    private record ExecResult(String stdout, String stderr, int exitCode, long durationMs,
                              boolean timedOut, boolean oomKilled) {
    }
}
