package com.edutest.codeexecution.docker;

import com.edutest.codeexecution.config.CodeExecutionProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerImagePreloader {

    private final DockerClient dockerClient;
    private final CodeExecutionProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!properties.isEnabled()) {
            return;
        }
        Thread t = new Thread(this::preload, "edutest-docker-preloader");
        t.setDaemon(true);
        t.start();
    }

    private void preload() {
        for (String image : properties.getDocker().getImagesPreload()) {
            try {
                dockerClient.inspectImageCmd(image).exec();
                log.debug("Image {} already present", image);
            } catch (NotFoundException notFound) {
                log.info("Pulling image {} ...", image);
                try {
                    dockerClient.pullImageCmd(image)
                            .exec(new PullImageResultCallback())
                            .awaitCompletion(10, TimeUnit.MINUTES);
                    log.info("Image {} pulled", image);
                } catch (Exception e) {
                    log.warn("Failed to pull image {}: {}", image, e.getMessage());
                }
            } catch (Exception e) {
                log.warn("Image inspection failed for {} (Docker daemon unreachable?): {}", image, e.getMessage());
                return;
            }
        }
    }
}
