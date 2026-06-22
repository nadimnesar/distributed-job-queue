package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.enums.JobAckStatus;
import com.nadimnesar.jobqueue.common.dto.ConsumedMessage;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobConsumerService {

    private final JobQueueService jobQueueService;
    private final JobProcessorService jobProcessorService;
    private final ExecutorService virtualThreadParTaskExecutor;

    @Scheduled(cron = "${schedule.cron.consume}")
    public void consumeJobs() {
        log.info("Starting job consumption cycle");

        ConsumedMessage consumedMessage = jobQueueService.consume();
        if (consumedMessage == null) {
            return;
        }

        String jobId = consumedMessage.jobId();
        virtualThreadParTaskExecutor.submit(() -> {
            try {
                TracingUtils.setNewSpanId();

                var result = jobProcessorService.processJob(jobId);
                if (JobAckStatus.ACK.equals(result)) {
                    jobQueueService.ack(consumedMessage);
                } else {
                    jobQueueService.nack(consumedMessage);
                }
            } catch (Exception e) {
                log.error("Error processing job {}: {}", jobId, e.getMessage(), e);
                jobQueueService.nack(consumedMessage);
            } finally {
                TracingUtils.clearTracing();
            }
        });
        TracingUtils.clearTracing();
    }
}
