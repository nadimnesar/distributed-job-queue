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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobRetryService {
    private static final Logger logger = LoggerFactory.getLogger(JobRetryService.class);

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

        logger.info("Received retry message for job: {}", jobId);

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            processRetryJob(jobId);
            jobQueueService.ack(channel, deliveryTag, jobId);
        } catch (Exception e) {
            if (isTransientException(e)) {
                // Transient error (DB, RabbitMQ) — don't ack, let RabbitMQ redeliver
                logger.error("Transient error processing retry job {}, will be redelivered: {}", jobId, e.getMessage(), e);
            } else {
                // Permanent error — mark as dead and ack to remove from queue
                logger.error("Permanent error processing retry job {}, marking as DEAD: {}", jobId, e.getMessage(), e);
                try {
                    updateAsDead(jobId);
                } catch (Exception ex) {
                    logger.error("Failed to mark job {} as DEAD during error handling: {}", jobId, ex.getMessage(), ex);
                }
                jobQueueService.ack(channel, deliveryTag, jobId);
            }
        } finally {
            TracingUtils.clearTracing();
        }
    }

    private void processRetryJob(String jobId) {
        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if (optionalJob.isEmpty()) {
            logger.error("Retry job with ID: {} not found in database", jobId);
            return;
        }

        JobEntity job = optionalJob.get();
        if (job.getStatus() == JobStatus.CANCELED) {
            logger.info("Retry job with ID: {} is canceled, skipping", jobId);
            return;
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            logger.info("Retry job with ID: {} already completed, skipping", jobId);
            return;
        }

        if (job.getStatus() == JobStatus.DEAD) {
            logger.info("Retry job with ID: {} is already dead, skipping", jobId);
            return;
        }

        if (!jobDependencyService.getDependencies(job.getId()).isEmpty()) {
            logger.info("Retry job {} still has unmet dependencies, re-publishing without incrementing attempt count",
                    jobId);
            job.setStatus(JobStatus.PENDING);
            job.setStartedAt(null);
            job.setCompletedAt(null);
            job.setResult(null);
            jobRepository.save(job);
            try {
                jobQueueService.publish(job);
                logger.info("Retry job {} re-routed to {} priority queue (waiting for dependencies)",
                        jobId, job.getPriority());
            } catch (Exception e) {
                logger.error("Failed to re-publish retry job {} to queue — job is PENDING in DB but NOT in queue." +
                        " Manual re-enqueue required: {}", jobId, e.getMessage(), e);
            }
            return;
        }

        if (job.getAttemptCount() >= job.getMaxAttemptCount()) {
            logger.warn("Max retry attempts reached for job with ID: {}, marking as DEAD", jobId);
            job.setStatus(JobStatus.DEAD);
            job.setResult("Max retry attempts reached");
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            jobQueueService.moveToDeadLetter(jobId);
            return;
        }

        job.setAttemptCount(job.getAttemptCount() + 1);
        logger.info("Retry job {} attempt count incremented to {}/{}",
                jobId, job.getAttemptCount(), job.getMaxAttemptCount());

        job.setStatus(JobStatus.PENDING);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setResult(null);
        jobRepository.save(job);

        try {
            jobQueueService.publish(job);
            logger.info("Retry job {} re-routed to {} priority queue", jobId, job.getPriority());
        } catch (Exception e) {
            logger.error("Failed to re-publish retry job {} to queue — job is PENDING in DB but NOT in queue." +
                    " Manual re-enqueue required: {}", jobId, e.getMessage(), e);
        }
    }

    private boolean isTransientException(Exception e) {
        return e instanceof DataAccessException || e instanceof AmqpException;
    }

    private void updateAsDead(String jobId) {
        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if (optionalJob.isEmpty()) {
            logger.error("Failed to retry job with ID: {} not found in database", jobId);
            return;
        }

        JobEntity job = optionalJob.get();
        job.setStatus(JobStatus.DEAD);
        job.setResult("Job marked as dead due to processing error");
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
        jobQueueService.moveToDeadLetter(jobId);

        logger.info("Failed to retry job with ID: {}, forwarding to dlq", jobId);
    }
}
