package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.Constants;
import com.nadimnesar.jobqueue.common.constant.RabbitMQConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobDependencyRepository;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobRetryService {
    private static final Logger logger = LoggerFactory.getLogger(JobRetryService.class);

    private final JobQueueService jobQueueService;
    private final JobRepository jobRepository;
    private final JobDependencyRepository jobDependencyRepository;
    private final JobDependencyService jobDependencyService;

    @RabbitListener(
            queues = RabbitMQConstants.QUEUE_RETRY,
            ackMode = "MANUAL",
            concurrency = "1"
    )
    public void onRetryMessage(String jobId, Channel channel, Message message) {
        String traceId = (String) message.getMessageProperties().getHeaders().get(Constants.HEADER_TRACE_ID);
        TracingUtils.setTraceId(traceId);
        TracingUtils.setSpanId(TracingUtils.newSpanId());

        logger.info("Received retry message for job: {}", jobId);

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            processRetryJob(jobId);
            jobQueueService.ack(channel, deliveryTag, jobId);
        } catch (Exception e) {
            logger.error("Failed to process retry job {}: {}", jobId, e.getMessage(), e);
            updateAsDead(jobId);
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

        job.setAttemptCount(job.getAttemptCount() + 1);
        logger.info("Retry job {} attempt count incremented to {}/{}", jobId, job.getAttemptCount(), job.getMaxAttemptCount());

        if (job.getAttemptCount() > job.getMaxAttemptCount()) {
            logger.warn("Max retry attempts reached for job with ID: {}, marking as DEAD", jobId);
            job.setStatus(JobStatus.DEAD);
            job.setResult("Max retry attempts reached");
            jobRepository.save(job);
            jobQueueService.moveToDeadLetter(jobId);
            return;
        }

        job.setStatus(JobStatus.PENDING);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setResult(null);
        jobRepository.save(job);

        removeOrphanDependencies();

        jobQueueService.publish(job);
        logger.info("Retry job {} re-routed to {} priority queue", jobId, job.getPriority());
    }

    private void removeOrphanDependencies() {
        logger.info("Removing orphan dependencies before retry job");

        var dependencies = jobDependencyRepository.findAll();
        dependencies.forEach(jobDependency -> {
            var dependentJob = jobRepository.findById(jobDependency.getDependencyId());
            if (dependentJob.isPresent() && dependentJob.get().getStatus().equals(JobStatus.COMPLETED)) {
                jobDependencyService.informDependents(dependentJob.get().getId());
            }
        });
    }

    private void updateAsDead(String jobId) {
        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if (optionalJob.isEmpty()) {
            logger.error("Failed to retry job with ID: {} not found in database", jobId);
            return;
        }

        JobEntity job = optionalJob.get();
        job.setStatus(JobStatus.DEAD);
        jobRepository.save(job);
        jobQueueService.moveToDeadLetter(jobId);

        logger.info("Failed to retry job with ID: {}, forwarding to dlq", jobId);
    }
}
