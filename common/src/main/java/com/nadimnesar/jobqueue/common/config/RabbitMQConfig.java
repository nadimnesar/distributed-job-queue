package com.nadimnesar.jobqueue.common.config;

import com.nadimnesar.jobqueue.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public DirectExchange jobExchange() {
        return new DirectExchange(RabbitMQConstants.EXCHANGE);
    }

    @Bean
    public Queue highQueue() {
        return buildQueue(RabbitMQConstants.QUEUE_HIGH);
    }

    @Bean
    public Queue mediumQueue() {
        return buildQueue(RabbitMQConstants.QUEUE_MEDIUM);
    }

    @Bean
    public Queue lowQueue() {
        return buildQueue(RabbitMQConstants.QUEUE_LOW);
    }

    private Queue buildQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-queue-type", RabbitMQConstants.QUEUE_TYPE_QUORUM)
                .withArgument("x-quorum-initial-group-size", RabbitMQConstants.QUORUM_INITIAL_GROUP_SIZE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.QUEUE_DELAY)
                .withArgument("x-dead-letter-strategy", RabbitMQConstants.DEAD_LETTER_STRATEGY_AT_LEAST_ONCE)
                .withArgument("x-overflow", RabbitMQConstants.OVERFLOW_REJECT_PUBLISH)
                .build();
    }

    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_DELAY)
                .withArgument("x-queue-type", RabbitMQConstants.QUEUE_TYPE_QUORUM)
                .withArgument("x-quorum-initial-group-size", RabbitMQConstants.QUORUM_INITIAL_GROUP_SIZE)
                .withArgument("x-message-ttl", RabbitMQConstants.TTL)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.QUEUE_RETRY)
                .withArgument("x-dead-letter-strategy", RabbitMQConstants.DEAD_LETTER_STRATEGY_AT_LEAST_ONCE)
                .withArgument("x-overflow", RabbitMQConstants.OVERFLOW_REJECT_PUBLISH)
                .build();
    }

    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_RETRY)
                .withArgument("x-queue-type", RabbitMQConstants.QUEUE_TYPE_QUORUM)
                .withArgument("x-quorum-initial-group-size", RabbitMQConstants.QUORUM_INITIAL_GROUP_SIZE)
                .withArgument("x-overflow", RabbitMQConstants.OVERFLOW_REJECT_PUBLISH)
                .build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_DLQ)
                .withArgument("x-queue-type", RabbitMQConstants.QUEUE_TYPE_QUORUM)
                .withArgument("x-quorum-initial-group-size", RabbitMQConstants.QUORUM_INITIAL_GROUP_SIZE)
                .withArgument("x-overflow", RabbitMQConstants.OVERFLOW_REJECT_PUBLISH)
                .build();
    }

    @Bean
    public Binding highBinding() {
        return BindingBuilder.bind(highQueue()).to(jobExchange()).with(RabbitMQConstants.QUEUE_HIGH);
    }

    @Bean
    public Binding mediumBinding() {
        return BindingBuilder.bind(mediumQueue()).to(jobExchange()).with(RabbitMQConstants.QUEUE_MEDIUM);
    }

    @Bean
    public Binding lowBinding() {
        return BindingBuilder.bind(lowQueue()).to(jobExchange()).with(RabbitMQConstants.QUEUE_LOW);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(jobExchange()).with(RabbitMQConstants.QUEUE_DELAY);
    }

    @Bean
    public Binding retryBinding() {
        return BindingBuilder.bind(retryQueue()).to(jobExchange()).with(RabbitMQConstants.QUEUE_RETRY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue()).to(jobExchange()).with(RabbitMQConstants.QUEUE_DLQ);
    }
}
