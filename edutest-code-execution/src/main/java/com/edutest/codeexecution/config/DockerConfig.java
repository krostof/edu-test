package com.edutest.codeexecution.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(CodeExecutionProperties.class)
@RequiredArgsConstructor
public class DockerConfig {

    private final CodeExecutionProperties properties;

    @Bean(destroyMethod = "close")
    public DockerClient dockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(properties.getDocker().getHost())
                .build();

        // Zerodep transport handles Windows npipe paths natively; the httpclient5 transport
        // mis-parses npipe URLs as TCP localhost:2375, which fails on Docker Desktop with
        // "Connection refused" even when the daemon is running normally over the named pipe.
        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(20)
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient client = DockerClientImpl.getInstance(config, httpClient);
        try {
            client.pingCmd().exec();
            log.info("Docker daemon reachable at {}", properties.getDocker().getHost());
        } catch (Exception e) {
            log.warn("Docker daemon not reachable at {} — coding submissions will fail with SYSTEM_ERROR. Cause: {}",
                    properties.getDocker().getHost(), e.getMessage());
        }
        return client;
    }
}
