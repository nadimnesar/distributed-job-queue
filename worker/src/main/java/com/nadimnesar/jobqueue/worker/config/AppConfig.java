package com.nadimnesar.jobqueue.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {
    @Bean
    public ExecutorService virtualThreadParTaskExecutor() {
        return new MdcAwareExecutorService(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public WorkerContext workerContext() {
        return new WorkerContext(System.getProperty("hostname"));
    }

    public record WorkerContext(String hostname) {
    }
}
