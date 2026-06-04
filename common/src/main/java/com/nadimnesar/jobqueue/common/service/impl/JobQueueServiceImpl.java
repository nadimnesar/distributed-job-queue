package com.nadimnesar.jobqueue.common.service.impl;

import com.nadimnesar.jobqueue.common.constant.RabbitMQConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobQueueServiceImpl implements JobQueueService {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueServiceImpl.class);

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(JobEntity job) {
        String jobId = job.getId().toString();
        String routingKey = resolveRoutingKey(job.getPriority());
        logger.info("Publishing job {} to exchange {} with routing key {}", jobId, RabbitMQConstants.EXCHANGE, routingKey);
        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE, routingKey, jobId);
        logger.info("Successfully published job {}", jobId);
    }

    @Override
    public String consume() {
        for (String queue : List.of(RabbitMQConstants.QUEUE_HIGH, RabbitMQConstants.QUEUE_MEDIUM, RabbitMQConstants.QUEUE_LOW)) {
            Message message = rabbitTemplate.receive(queue);
            if (message != null) {
                String jobId = new String(message.getBody());
                logger.info("Consumed job {} from queue {}", jobId, queue);
                return jobId;
            }
        }
        logger.info("No jobs available in any priority queue");
        return null;
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

    private String resolveRoutingKey(JobPriority priority) {
        return switch (priority) {
            case HIGH -> RabbitMQConstants.QUEUE_HIGH;
            case LOW -> RabbitMQConstants.QUEUE_LOW;
            default -> RabbitMQConstants.QUEUE_MEDIUM;
        };
    }
}
