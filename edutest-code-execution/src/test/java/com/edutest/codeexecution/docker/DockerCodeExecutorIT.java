package com.edutest.codeexecution.docker;

import com.edutest.codeexecution.ExecutionReport;
import com.edutest.codeexecution.config.CodeExecutionProperties;
import com.edutest.codeexecution.runners.CSharpRunner;
import com.edutest.codeexecution.runners.JavaRunner;
import com.edutest.codeexecution.runners.JavascriptRunner;
import com.edutest.codeexecution.runners.LanguageRunnerRegistry;
import com.edutest.codeexecution.runners.PythonRunner;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.code.CompilationStatusEnum;
import com.edutest.persistance.entity.code.ExecutionStatusEnum;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DockerCodeExecutor} — runs real containers.
 *
 * <p>Disabled by default. Enable with {@code -Ddocker.it=true} on a machine
 * with a reachable Docker daemon. First run will pull {@code python:3.12-alpine}
 * (~50 MB), {@code eclipse-temurin:21-jdk-alpine} (~200 MB) and {@code mono:6.12}
 * (~620 MB; slim variant lacks {@code mcs}) — can take several minutes on a cold
 * cache.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "docker.it", matches = "true")
class DockerCodeExecutorIT {

    private DockerClient dockerClient;
    private DockerCodeExecutor executor;

    @BeforeAll
    void setUp() throws InterruptedException {
        String host = System.getProperty("docker.host",
                System.getProperty("os.name").toLowerCase().contains("win")
                        ? "npipe:////./pipe/docker_engine"
                        : "unix:///var/run/docker.sock");

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .build();
        // Zerodep transport handles Windows npipe paths natively; httpclient5 mis-parses them
        // as TCP and tries to connect to npipe://localhost:2375.
        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(20)
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofSeconds(60))
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
        dockerClient.pingCmd().exec();

        ensureImage("python:3.12-alpine");
        ensureImage("eclipse-temurin:21-jdk-alpine");
        ensureImage("mono:6.12");

        CodeExecutionProperties properties = new CodeExecutionProperties();
        properties.setEnabled(true);
        properties.setGlobalTimeoutMs(60_000L);
        properties.setDefaultTimeMs(5_000L);
        properties.setDefaultMemoryMb(128);
        properties.setOutputLimitChars(2_000);
        // Disable readonly rootfs for the IT — required to make copyArchiveToContainer work
        // against Docker Desktop on Windows/WSL2 (silent failure / "rootfs read-only" otherwise).
        // Workspace stays on tmpfs so containers still can't persist anything cross-run.
        properties.setReadonlyRootfs(false);

        LanguageRunnerRegistry registry = new LanguageRunnerRegistry(List.of(
                new PythonRunner(),
                new JavascriptRunner(),
                new JavaRunner(),
                new CSharpRunner()
        ));

        executor = new DockerCodeExecutor(dockerClient, registry, properties);
    }

    @AfterAll
    void tearDown() throws Exception {
        if (dockerClient != null) {
            dockerClient.close();
        }
    }

    @Test
    @DisplayName("Python: doubles input, all test cases pass")
    void pythonHappyPath() {
        String code = "print(int(input()) * 2)";
        List<TestCaseEntity> cases = List.of(
                testCase(1L, "5", "10"),
                testCase(2L, "7", "14"),
                testCase(3L, "0", "0"));

        ExecutionReport report = executor.execute(code, "python", cases, 5_000, 128);

        assertThat(report.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.SUCCESS);
        assertThat(report.getTestCaseResults())
                .hasSize(3)
                .allSatisfy(r -> assertThat(r.isPassed()).isTrue());
    }

    @Test
    @DisplayName("Python: wrong output marks test cases as failed but keeps SUCCESS execution status")
    void pythonWrongOutput() {
        String code = "print(int(input()) + 1)";
        List<TestCaseEntity> cases = List.of(
                testCase(1L, "5", "10"),
                testCase(2L, "7", "14"));

        ExecutionReport report = executor.execute(code, "python", cases, 5_000, 128);

        assertThat(report.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.SUCCESS);
        assertThat(report.getTestCaseResults())
                .extracting(r -> r.isPassed())
                .containsExactly(false, false);
        assertThat(report.getTestCaseResults().get(0).getActualOutput().trim()).isEqualTo("6");
    }

    @Test
    @DisplayName("Python: runtime error surfaces RUNTIME_ERROR with stderr captured")
    void pythonRuntimeError() {
        String code = "raise RuntimeError('boom')";
        List<TestCaseEntity> cases = List.of(testCase(1L, "", ""));

        ExecutionReport report = executor.execute(code, "python", cases, 5_000, 128);

        assertThat(report.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.RUNTIME_ERROR);
        assertThat(report.getTestCaseResults()).hasSize(1);
        assertThat(report.getTestCaseResults().get(0).getErrorMessage())
                .contains("boom");
        assertThat(report.getTestCaseResults().get(0).isPassed()).isFalse();
    }

    @Test
    @DisplayName("Python: infinite loop kills container after time limit")
    void pythonTimeout() {
        String code = "while True: pass";
        List<TestCaseEntity> cases = List.of(testCase(1L, "", "anything"));

        long start = System.currentTimeMillis();
        ExecutionReport report = executor.execute(code, "python", cases, 800, 128);
        long duration = System.currentTimeMillis() - start;

        assertThat(report.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.TIME_LIMIT_EXCEEDED);
        assertThat(report.getTestCaseResults().get(0).isTimedOut()).isTrue();
        assertThat(duration).isLessThan(15_000L);
    }

    @Test
    @DisplayName("Java: javac compiles Solution.java to /tmp, java runs the class")
    void javaHappyPath() {
        // Class name must be `Solution` to match JavaRunner.sourceFilename = "Solution.java"
        // (Java requires the public class to share its file's name).
        String code = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int n = sc.nextInt();
                        System.out.println(n * 2);
                    }
                }
                """;
        List<TestCaseEntity> cases = List.of(
                testCase(1L, "5", "10"),
                testCase(2L, "7", "14"),
                testCase(3L, "0", "0"));

        // JVM cold-start under Docker is heavier than Python — give it 10s per case and
        // bump the memory headroom (-Xmx in the runner caps the heap to memoryLimitMb).
        ExecutionReport report = executor.execute(code, "java", cases, 10_000, 384);

        assertThat(report.getCompilationStatus()).isEqualTo(CompilationStatusEnum.SUCCESS);
        assertThat(report.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.SUCCESS);
        assertThat(report.getTestCaseResults())
                .hasSize(3)
                .allSatisfy(r -> assertThat(r.isPassed()).isTrue());
    }

    @Test
    @DisplayName("C#: Console.WriteLine compiles via mcs and runs under Mono")
    void csharpHappyPath() {
        // Class name must differ from method name (CS0542 otherwise).
        String code = """
                using System;
                class Program {
                    static void Main(string[] args) {
                        int n = int.Parse(Console.ReadLine());
                        Console.WriteLine(n * 2);
                    }
                }
                """;
        List<TestCaseEntity> cases = List.of(
                testCase(1L, "5", "10"),
                testCase(2L, "7", "14"),
                testCase(3L, "0", "0"));

        // Mono cold-start under Docker can be slow on the first run — give it more headroom
        // than Python (which uses ~50 MB alpine) without changing global limits.
        ExecutionReport report = executor.execute(code, "csharp", cases, 10_000, 256);

        assertThat(report.getCompilationStatus()).isEqualTo(CompilationStatusEnum.SUCCESS);
        assertThat(report.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.SUCCESS);
        assertThat(report.getTestCaseResults())
                .hasSize(3)
                .allSatisfy(r -> assertThat(r.isPassed()).isTrue());
    }

    @Test
    @DisplayName("Unsupported language returns SYSTEM_ERROR without spinning up a container")
    void unsupportedLanguage() {
        ExecutionReport report = executor.execute("fn main() {}", "rust", List.of(), 5_000, 128);

        assertThat(report.getExecutionStatus()).isEqualTo(ExecutionStatusEnum.SYSTEM_ERROR);
        assertThat(report.getCompilationError()).contains("rust");
    }

    private static TestCaseEntity testCase(Long id, String input, String expected) {
        TestCaseEntity tc = new TestCaseEntity();
        tc.setId(id);
        tc.setInputData(input);
        tc.setExpectedOutput(expected);
        tc.setIsPublic(true);
        tc.setWeight(1);
        return tc;
    }

    private void ensureImage(String image) throws InterruptedException {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException e) {
            dockerClient.pullImageCmd(image)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion(5, TimeUnit.MINUTES);
        }
    }
}
