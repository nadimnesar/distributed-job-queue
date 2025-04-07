package com.nadimnesar.jobqueue.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication(scanBasePackages = "com.nadimnesar.jobqueue")
public class ProducerApplication {
    public static void main(String[] args) {
        // Set a unique instance id for the producer
        System.setProperty("instance_id", UUID.randomUUID().toString().substring(0, 12));

        SpringApplication.run(ProducerApplication.class, args);
    }
}
