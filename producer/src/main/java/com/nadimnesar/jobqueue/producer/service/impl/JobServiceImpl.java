package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
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
    public CommonResponse submitJob(JobRequest jobRequest) {
        logger.info("Submitting job {}", jobRequest);

        validateJobRequest(jobRequest);

        JobEntity jobEntity = JobEntity.builder()
                .priority(jobRequest.getPriority())
                .type(jobRequest.getType())
                .payload(jobRequest.getPayload())
                .maxAttemptCount(jobRequest.getMaxAttemptCount())
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

        return CommonResponse.builder()
                .message("Job is created successfully.")
                .data(savedJob)
                .code(HttpStatus.CREATED.value())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CommonResponse getAllJobs(int pageNumber, int pageSize) {
        logger.info("Getting all jobs by page number: {}, page size: {}", pageNumber, pageSize);

        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);
        List<JobEntity> jobEntities = jobRepository.findAll(pageable).getContent();

        var jobResponses = jobEntities.stream().map(this::convertToJobResponse).toList();

        if (jobResponses.isEmpty()) {
            logger.info("No jobs found");
            return CommonResponse.notFound("No jobs found");
        }

        return CommonResponse.builder()
                .data(jobResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CommonResponse getJobById(String jobId) {
        logger.info("Getting job by id: {}", jobId);

        Optional<JobEntity> job = jobRepository.findById(UUID.fromString(jobId));

        if (job.isEmpty()) {
            logger.info("No job found with given id: {}", jobId);
            return CommonResponse.notFound("No job found with given id.");
        }

        var jobResponse = job.map(this::convertToJobResponse);

        return CommonResponse.builder()
                .data(jobResponse)
                .build();
    }

    @Override
    @Transactional
    public CommonResponse cancelJob(String jobId) {
        logger.info("Cancelling job: {}", jobId);

        Optional<JobEntity> optionalJob = jobRepository.findById(UUID.fromString(jobId));

        if (optionalJob.isEmpty()) {
            logger.info("Job not found: {}", jobId);
            return CommonResponse.notFound("Job not found");
        }

        var job = optionalJob.get();

        if (job.getStatus() == JobStatus.CANCELED) {
            logger.info("Job cancellation failed, job with ID: {} is already canceled", jobId);
            return CommonResponse.badRequest("Job is already cancelled");
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            logger.info("Job cancellation failed, job with ID: {} is already completed", jobId);
            return CommonResponse.badRequest("Job is already completed");
        }

        try {
            cancelJob(job);
            return CommonResponse.builder()
                    .message("Job is cancelled successfully")
                    .code(HttpStatus.ACCEPTED.value())
                    .build();
        } catch (Exception e) {
            logger.error("Job cancellation failed, error: {}", e.getMessage(), e);
            return CommonResponse.builder()
                    .message("Job cancellation failed")
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    //Insure Directed Acyclic Graph (DAG) of jobs
    //Insure Priority Hierarchy for jobs
    private void validateJobRequest(JobRequest jobRequest) {
        Set<UUID> dependencies = jobRequest.getDependencies();

        if (dependencies == null || dependencies.isEmpty()) {
            logger.debug("Job request has no dependencies");
            return;
        }

        List<JobEntity> dependencyJobs = jobRepository.findAllById(dependencies);

        Set<UUID> foundJobIds = dependencyJobs.stream()
                .map(JobEntity::getId)
                .collect(Collectors.toSet());

        Set<UUID> missingDependencies = dependencies.stream()
                .filter(id -> !foundJobIds.contains(id))
                .collect(Collectors.toSet());

        if (!missingDependencies.isEmpty()) {
            logger.error("Dependencies not found: {}", missingDependencies);
            throw new IllegalArgumentException("Dependencies not found: " + missingDependencies);
        }

        Set<UUID> canceledDependencies = dependencyJobs.stream()
                .filter(job -> job.getStatus() == JobStatus.CANCELED)
                .map(JobEntity::getId)
                .collect(Collectors.toSet());

        if (!canceledDependencies.isEmpty()) {
            logger.error("Cannot depend on canceled jobs: {}", canceledDependencies);
            throw new IllegalArgumentException("Cannot depend on canceled jobs: " + canceledDependencies);
        }

        for (JobEntity dependency : dependencyJobs) {
            if (isPriorityViolation(dependency.getPriority(), jobRequest.getPriority())) {
                logger.error("Priority hierarchy violation: {} job cannot depend on {} job",
                        jobRequest.getPriority(), dependency.getPriority());
                throw new IllegalArgumentException(
                        String.format("Job priority hierarchy violated: %s job cannot depend on %s job",
                                jobRequest.getPriority(), dependency.getPriority()));
            }
        }
    }

    // Higher priority job cannot depend on lower priority job
    private boolean isPriorityViolation(JobPriority dependencyPriority, JobPriority jobPriority) {
        return (dependencyPriority.equals(JobPriority.MEDIUM) && jobPriority.equals(JobPriority.HIGH)) ||
                (dependencyPriority.equals(JobPriority.LOW) &&
                        (jobPriority.equals(JobPriority.HIGH) || jobPriority.equals(JobPriority.MEDIUM)));
    }

    @Override
    public CommonResponse reviveDeadJobs() {
        logger.info("Starting to revive dead jobs");

        try {
            List<String> revivedJobIds = jobQueueService.dequeueDeadLetterJobs();

            if (revivedJobIds.isEmpty()) {
                logger.info("No dead jobs found to revive");
                return CommonResponse.notFound("No dead jobs found to revive");
            }

            logger.info("Reviving dead jobs: {}", revivedJobIds);

            // Convert String IDs to UUIDs
            List<UUID> revivedJobUUIDs = revivedJobIds.stream().map(UUID::fromString).toList();

            // Find all jobs that were revived and update their status
            List<JobEntity> revivedJobs = jobRepository.findAllById(revivedJobUUIDs);
            for (JobEntity job : revivedJobs) {
                job.setStatus(JobStatus.PENDING);
                job.setErrorMessage(null);
                job.setCurrentProgress(0);
                job.setCurrentAttemptCount(0);
                job.setMaxAttemptCount(3); //default value 3
                job.setStartedAt(null);
            }

            jobRepository.saveAll(revivedJobs);

            logger.info("Successfully revived {} dead jobs", revivedJobIds.size());
            return CommonResponse.builder()
                    .message(String.format("Successfully revived %d dead jobs", revivedJobIds.size()))
                    .data(revivedJobIds)
                    .code(HttpStatus.OK.value())
                    .build();
        } catch (Exception e) {
            logger.error("Failed to revive dead jobs: {}", e.getMessage(), e);
            return CommonResponse.builder()
                    .message("Failed to revive dead jobs: " + e.getMessage())
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    private void cancelJob(JobEntity jobEntity) {
        jobEntity.setStatus(JobStatus.CANCELED);
        jobRepository.save(jobEntity);

        jobDependencyService.informDependents(jobEntity.getId());

        logger.info("Job cancellation completed, job with ID: {}", jobEntity.getId());
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
                .currentAttemptCount(jobEntity.getCurrentAttemptCount())
                .maxAttemptCount(jobEntity.getMaxAttemptCount())
                .startedAt(jobEntity.getStartedAt())
                .completedAt(jobEntity.getCompletedAt())
                .build();
    }
}
