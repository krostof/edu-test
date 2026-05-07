package com.edutest.codeexecution.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async executor for student "Run tests" preview operations.
 *
 * Bounded queue prevents Docker daemon overload — if too many concurrent runs,
 * caller-runs policy makes the request thread itself execute (which throttles
 * incoming requests rather than dropping them silently).
 *
 * Pool size matches typical Docker daemon concurrency limits.
 */
@Configuration
@EnableAsync
public class AsyncCodeExecutionConfig {

    @Bean(name = "codeRunExecutor")
    public Executor codeRunExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("code-run-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(60);
        exec.initialize();
        return exec;
    }
}
