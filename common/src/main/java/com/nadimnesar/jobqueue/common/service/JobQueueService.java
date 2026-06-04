package com.nadimnesar.jobqueue.common.service;

import com.nadimnesar.jobqueue.common.entity.JobEntity;

import java.util.List;

public interface JobQueueService {
    void publish(JobEntity job);

    String consume();

    void moveToDeadLetter(String jobId);

    List<String> consumeDeadLetters();
}
