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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
                .withReadonlyRootfs(true)
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
        byte[] tarBytes = TarUtil.singleFileTar(filename, sourceCode.getBytes(StandardCharsets.UTF_8));
        try (ByteArrayInputStream in = new ByteArrayInputStream(tarBytes)) {
            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withRemotePath(WORKSPACE_DIR)
                    .withTarInputStream(in)
                    .exec();
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
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                .withCmd(cmd)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withAttachStdin(stdin != null)
                .exec();

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
            ByteArrayInputStream stdinStream = stdin != null
                    ? new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8))
                    : null;

            dockerClient.execStartCmd(exec.getId())
                    .withStdIn(stdinStream)
                    .exec(callback);

            finished = callback.awaitCompletion(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                timedOut = true;
                try {
                    callback.close();
                } catch (Exception ignore) {
                }
                try {
                    dockerClient.killContainerCmd(containerId).exec();
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

    private record ExecResult(String stdout, String stderr, int exitCode, long durationMs,
                              boolean timedOut, boolean oomKilled) {
    }
}
