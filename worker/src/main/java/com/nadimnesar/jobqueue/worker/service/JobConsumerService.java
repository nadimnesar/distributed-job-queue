package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.dto.ConsumedMessage;
import com.nadimnesar.jobqueue.common.dto.JobProcessResult;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(JobConsumerService.class);

    private final JobQueueService jobQueueService;
    private final JobProcessorService jobProcessorService;

    @Scheduled(cron = "${schedule.cron.consume}")
    public void consumeJobs() {
        logger.info("Starting job consumption cycle");

        ConsumedMessage consumedMessage = jobQueueService.consume();
        if (consumedMessage == null) {
            return;
        }

        String jobId = consumedMessage.jobId();
        try {
            var result = jobProcessorService.processJob(jobId);

            if (JobProcessResult.ACK.equals(result)) {
                jobQueueService.ack(consumedMessage);
            } else {
                jobQueueService.nack(consumedMessage);
            }
        } catch (Exception e) {
            logger.error("Error processing job {}: {}", jobId, e.getMessage(), e);
            jobQueueService.nack(consumedMessage);
        } finally {
            TracingUtils.clearTracing();
        }
    }
}
