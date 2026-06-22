package com.nadimnesar.jobqueue.common.config;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@EnableConfigurationProperties(AppRabbitProperties.class)
public class RabbitMQStartupChecker implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQStartupChecker.class);

    private final ConnectionFactory connectionFactory;
    private final RabbitProperties rabbitProperties;
    private final AppRabbitProperties appRabbitProperties;
    private final Environment environment;

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

        logger.info("Checking RabbitMQ connection for {} at {}:{} (vhost={})",
                appName, host, port, virtualHost);

        Exception lastError = null;

        for (int attempt = 1; attempt <= AppConstants.STARTUP_CHECK_MAX_ATTEMPTS; attempt++) {
            try (Connection _ = connectionFactory.createConnection()) {

                logger.info("RabbitMQ startup check attempt {}/{} for {} at {}:{} (vhost={})",
                        attempt, AppConstants.STARTUP_CHECK_MAX_ATTEMPTS, appName, host, port, virtualHost);

                logger.info("RabbitMQ connection established for {} at {}:{} (vhost={})",
                        appName, host, port, virtualHost);

                return;

            } catch (Exception e) {
                lastError = e;

                logger.warn("RabbitMQ startup check attempt {}/{} failed: {}",
                        attempt, AppConstants.STARTUP_CHECK_MAX_ATTEMPTS, e.getMessage());

                if (attempt < AppConstants.STARTUP_CHECK_MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(AppConstants.STARTUP_CHECK_RETRY_SLEEP_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("RabbitMQ startup check interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException(
                "RabbitMQ startup check failed after " + AppConstants.STARTUP_CHECK_MAX_ATTEMPTS + " attempts",
                lastError
        );
    }
}
