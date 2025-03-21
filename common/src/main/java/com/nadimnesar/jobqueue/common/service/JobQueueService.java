package com.nadimnesar.jobqueue.common.service;

public interface JobQueueService {
    void enqueueJob(String jobId);
    String dequeueJob();
    void moveToDeadLetterQueue(String jobId);
}
