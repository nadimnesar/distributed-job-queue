package com.nadimnesar.jobqueue.producer.service;

public interface JobQueueService {
    void enqueueJob(String jobId);
}
