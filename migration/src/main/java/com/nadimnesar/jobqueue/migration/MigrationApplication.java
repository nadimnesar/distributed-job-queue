package com.nadimnesar.jobqueue.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MigrationApplication {
    static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MigrationApplication.class, args);
        SpringApplication.exit(context, () -> 0);
        System.exit(0);
    }
}
