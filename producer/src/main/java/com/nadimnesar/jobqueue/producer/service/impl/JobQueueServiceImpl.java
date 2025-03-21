package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.constant.RedisConstant;
import com.nadimnesar.jobqueue.producer.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobQueueServiceImpl implements JobQueueService {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueServiceImpl.class);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void enqueueJob(String jobId) {
        logger.info("RedisJobQueueServiceImpl|Enqueueing job with ID: {}", jobId);
        redisTemplate.opsForList().rightPush(RedisConstant.JOB_QUEUE_KEY, jobId);
        logger.info("RedisJobQueueServiceImpl|Successfully enqueued job with ID: {}", jobId);
    }
}
