// src/main/java/com/adp/benefits/carrier/config/VirtualThreadConfig.java
package com.adp.benefits.carrier.config.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Configuration
public class VirtualThreadConfig {

    /**
     * ExecutorService that spins up one virtual thread per task. Use for both CPU-light
     * orchestration and blocking work.
     */
    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("vt-worker-", 1).factory());
    }

    /** Scheduler for all subscribeOn / publishOn: pure virtual threads. */
    @Bean
    public Scheduler workerScheduler(ExecutorService virtualThreadExecutor) {
        return Schedulers.fromExecutorService(virtualThreadExecutor);
    }

    /**
     * A ScheduledExecutorService on virtual threads, wrapped as a Scheduler, so Reactor delays
     * (timeouts, retries, etc.) also happen on virtual threads.
     */
    @Bean
    public Scheduler timerScheduler() {
        ScheduledExecutorService virtualTimer =
                Executors.newScheduledThreadPool(
                        Runtime.getRuntime().availableProcessors(),
                        Thread.ofVirtual().name("vt-timer-", 1).factory());
        return Schedulers.fromExecutorService(virtualTimer);
    }
}
