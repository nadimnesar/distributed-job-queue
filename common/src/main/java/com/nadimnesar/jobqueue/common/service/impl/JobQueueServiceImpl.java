package com.nadimnesar.jobqueue.common.service.impl;

import com.nadimnesar.jobqueue.common.constant.RabbitMQConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.dto.ConsumedMessage;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class JobQueueServiceImpl implements JobQueueService {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueServiceImpl.class);
    private static final List<String> orderedQueueList = List.of(
            RabbitMQConstants.QUEUE_HIGH,
            RabbitMQConstants.QUEUE_MEDIUM,
            RabbitMQConstants.QUEUE_LOW);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    @Override
    public void publish(JobEntity job) {
        String jobId = job.getId().toString();
        String routingKey = resolveRoutingKey(job.getPriority());
        logger.info("Publishing job {} to exchange {} with routing key {}", jobId, RabbitMQConstants.EXCHANGE, routingKey);
        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE, routingKey, jobId);
        logger.info("Successfully published job {}", jobId);
    }

    @Override
    public ConsumedMessage consume() {
        for (String queue : orderedQueueList) {
            try {
                ConsumedMessage result = rabbitTemplate.execute(channel -> {
                    GetResponse getResponse = channel.basicGet(queue, false);
                    if (getResponse != null) {
                        String jobId = new String(getResponse.getBody());
                        logger.info("Consumed job {} from queue {}", jobId, queue);
                        return new ConsumedMessage(getResponse, channel, jobId, queue);
                    }
                    return null;
                });

                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                logger.error("Failed to consume from queue {}: {}", queue, e.getMessage(), e);
                return null;
            }
        }

        logger.info("No jobs available in any priority queue");
        return null;
    }

    @Override
    public void ack(Channel channel, long deliveryTag, String jobId) {
        try (channel) {
            channel.basicAck(deliveryTag, false);
            logger.info("Acknowledged job {}", jobId);
        } catch (IOException | TimeoutException e) {
            logger.error("Failed to ack for job {}: {}", jobId, e.getMessage(), e);
        }
    }

    @Override
    public void ack(ConsumedMessage message) {
        String jobId = message.jobId();

        try (Channel channel = message.channel()) {
            channel.basicAck(message.getDeliveryTag(), false);
            logger.info("Acknowledged message {}", message);
        } catch (IOException | TimeoutException e) {
            logger.error("Failed to ack for job {}: {}", jobId, e.getMessage(), e);
        }
    }

    @Override
    public void nack(ConsumedMessage message) {
        String jobId = message.jobId();

        try (Channel channel = message.channel()) {
            channel.basicNack(message.getDeliveryTag(), false, false);
            logger.info("Nacked job {}", jobId);
        } catch (IOException | TimeoutException e) {
            logger.error("Failed to nack for job {}: {}", jobId, e.getMessage(), e);
        }
    }

    @Override
    public void moveToDeadLetter(String jobId) {
        logger.info("Moving job {} to dead letter queue", jobId);
        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE, RabbitMQConstants.QUEUE_DLQ, jobId);
        logger.info("Successfully moved job {} to dead letter queue", jobId);
    }

    @Override
    public List<String> consumeDeadLetters() {
        logger.info("Attempting to consume dead letter jobs");
        List<String> jobIds = new ArrayList<>();
        while (true) {
            Message message = rabbitTemplate.receive(RabbitMQConstants.QUEUE_DLQ);
            if (message == null) break;
            jobIds.add(new String(message.getBody()));
        }
        if (!jobIds.isEmpty()) {
            logger.info("Consumed {} dead letter jobs: {}", jobIds.size(), jobIds);
        } else {
            logger.info("No dead letter jobs available");
        }
        return jobIds;
    }

    @Override
    public long getQueueMessageCount(String queueName) {
        Properties props = rabbitAdmin.getQueueProperties(queueName);
        if (props == null) return 0L;
        return (Long) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
    }

    private String resolveRoutingKey(JobPriority priority) {
        return switch (priority) {
            case HIGH -> RabbitMQConstants.QUEUE_HIGH;
            case LOW -> RabbitMQConstants.QUEUE_LOW;
            default -> RabbitMQConstants.QUEUE_MEDIUM;
        };
    }
}
