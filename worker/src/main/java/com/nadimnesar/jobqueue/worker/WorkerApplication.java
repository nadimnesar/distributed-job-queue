package com.nadimnesar.jobqueue.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.UUID;

@SpringBootApplication(scanBasePackages = "com.nadimnesar.jobqueue")
@EnableScheduling
public class WorkerApplication {
    public static void main(String[] args) {
        // Set a unique instance id for the worker
        System.setProperty("instance-id", UUID.randomUUID().toString().substring(0, 12));

        SpringApplication.run(WorkerApplication.class, args);
    }
}
