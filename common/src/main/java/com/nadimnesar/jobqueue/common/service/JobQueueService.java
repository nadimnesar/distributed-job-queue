package com.nadimnesar.jobqueue.common.service;

import com.nadimnesar.jobqueue.common.entity.JobEntity;

import java.util.List;

public interface JobQueueService {
    void enqueueJob(JobEntity job);

    String dequeueJob();

    void moveToDeadLetterQueue(String jobId);

    List<String> dequeueDeadLetterJobs();
}
