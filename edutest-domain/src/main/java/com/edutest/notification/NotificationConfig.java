package com.edutest.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Thread pool for fire-and-forget notification work (emails, future push/SMS).
 *
 * Separate from code execution executor — different SLA: notifications can be slow
 * and tolerate burstiness, code execution must be tight to keep "Run tests" responsive.
 *
 * {@code @EnableAsync} is also activated by AsyncCodeExecutionConfig in the
 * code-execution module; declaring it here keeps the domain module self-sufficient
 * for tests/standalone usage. Spring tolerates duplicate {@code @EnableAsync} declarations.
 */
@Configuration
@EnableAsync
public class NotificationConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(4);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("notify-");
        // Drop oldest if the queue overflows — losing a notification is preferable to OOM
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.initialize();
        return exec;
    }
}
