package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import com.nadimnesar.jobqueue.producer.dto.response.JobResponse;
import com.nadimnesar.jobqueue.producer.service.JobService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class JobServiceImpl implements JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;
    private final JobDependencyService jobDependencyService;

    @Override
    @Transactional
    public JobEntity submitJob(JobRequest jobRequest) {
        logger.info("JobServiceImpl|Submitting job {}", jobRequest);

        if (jobRequest.getDependencies() != null && !jobRequest.getDependencies().isEmpty()) {
            List<JobEntity> existingJobs = jobRepository.findAllById(jobRequest.getDependencies());

            Set<UUID> foundJobIds = existingJobs.stream()
                    .map(JobEntity::getId)
                    .collect(Collectors.toSet());

            //Job should be submitted only if all dependencies are found and not canceled
            Set<UUID> invalidDependencyIds = jobRequest.getDependencies().stream()
                    .filter(depId -> !foundJobIds.contains(depId) ||
                            existingJobs.stream()
                                    .anyMatch(job -> job.getId().equals(depId) && job.getStatus() == JobStatus.CANCELED))
                    .collect(Collectors.toSet());

            if (!invalidDependencyIds.isEmpty()) {
                logger.error("JobServiceImpl|Invalid dependencies found or canceled: {}", invalidDependencyIds);
                throw new IllegalArgumentException("Dependencies not found or canceled: " + invalidDependencyIds);
            }
        }

        JobEntity jobEntity = JobEntity.builder()
                .priority(jobRequest.getPriority())
                .type(jobRequest.getType())
                .payload(jobRequest.getPayload())
                .maxRetryAttemptCount(jobRequest.getMaxRetryAttemptCount())
                .build();

        JobEntity savedJob = jobRepository.save(jobEntity);

        jobDependencyService.setDependencies(savedJob.getId(), jobRequest.getDependencies());

        // Enqueue job only after transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobQueueService.enqueueJob(savedJob);
            }
        });

        return savedJob;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs(int pageNumber, int pageSize) {
        logger.info("JobServiceImpl|Getting all jobs by page number: {}, page size: {}", pageNumber, pageSize);

        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);
        List<JobEntity> jobEntities = jobRepository.findAll(pageable).getContent();

        return jobEntities.stream()
                .map(this::convertToJobResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(String jobId) {
        logger.info("JobServiceImpl|Getting job by id: {}", jobId);

        try {
            UUID uuid = UUID.fromString(jobId);
            Optional<JobEntity> job = jobRepository.findById(uuid);
            return job.map(this::convertToJobResponse).orElse(null);
        } catch (IllegalArgumentException e) {
            logger.error("JobServiceImpl|Invalid job ID format: {}", jobId);
            return null;
        }
    }

    @Override
    @Transactional
    public CommonResponse cancelJob(String jobId) {
        logger.info("JobServiceImpl|Cancelling job: {}", jobId);

        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));

        if (optionalJob.isEmpty()) {
            logger.info("JobServiceImpl|Job not found: {}", jobId);
            return CommonResponse.notFound("Job not found");
        }

        var job = optionalJob.get();

        if (job.getStatus() == JobStatus.CANCELED) {
            logger.info("JobServiceImpl|Job cancellation failed, job with ID: {} is already canceled", jobId);
            return CommonResponse.badRequest("Job is already cancelled");
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            logger.info("JobServiceImpl|Job cancellation failed, job with ID: {} is already completed", jobId);
            return CommonResponse.badRequest("Job is already completed");
        }

        try {
            cancelJob(job);
            return CommonResponse.builder()
                    .message("Job is cancelled successfully")
                    .code(HttpStatus.ACCEPTED.value())
                    .build();
        } catch (Exception e) {
            logger.error("JobServiceImpl|Job cancellation failed, error: {}", e.getMessage(), e);
            return CommonResponse.builder()
                    .message("Job cancellation failed")
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    private void cancelJob(JobEntity jobEntity) {
        jobEntity.setStatus(JobStatus.CANCELED);
        jobRepository.save(jobEntity);

        jobDependencyService.informDependents(jobEntity.getId());

        logger.info("JobServiceImpl|Job cancellation completed, job with ID: {}", jobEntity.getId());
    }

    private JobResponse convertToJobResponse(JobEntity jobEntity) {
        return JobResponse.builder()
                .id(jobEntity.getId())
                .priority(jobEntity.getPriority())
                .status(jobEntity.getStatus())
                .type(jobEntity.getType())
                .dependents(jobDependencyService.getDependents(jobEntity.getId()))
                .dependencies(jobDependencyService.getDependencies(jobEntity.getId()))
                .result(jobEntity.getResult())
                .errorMessage(jobEntity.getErrorMessage())
                .currentProgress(jobEntity.getCurrentProgress())
                .currentRetryAttemptCount(jobEntity.getCurrentRetryAttemptCount())
                .maxRetryAttemptCount(jobEntity.getMaxRetryAttemptCount())
                .startedAt(jobEntity.getStartedAt())
                .completedAt(jobEntity.getCompletedAt())
                .build();
    }
}
