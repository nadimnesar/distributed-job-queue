package com.nadimnesar.jobqueue.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {

    @Bean
    public ExecutorService virtualThreadParTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
