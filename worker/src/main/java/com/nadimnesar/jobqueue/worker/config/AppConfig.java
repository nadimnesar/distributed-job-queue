package com.nadimnesar.jobqueue.worker.config;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public ExecutorService virtualThreadParTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public WorkerContext workerContext() {
        return new WorkerContext(UuidCreator.getTimeOrderedEpoch());
    }

    public record WorkerContext(UUID workerId) {
    }
}
