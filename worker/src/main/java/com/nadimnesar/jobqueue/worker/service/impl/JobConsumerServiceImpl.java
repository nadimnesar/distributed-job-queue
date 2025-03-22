package com.nadimnesar.jobqueue.worker.service.impl;

import com.nadimnesar.jobqueue.common.constant.RedisConstant;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.common.service.RedisQueueService;
import com.nadimnesar.jobqueue.worker.service.JobConsumerService;
import com.nadimnesar.jobqueue.worker.service.JobProcessorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class JobConsumerServiceImpl implements JobConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(JobConsumerServiceImpl.class);

    private final RedisQueueService redisQueueService;
    private final JobQueueService jobQueueService;
    private final JobProcessorService jobProcessorService;
    private final ExecutorService virtualThreadParTaskExecutor;

    @Scheduled(fixedDelay = 10000) // Poll every 10 second
    public void consumeJobs() {
        logger.info("JobConsumerService|Starting job consumption cycle");

        String jobId = jobQueueService.dequeueJob();
        if (jobId == null) {
            logger.info("JobConsumerService|No jobs found in the queue");
            return;
        }

        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
            try {
                jobProcessorService.processJob(jobId);
            } catch (Exception e) {
                logger.error("JobConsumerService|Error processing job {}: {}", jobId, e.getMessage(), e);
            } finally {
                redisQueueService.releaseLock(RedisConstant.JOB_PROCESSING_STATE + jobId);
            }
        }, virtualThreadParTaskExecutor).exceptionally(throwable -> {
            logger.error("JobConsumerService|Error processing job {}: {}", jobId, throwable.getMessage(), throwable);
            return null;
        });
    }
}
