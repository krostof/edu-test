package com.edutest.codeexecution.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "coding.execution")
public class CodeExecutionProperties {

    private boolean enabled = true;
    private long globalTimeoutMs = 60_000L;
    private long defaultTimeMs = 5_000L;
    private int defaultMemoryMb = 256;
    private int outputLimitChars = 2_000;
    /**
     * Read-only rootfs hardening for sandbox containers. Production keeps this true.
     *
     * Set to false locally only when running against Docker Desktop on Windows/WSL2,
     * where {@code copyArchiveToContainer} into a tmpfs target on a readonly-rootfs
     * container fails with "container rootfs is marked read-only" — a Docker Desktop
     * limitation that does not occur on real Linux Docker daemons (CI / prod).
     */
    private boolean readonlyRootfs = true;

    private Docker docker = new Docker();

    @Data
    public static class Docker {
        private String host = "npipe:////./pipe/docker_engine";
        private List<String> imagesPreload = List.of(
                "python:3.12-alpine",
                "node:20-alpine",
                "eclipse-temurin:21-jdk-alpine",
                "gcc:13"
        );
    }
}
