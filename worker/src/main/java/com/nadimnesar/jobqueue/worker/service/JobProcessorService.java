package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.dto.JobProcessResult;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.worker.handler.JobHandler;
import com.nadimnesar.jobqueue.worker.handler.JobHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(JobProcessorService.class);

    private final JobDependencyService jobDependencyService;
    private final JobHandlerRegistry jobHandlerRegistry;
    private final JobRepository jobRepository;

    public JobProcessResult processJob(String jobId) {
        logger.info("Processing job with id: {}", jobId);

        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if (optionalJob.isEmpty()) {
            logger.error("Job with ID: {} not found", jobId);
            return JobProcessResult.ACK;
        }

        var job = optionalJob.get();
        switch (job.getStatus()) {
            case JobStatus.CANCELED:
                logger.info("Job with ID: {} already canceled, no needs to process it", jobId);
                return JobProcessResult.ACK;
            case JobStatus.COMPLETED:
                logger.info("Job with ID: {} already completed", jobId);
                return JobProcessResult.ACK;
            default:
                break;
        }

        if (!jobDependencyService.getDependencies(job.getId()).isEmpty()) {
            logger.info("Job with ID: {} has dependencies, will retry after delay", jobId);
            return JobProcessResult.NACK;
        }

        if (checkCanceled(job.getId())) {
            return JobProcessResult.ACK;
        }

        JobHandler handler = jobHandlerRegistry.getHandler(job.getType());
        if (handler == null) {
            logger.error("No handler registered for job type: {}, jobId={}", job.getType(), jobId);
            handleFailedJob(job, "Unknown job type: " + job.getType());
            return JobProcessResult.NACK;
        }

        updateJobAsProcessing(job);

        try {
            handler.handle(job);
            if (!checkCanceled(job.getId())) {
                handleCompletedJob(job);
            }
            return JobProcessResult.ACK;
        } catch (Exception e) {
            logger.error("Handler failed for job id={}, type={}: {}", job.getId(), job.getType(), e.getMessage(), e);
            handleFailedJob(job, e.getMessage());
            return JobProcessResult.NACK;
        }
    }

    private void updateJobAsProcessing(JobEntity job) {
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    private void handleFailedJob(JobEntity job, String reason) {
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(null);

        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }
        job.setResult(reason != null ? reason : "Unknown error");
        jobRepository.save(job);
    }

    private void handleCompletedJob(JobEntity job) {
        job.setStatus(JobStatus.COMPLETED);
        job.setResult("Job completed successfully");

        if (job.getCompletedAt() == null) {
            job.setCompletedAt(LocalDateTime.now());
        }

        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }

        jobDependencyService.informDependents(job.getId());
        jobRepository.save(job);
    }

    private boolean checkCanceled(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(j -> j.getStatus() == JobStatus.CANCELED)
                .orElse(false);
    }
}
