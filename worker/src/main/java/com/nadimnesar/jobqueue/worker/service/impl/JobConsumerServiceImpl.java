package com.nadimnesar.jobqueue.worker.service.impl;

import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.worker.service.JobConsumerService;
import com.nadimnesar.jobqueue.worker.service.JobProcessorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class JobConsumerServiceImpl implements JobConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(JobConsumerServiceImpl.class);

    private final JobQueueService jobQueueService;
    private final JobProcessorService jobProcessorService;
    private final ExecutorService virtualThreadParTaskExecutor;

    @Override
    @Scheduled(cron = "${schedule.cron.consume}")
    public void consumeJobs() {
        logger.info("Starting job consumption cycle, at: {}", LocalDateTime.now());

        String jobId = jobQueueService.dequeueJob();
        if (jobId == null) {
            return;
        }

        virtualThreadParTaskExecutor.submit(() -> {
            try {
                jobProcessorService.processJob(jobId);
            } catch (Exception e) {
                logger.error("Error processing job {}: {}", jobId, e.getMessage(), e);
            }
        });
    }
}
