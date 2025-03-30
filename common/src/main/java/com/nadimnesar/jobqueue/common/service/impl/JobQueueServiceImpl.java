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

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobQueueServiceImpl implements JobQueueService {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueServiceImpl.class);

    private final RedisQueueService redisQueueService;

    @Override
    public void enqueueJob(JobEntity job) {
        var jobId = job.getId().toString();
        logger.info("Enqueueing job with ID: {}", jobId);

        String queueKey = getQueueKeyByPriority(job.getPriority());
        redisQueueService.enqueue(queueKey, jobId);

        logger.info("Successfully enqueued job with ID: {}", jobId);
    }

    @Override
    public String dequeueJob() {
        logger.info("Attempting to dequeue job");

        String jobId = redisQueueService.dequeue(RedisConstant.HIGH_PRIORITY_JOB_QUEUE_KEY);

        if (jobId == null) {
            jobId = redisQueueService.dequeue(RedisConstant.MEDIUM_PRIORITY_JOB_QUEUE_KEY);
        }

        if (jobId == null) {
            jobId = redisQueueService.dequeue(RedisConstant.LOW_PRIORITY_JOB_QUEUE_KEY);
        }

        if (jobId == null) {
            logger.info("No jobs found in the queue");
            return null;
        }

        logger.info("Successfully dequeued job with ID: {}", jobId);
        return jobId;
    }

    @Override
    public void moveToDeadLetterQueue(String jobId) {
        logger.info("Moving job {} to dead letter queue", jobId);
        redisQueueService.enqueue(RedisConstant.DEAD_LETTER_QUEUE_KEY, jobId);
    }

    @Override
    public List<String> dequeueDeadLetterJobs() {
        logger.info("Attempting to dequeue dead letter jobs");

        List<String> jobIds = redisQueueService.dequeueAll(RedisConstant.DEAD_LETTER_QUEUE_KEY);

        if (!jobIds.isEmpty()) {
            logger.info("JobQueueServiceImpl|Successfully dequeued {} dead letter jobs", jobIds.size());
            return jobIds;
        }

        logger.info("JobQueueServiceImpl|No dead letter jobs available to dequeue");
        return List.of();
    }

    private String getQueueKeyByPriority(JobPriority priority) {
        return switch (priority) {
            case HIGH -> RedisConstant.HIGH_PRIORITY_JOB_QUEUE_KEY;
            case LOW -> RedisConstant.LOW_PRIORITY_JOB_QUEUE_KEY;
            case MEDIUM -> RedisConstant.MEDIUM_PRIORITY_JOB_QUEUE_KEY;
        };
    }
}
