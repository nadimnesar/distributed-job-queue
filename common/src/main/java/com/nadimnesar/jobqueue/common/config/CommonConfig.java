package com.nadimnesar.jobqueue.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories("com.nadimnesar.jobqueue.common.repository")
@EntityScan("com.nadimnesar.jobqueue.common.entity")
@EnableJpaAuditing
public class CommonConfig {
}
