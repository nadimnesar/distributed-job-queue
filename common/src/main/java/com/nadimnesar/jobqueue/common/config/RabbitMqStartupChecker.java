package com.nadimnesar.jobqueue.common.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rabbitmq.startup-check", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class RabbitMqStartupChecker implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqStartupChecker.class);

    private final ConnectionFactory connectionFactory;
    private final RabbitProperties rabbitProperties;
    private final Environment environment;

    public RabbitMqStartupChecker(ConnectionFactory connectionFactory,
                                  RabbitProperties rabbitProperties,
                                  Environment environment) {
        this.connectionFactory = connectionFactory;
        this.rabbitProperties = rabbitProperties;
        this.environment = environment;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        String appName = environment.getProperty("spring.application.name", "application");
        String host = rabbitProperties.getHost();
        int port = rabbitProperties.getPort();
        String virtualHost = rabbitProperties.getVirtualHost();

        logger.info("Checking RabbitMQ connection for {} at {}:{} (vhost={})", appName, host, port, virtualHost);

        Connection connection = connectionFactory.createConnection();
        connection.close();

        logger.info("RabbitMQ connection established for {} at {}:{} (vhost={})", appName, host, port, virtualHost);
    }
}
