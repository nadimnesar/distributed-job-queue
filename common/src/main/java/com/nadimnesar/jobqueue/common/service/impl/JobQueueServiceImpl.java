package com.nadimnesar.jobqueue.common.service.impl;

import com.nadimnesar.jobqueue.common.constant.RedisConstant;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.common.service.RedisQueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobQueueServiceImpl implements JobQueueService {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueServiceImpl.class);

    private final RedisQueueService redisQueueService;

    @Override
    public void enqueueJob(JobEntity job) {
        var jobId = job.getId().toString();
        logger.info("RedisJobQueueServiceImpl|Enqueueing job with ID: {}", jobId);

        String queueKey = getQueueKeyByPriority(job.getPriority());
        redisQueueService.enqueue(queueKey, jobId);

        logger.info("RedisJobQueueServiceImpl|Successfully enqueued job with ID: {}", jobId);
    }

    @Override
    public String dequeueJob() {
        logger.info("RedisJobQueueServiceImpl|Attempting to dequeue job");

        String jobId = redisQueueService.dequeue(RedisConstant.HIGH_PRIORITY_JOB_QUEUE_KEY);

        if (jobId == null) {
            jobId = redisQueueService.dequeue(RedisConstant.MEDIUM_PRIORITY_JOB_QUEUE_KEY);
        }

        if (jobId == null) {
            jobId = redisQueueService.dequeue(RedisConstant.LOW_PRIORITY_JOB_QUEUE_KEY);
        }

        if (jobId != null) {

            // This job is locked till 3 minutes
            boolean lockStatus = redisQueueService.acquireLock(jobId, 180000);

            if (!lockStatus) {
                logger.info("JobQueueServiceImpl|Job {} is already being processed by another worker", jobId);
                return null;
            }

            logger.info("JobQueueServiceImpl|Successfully dequeued job with ID: {}", jobId);
        } else {
            logger.info("JobQueueServiceImpl|No jobs available to dequeue");
        }

        return jobId;
    }

    @Override
    public void moveToDeadLetterQueue(String jobId) {
        logger.info("JobQueueServiceImpl|Moving job {} to dead letter queue", jobId);
        redisQueueService.enqueue(RedisConstant.DEAD_LETTER_QUEUE_KEY, jobId);
    }

    private String getQueueKeyByPriority(JobPriority priority) {
        return switch (priority) {
            case HIGH -> RedisConstant.HIGH_PRIORITY_JOB_QUEUE_KEY;
            case LOW -> RedisConstant.LOW_PRIORITY_JOB_QUEUE_KEY;
            case MEDIUM -> RedisConstant.MEDIUM_PRIORITY_JOB_QUEUE_KEY;
        };
    }
}
