package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.constant.RabbitMQConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRetryService {
    private final JobQueueService jobQueueService;
    private final JobRepository jobRepository;
    private final JobDependencyService jobDependencyService;

    @RabbitListener(
            queues = RabbitMQConstants.QUEUE_RETRY,
            ackMode = "MANUAL",
            concurrency = "1"
    )
    public void onRetryMessage(String jobId, Channel channel, Message message) {
        String traceId = (String) message.getMessageProperties().getHeaders().get(AppConstants.HEADER_TRACE_ID);
        TracingUtils.setTraceId(traceId);
        TracingUtils.setNewSpanId();

        log.info("Received retry message for job: {}", jobId);

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            processRetryJob(jobId);
            jobQueueService.ack(channel, deliveryTag, jobId);
        } catch (Exception e) {
            if (isTransientException(e)) {
                log.error("Transient error processing retry job {}, will be redelivered: {}", jobId, e.getMessage(), e);
            } else {
                log.error("Permanent error processing retry job {}, marking as DEAD: {}", jobId, e.getMessage(), e);
                try {
                    updateAsDead(jobId);
                    jobQueueService.ack(channel, deliveryTag, jobId);
                } catch (Exception ex) {
                    log.error("Failed to mark job {} as DEAD during error handling: {}", jobId, ex.getMessage(), ex);
                    jobQueueService.nack(channel, deliveryTag, jobId, true);
                }
            }
        } finally {
            TracingUtils.clearTracing();
        }
    }

    private void processRetryJob(String jobId) {
        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if (optionalJob.isEmpty()) {
            log.error("Retry job with ID: {} not found in database", jobId);
            return;
        }

        JobEntity job = optionalJob.get();
        if (job.getStatus() == JobStatus.CANCELED) {
            log.info("Retry job with ID: {} is canceled, skipping", jobId);
            return;
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            log.info("Retry job with ID: {} already completed, skipping", jobId);
            return;
        }

        if (job.getStatus() == JobStatus.DEAD) {
            log.info("Retry job with ID: {} is already dead, skipping", jobId);
            return;
        }

        int cleanedCount = jobDependencyService.cleanupStaleDependencies(job.getId());
        log.info("Cleaned up {} stale dependencies for retry job {}", cleanedCount, jobId);

        if (!jobDependencyService.getDependencies(job.getId()).isEmpty()) {
            log.info("Retry job {} still has unmet dependencies, re-publishing without incrementing attempt count",
                    jobId);
            rerouteToPriorityQueue(job, " (waiting for dependencies)");
            return;
        }

        if (job.getAttemptCount() >= job.getMaxAttemptCount()) {
            log.warn("Max retry attempts reached for job with ID: {}, marking as DEAD", jobId);
            job.setStatus(JobStatus.DEAD);
            job.setResult("Max retry attempts reached");
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            jobQueueService.moveToDeadLetter(jobId);
            return;
        }

        job.setAttemptCount(job.getAttemptCount() + 1);
        log.info("Retry job {} attempt count incremented to {}/{}",
                jobId, job.getAttemptCount(), job.getMaxAttemptCount());

        rerouteToPriorityQueue(job, "");
    }

    private void rerouteToPriorityQueue(JobEntity job, String logSuffix) {
        job.setStatus(JobStatus.PENDING);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setResult(null);
        jobRepository.save(job);

        try {
            jobQueueService.publish(job);
            log.info("Retry job {} re-routed to {} priority queue{}", job.getId(), job.getPriority(), logSuffix);
        } catch (Exception e) {
            log.error("Failed to re-publish retry job {} to queue — job is PENDING in DB but NOT in queue." +
                    " Manual re-enqueue required: {}", job.getId(), e.getMessage(), e);
        }
    }

    private boolean isTransientException(Exception e) {
        return e instanceof DataAccessException || e instanceof AmqpException;
    }

    private void updateAsDead(String jobId) {
        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if (optionalJob.isEmpty()) {
            log.error("Failed to retry job with ID: {} not found in database", jobId);
            return;
        }

        JobEntity job = optionalJob.get();
        job.setStatus(JobStatus.DEAD);
        job.setResult("Job marked as dead due to processing error");
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
        jobQueueService.moveToDeadLetter(jobId);

        log.info("Failed to retry job with ID: {}, forwarding to dlq", jobId);
    }
}
