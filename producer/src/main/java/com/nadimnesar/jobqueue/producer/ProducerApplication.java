package com.nadimnesar.jobqueue.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication(scanBasePackages = "com.nadimnesar.jobqueue")
public class ProducerApplication {
    public static void main(String[] args) throws UnknownHostException {
        // Set the hostname system property to the local host name
        System.setProperty("hostname", InetAddress.getLocalHost().getHostName());

        // Start the Spring Boot application
        SpringApplication.run(ProducerApplication.class, args);
    }
}
