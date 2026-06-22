package com.nadimnesar.jobqueue.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record AppRabbitProperties(@DefaultValue StartupCheck startupCheck) {

    public record StartupCheck(@DefaultValue("true") boolean enabled) {
    }
}
