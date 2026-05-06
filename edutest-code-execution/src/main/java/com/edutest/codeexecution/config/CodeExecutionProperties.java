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

    private Docker docker = new Docker();

    @Data
    public static class Docker {
        private String host = "npipe:////./pipe/docker_engine";
        private List<String> imagesPreload = List.of(
                "python:3.12-alpine",
                "node:20-alpine",
                "openjdk:21-slim",
                "gcc:13"
        );
    }
}
