package com.nadimnesar.jobqueue.producer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootTest
class ProducerApplicationTests {

    @BeforeAll
    static void setUp() throws UnknownHostException {
        System.setProperty("hostname", InetAddress.getLocalHost().getHostName());
    }

    @Test
    void contextLoads() {
    }
}
