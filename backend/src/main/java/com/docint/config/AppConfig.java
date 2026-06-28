package com.docint.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AppConfig {

    @Value("${app.ingestion.async-thread-pool-size:5}")
    private int ingestionThreadPoolSize;

    @Bean(name = "ingestionExecutor")
    public Executor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(ingestionThreadPoolSize);
        executor.setMaxPoolSize(ingestionThreadPoolSize * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ingestion-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        // Backpressure: when the pool + queue (maxPool + queueCapacity) are full, run the task on the
        // CALLING (Tomcat) thread instead of rejecting it. Under a burst of uploads this naturally
        // throttles intake — uploads slow down but none are dropped, and no document is left orphaned
        // in PROCESSING. Without this, the default AbortPolicy throws TaskRejectedException past
        // maxPoolSize+queueCapacity in-flight tasks, 500-ing the upload after the row was already saved.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "syncExecutor")
    public Executor syncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
