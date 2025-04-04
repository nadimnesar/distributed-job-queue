package com.nadimnesar.jobqueue.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication(scanBasePackages = "com.nadimnesar.jobqueue")
@EnableScheduling
public class WorkerApplication {
    public static void main(String[] args) throws UnknownHostException {
        // Set the hostname system property to the local host name
        System.setProperty("hostname", InetAddress.getLocalHost().getHostName());

        SpringApplication.run(WorkerApplication.class, args);
    }
}
