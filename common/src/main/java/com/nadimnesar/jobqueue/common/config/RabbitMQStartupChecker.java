package com.nadimnesar.jobqueue.common.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(AppRabbitProperties.class)
public class RabbitMQStartupChecker implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQStartupChecker.class);

    private final ConnectionFactory connectionFactory;
    private final RabbitProperties rabbitProperties;
    private final AppRabbitProperties appRabbitProperties;
    private final Environment environment;

    public RabbitMQStartupChecker(ConnectionFactory connectionFactory,
                                  RabbitProperties rabbitProperties,
                                  AppRabbitProperties appRabbitProperties,
                                  Environment environment) {
        this.connectionFactory = connectionFactory;
        this.rabbitProperties = rabbitProperties;
        this.appRabbitProperties = appRabbitProperties;
        this.environment = environment;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!appRabbitProperties.startupCheck().enabled()) {
            logger.info("RabbitMQ startup check disabled, skipping");
            return;
        }

        String appName = environment.getProperty("spring.application.name", "application");
        String host = rabbitProperties.getHost();
        Integer portObj = rabbitProperties.getPort();
        int port = portObj != null ? portObj : 5672;
        String virtualHost = rabbitProperties.getVirtualHost();

        logger.info("Checking RabbitMQ connection for {} at {}:{} (vhost={})", appName, host, port, virtualHost);

        Connection connection = connectionFactory.createConnection();
        connection.close();

        logger.info("RabbitMQ connection established for {} at {}:{} (vhost={})", appName, host, port, virtualHost);
    }
}
