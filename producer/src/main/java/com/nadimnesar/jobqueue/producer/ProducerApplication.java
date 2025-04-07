package com.nadimnesar.jobqueue.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication(scanBasePackages = "com.nadimnesar.jobqueue")
public class ProducerApplication {
    public static void main(String[] args) throws UnknownHostException {
        // Set hostname as a system property to be used in the application
        System.setProperty("hostname", InetAddress.getLocalHost().getHostName());

        SpringApplication.run(ProducerApplication.class, args);
    }
}
