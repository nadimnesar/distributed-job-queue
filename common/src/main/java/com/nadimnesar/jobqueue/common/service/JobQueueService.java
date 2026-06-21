package com.nadimnesar.jobqueue.common.service;

import com.nadimnesar.jobqueue.common.dto.ConsumedMessage;
import com.nadimnesar.jobqueue.common.entity.JobEntity;

import java.util.List;

public interface JobQueueService {

    void publish(JobEntity job);

    ConsumedMessage consume();

    void ack(ConsumedMessage message);

    void nack(ConsumedMessage message);

    void moveToDeadLetter(String jobId);

    List<String> consumeDeadLetters();

    long getQueueMessageCount(String queueName);
}
