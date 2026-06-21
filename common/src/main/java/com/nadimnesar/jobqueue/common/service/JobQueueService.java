package com.nadimnesar.jobqueue.common.service;

import com.nadimnesar.jobqueue.common.dto.ConsumedMessage;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.rabbitmq.client.Channel;

import java.util.List;

public interface JobQueueService {

    void publish(JobEntity job);

    ConsumedMessage consume();

    void ack(Channel channel, long deliveryTag, String jobId);

    void ack(ConsumedMessage consumedMessage);

    void nack(ConsumedMessage consumedMessage);

    void moveToDeadLetter(String jobId);

    List<String> consumeDeadLetters();

    long getQueueMessageCount(String queueName);
}
