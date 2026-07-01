package com.nadimnesar.jobqueue.common.service;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.constant.RabbitMQConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.dto.ConsumedDlqMessage;
import com.nadimnesar.jobqueue.common.dto.ConsumedMessage;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobQueueService {

    private static final List<String> orderedQueueList = List.of(
            RabbitMQConstants.QUEUE_HIGH,
            RabbitMQConstants.QUEUE_MEDIUM,
            RabbitMQConstants.QUEUE_LOW);

    private static final MessagePostProcessor setTraceIdByMessagePostProcessor = new MessagePostProcessor() {
        @Override
        @NonNull
        public Message postProcessMessage(@NonNull Message message) {
            String traceId = TracingUtils.getTraceId();
            if (traceId != null) {
                message.getMessageProperties()
                        .setHeader(AppConstants.HEADER_TRACE_ID, traceId);
            }
            return message;
        }
    };

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    private final ConnectionFactory connectionFactory;

    private final ConcurrentHashMap<String, String> traceIdMap = new ConcurrentHashMap<>();

    @PostConstruct
    void initPublisherCallbacks() {
        rabbitTemplate.setMandatory(true);

        rabbitTemplate.setConfirmCallback(this::handlePublisherConfirm);
        rabbitTemplate.setReturnsCallback(this::handleReturnedMessage);

        connectionFactory.addConnectionListener(new ConnectionListener() {
            @Override
            public void onCreate(Connection connection) {
            }

            @Override
            public void onClose(@NonNull Connection connection) {
                traceIdMap.clear();
            }

            @Override
            public void onShutDown(@NonNull ShutdownSignalException signal) {
                traceIdMap.clear();
            }
        });
    }

    private void handlePublisherConfirm(CorrelationData correlationData,
                                        boolean ack,
                                        String cause) {
        String jobId = extractJobId(correlationData);
        String traceId = traceIdMap.remove(jobId);

        try {
            TracingUtils.setTraceId(traceId);
            TracingUtils.setNewSpanId();

            if (ack) {
                log.debug("Message published successfully to broker. jobId={}", jobId);
                return;
            }

            log.error("Broker NACK received. jobId={}, cause={}", jobId, cause);
        } finally {
            TracingUtils.clearTracing();
        }
    }

    private void handleReturnedMessage(ReturnedMessage returned) {
        String body = new String(returned.getMessage().getBody(), StandardCharsets.UTF_8);
        String traceId = extractTraceIdFromHeaders(
                returned.getMessage().getMessageProperties().getHeaders());

        try {
            TracingUtils.setTraceId(traceId);
            TracingUtils.setNewSpanId();

            log.error(
                    "Unroutable message detected. exchange={}, routingKey={}, replyCode={}, replyText={}, body={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    body
            );
        } finally {
            TracingUtils.clearTracing();
        }
    }

    private String extractJobId(CorrelationData correlationData) {
        if (correlationData == null) {
            return "unknown";
        }
        return correlationData.getId();
    }

    public void publish(JobEntity job) {
        String jobId = job.getId().toString();
        String routingKey = resolveRoutingKey(job.getPriority());
        String traceId = TracingUtils.getTraceId();
        if (traceId != null) {
            traceIdMap.put(jobId, traceId);
        }

        log.info("Publishing job {} to exchange {} with routing key {}", jobId, RabbitMQConstants.EXCHANGE, routingKey);

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE, routingKey, jobId, setTraceIdByMessagePostProcessor,
                new CorrelationData(jobId));

        log.info("Successfully published job {}", jobId);
    }

    public ConsumedMessage consume() {
        Channel channel = null;
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            channel = connection.createChannel(false);
            for (String queue : orderedQueueList) {
                Map<String, String> savedMdc = MDC.getCopyOfContextMap();
                try {
                    GetResponse getResponse = channel.basicGet(queue, false);
                    if (getResponse == null) {
                        continue;
                    }
                    String jobId = new String(getResponse.getBody(), StandardCharsets.UTF_8);
                    String traceId = extractTraceIdFromHeaders(getResponse.getProps().getHeaders());
                    TracingUtils.setTraceId(traceId);
                    TracingUtils.setNewSpanId();

                    log.info("Consumed job {} from queue {}", jobId, queue);
                    ConsumedMessage message = ConsumedMessage.builder()
                            .getResponse(getResponse)
                            .channel(channel)
                            .connection(connection)
                            .jobId(jobId)
                            .queue(queue)
                            .traceId(traceId)
                            .build();
                    channel = null;
                    connection = null;
                    return message;
                } catch (Exception e) {
                    log.error("Failed to consume from queue {}: {}", queue, e.getMessage(), e);
                    restoreMdc(savedMdc);
                }
            }

            log.info("No jobs available in any priority queue");
            return null;
        } catch (Exception e) {
            log.error("Failed to open channel or consume: {}", e.getMessage(), e);
            return null;
        } finally {
            closeChannel(channel);
            closeConnection(connection);
        }
    }

    //No need to close the channel, because it used by rabbitmq listener.
    public void ack(Channel channel, long deliveryTag, String jobId) {
        try {
            channel.basicAck(deliveryTag, false);
            log.info("Acknowledged job {}", jobId);
        } catch (IOException e) {
            log.error("Failed to ack for job {}: {}", jobId, e.getMessage(), e);
        }
    }

    public void nack(Channel channel, long deliveryTag, String jobId, boolean requeue) {
        try {
            channel.basicNack(deliveryTag, false, requeue);
            log.info("Nacked job {} (requeue={})", jobId, requeue);
        } catch (IOException e) {
            log.error("Failed to nack for job {}: {}", jobId, e.getMessage(), e);
        }
    }

    public void ack(ConsumedMessage message) {
        String jobId = message.jobId();
        Channel channel = message.channel();
        Connection connection = message.connection();

        try {
            channel.basicAck(message.getDeliveryTag(), false);
            log.info("Acknowledged message {}", message);
        } catch (IOException e) {
            log.error("Failed to ack for job {}: {}", jobId, e.getMessage(), e);
        } finally {
            closeChannel(channel);
            closeConnection(connection);
        }
    }

    public void nack(ConsumedMessage message) {
        String jobId = message.jobId();
        Channel channel = message.channel();
        Connection connection = message.connection();

        try {
            channel.basicNack(message.getDeliveryTag(), false, false);
            log.info("Nacked job {}", jobId);
        } catch (IOException e) {
            log.error("Failed to nack for job {}: {}", jobId, e.getMessage(), e);
        } finally {
            closeChannel(channel);
            closeConnection(connection);
        }
    }

    public void moveToDeadLetter(String jobId) {
        log.info("Moving job {} to dead letter queue", jobId);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.QUEUE_DLQ,
                jobId,
                setTraceIdByMessagePostProcessor
        );
        log.info("Successfully moved job {} to dead letter queue", jobId);
    }

    public List<ConsumedDlqMessage> consumeDeadLetters() {
        log.info("Attempting to consume dead letter jobs");
        List<ConsumedDlqMessage> messages = new ArrayList<>();
        while (true) {
            Message message = rabbitTemplate.receive(RabbitMQConstants.QUEUE_DLQ);
            if (message == null) break;
            String jobId = new String(message.getBody(), StandardCharsets.UTF_8);
            String traceId = extractTraceIdFromHeaders(message.getMessageProperties().getHeaders());
            messages.add(new ConsumedDlqMessage(jobId, traceId));
        }
        if (!messages.isEmpty()) {
            log.info("Consumed {} dead letter jobs: {}", messages.size(), messages);
        } else {
            log.info("No dead letter jobs available");
        }
        return messages;
    }

    public long getQueueMessageCount(String queueName) {
        Properties props = rabbitAdmin.getQueueProperties(queueName);
        if (props == null) return 0L;
        Long count = (Long) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        return count != null ? count : 0L;
    }

    private String resolveRoutingKey(JobPriority priority) {
        return switch (priority) {
            case HIGH -> RabbitMQConstants.QUEUE_HIGH;
            case LOW -> RabbitMQConstants.QUEUE_LOW;
            default -> RabbitMQConstants.QUEUE_MEDIUM;
        };
    }

    private static String extractTraceIdFromHeaders(Map<String, Object> headers) {
        if (headers == null) return null;
        Object traceId = headers.get(AppConstants.HEADER_TRACE_ID);
        return traceId != null ? traceId.toString() : null;
    }

    public static void restoreMdc(Map<String, String> savedMdc) {
        if (savedMdc == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(savedMdc);
        }
    }

    private void closeChannel(Channel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.warn("Failed to close channel: {}", e.getMessage());
            }
        }
    }

    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("Failed to close connection: {}", e.getMessage());
            }
        }
    }
}
