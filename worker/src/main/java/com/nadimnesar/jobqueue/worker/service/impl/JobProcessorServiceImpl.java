package com.nadimnesar.jobqueue.worker.service.impl;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.worker.service.JobProcessorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class JobProcessorServiceImpl implements JobProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(JobProcessorServiceImpl.class);

    private final JobQueueService jobQueueService;
    private final JobRepository jobRepository;
    private final JobDependencyService jobDependencyService;

    @Override
    public void processJob(String jobId) {
        logger.info("Processing job with id: {}", jobId);

        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if (optionalJob.isEmpty()) {
            logger.error("Job with ID: {} not found", jobId);
            return;
        }

        var job = optionalJob.get();

        switch (job.getStatus()) {
            case JobStatus.CANCELED:
                logger.info("Job with ID: {} already canceled, no needs to process it", jobId);
                return;
            case JobStatus.COMPLETED:
                logger.info("Job with ID: {} already completed", jobId);
                return;
            default:
                break;
        }

        if (!jobDependencyService.getDependencies(job.getId()).isEmpty()) {
            logger.info("Job with ID: {} has dependencies, waiting for them to complete", jobId);
            jobQueueService.enqueueJob(job);
            return;
        }

        if (job.getCurrentRetryAttemptCount() >= job.getMaxRetryAttemptCount() && !checkCanceled(job.getId())) {
            logger.warn("Max retry attempts reached for job with ID: {}", jobId);
            handleFailedJob(job, "Max retry attempts reached");
            jobQueueService.moveToDeadLetterQueue(jobId);
            return;
        }

        if (!checkCanceled(job.getId())) {
            updateJobAsProcessing(job);
        }

        try {
            AtomicBoolean cancellationFlag = new AtomicBoolean(false);

            switch (job.getType()) {
                case JobType.EMAIL_SENDING:
                    processEmailSendingJob(job, cancellationFlag);
                    break;
                case JobType.PAYMENT_PROCESSING:
                    processPaymentProcessingJob(job, cancellationFlag);
                    break;
                default:
                    handleFailedJob(job, "Job type not supported");
                    return;
            }

            if (!cancellationFlag.get()) {
                handleCompletedJob(job);
                logger.info("Successfully processed job with ID: {}", jobId);
            }
        } catch (Exception e) {
            handleFailedJob(job, e.getMessage());
            logger.error("Failed to process job with ID: {}", jobId, e);
        }
    }

    private void processEmailSendingJob(JobEntity job, AtomicBoolean cancellationFlag) {
        logger.info("Processing email sending job with ID: {}", job.getId());

        try {
            int totalSteps = 10;
            int waitTimeMs = 3000; // 3 seconds per step, 10x3 = 30 seconds, total = 0.5 minutes

            for (int step = 1; step <= totalSteps; step++) {

                if (checkCanceled(job.getId())) {
                    cancellationFlag.set(true);
                    return;
                }

                int progress = (step * 100) / totalSteps;
                updateJobProgress(job, progress);

                logger.info("Email sending job id {}, progress: {}%", job.getId(), progress);

                Thread.sleep(waitTimeMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email sending job was interrupted: " + e.getMessage());
        }
    }

    private void processPaymentProcessingJob(JobEntity job, AtomicBoolean cancellationFlag) {
        logger.info("Processing payment processing job with ID: {}", job.getId());

        try {
            int totalSteps = 10;
            int waitTimeMs = 6000; // 6 seconds per step, 10x6 = 60 seconds, total = 1 minutes

            for (int step = 1; step <= totalSteps; step++) {

                if (checkCanceled(job.getId())) {
                    cancellationFlag.set(true);
                    return;
                }

                int progress = (step * 100) / totalSteps;
                updateJobProgress(job, progress);

                logger.info("Payment processing job id {}, progress: {}%", job.getId(), progress);

                Thread.sleep(waitTimeMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing job was interrupted: " + e.getMessage());
        }
    }

    private void updateJobProgress(JobEntity job, int progress) {
        job.setCurrentProgress(progress);
        jobRepository.save(job);
    }

    //In this implementation, job isn't resuming from the last progress, it's starting from 0% again.
    private void updateJobAsProcessing(JobEntity job) {
        job.setStatus(JobStatus.PROCESSING);
        job.setCurrentProgress(0);
        job.setStartedAt(LocalDateTime.now());
        job.setCurrentRetryAttemptCount(job.getCurrentRetryAttemptCount() + 1);
        jobRepository.save(job);
    }

    private void handleFailedJob(JobEntity job, String reason) {
        job.setStatus(JobStatus.FAILED);
        job.setResult(null);
        job.setCurrentProgress(0);
        job.setCompletedAt(null);

        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }

        job.setErrorMessage(reason != null ? reason : "Unknown error");

        jobRepository.save(job);
    }

    private void handleCompletedJob(JobEntity job) {
        job.setStatus(JobStatus.COMPLETED);
        job.setResult("Job completed successfully");
        job.setCurrentProgress(100);

        if (job.getCompletedAt() == null) {
            job.setCompletedAt(LocalDateTime.now());
        }

        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }

        job.setErrorMessage(null);

        jobDependencyService.informDependents(job.getId());

        jobRepository.save(job);
    }

    private Boolean checkCanceled(UUID jobId) {
        var jobFromDb = jobRepository.findById(jobId);
        if (jobFromDb.isPresent() && jobFromDb.get().getStatus() == JobStatus.CANCELED) {
            logger.info("Job with ID: {} was canceled during processing", jobId);
            return true;
        }
        return false;
    }
}
