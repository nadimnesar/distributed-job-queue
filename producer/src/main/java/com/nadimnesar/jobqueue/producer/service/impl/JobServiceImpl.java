package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.constant.Constants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
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

import java.util.*;
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
                .build();

        if (Objects.nonNull(jobRequest.getMaxAttemptCount()) &&
                jobRequest.getMaxAttemptCount() > 0) {
            jobEntity.setMaxAttemptCount(jobRequest.getMaxAttemptCount());
        }

        JobEntity savedJob = jobRepository.save(jobEntity);

        jobDependencyService.setDependencies(savedJob.getId(), jobRequest.getDependencies());

        // Enqueue job only after transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobQueueService.publish(savedJob);
            }
        });

        return CommonResponse.builder()
                .message("Job is created successfully.")
                .data(savedJob)
                .code(HttpStatus.CREATED.value())
                .build();
    }

    /**
     * Validates the job request to ensure a Directed Acyclic Graph (DAG) of
     * dependencies and enforces the priority hierarchy.
     * <p>
     * Checks performed:
     * <ul>
     *   <li>All referenced dependency jobs exist in the database</li>
     *   <li>No dependency is in {@link JobStatus#CANCELED CANCELED} or
     *       {@link JobStatus#DEAD DEAD} state</li>
     *   <li>Priority hierarchy is not violated (a higher-priority job cannot
     *       depend on a lower-priority job)</li>
     * </ul>
     *
     * @param jobRequest the job request to validate
     * @throws IllegalArgumentException if any validation check fails
     */
    private void validateJobRequest(JobRequest jobRequest) {
        Set<UUID> dependencies = jobRequest.getDependencies();

        if (dependencies == null || dependencies.isEmpty()) {
            logger.info("Job request has no dependencies");
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

        Set<UUID> invalidDependencies = dependencyJobs.stream()
                .filter(job -> job.getStatus() == JobStatus.CANCELED ||
                        job.getStatus() == JobStatus.DEAD)
                .map(JobEntity::getId)
                .collect(Collectors.toSet());

        if (!invalidDependencies.isEmpty()) {
            logger.error("Cannot depend on canceled/dead jobs: {}", invalidDependencies);
            throw new IllegalArgumentException("Cannot depend on canceled/dead jobs: " + invalidDependencies);
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

    /**
     * Checks whether a job's priority would violate the hierarchy rule:
     * a higher-priority job cannot depend on a lower-priority job.
     * <p>
     * The valid dependency direction is: equal or higher-to-equal/lower priority.
     * For example, a {@link JobPriority#HIGH HIGH} job may not depend on a
     * {@link JobPriority#MEDIUM MEDIUM} or {@link JobPriority#LOW LOW} job.
     *
     * @param dependencyPriority the priority of the existing dependency job
     * @param jobPriority        the priority of the job being submitted
     * @return {@code true} if the dependency violates the priority hierarchy
     */
    private boolean isPriorityViolation(JobPriority dependencyPriority, JobPriority jobPriority) {
        return (dependencyPriority.equals(JobPriority.MEDIUM) && jobPriority.equals(JobPriority.HIGH)) ||
                (dependencyPriority.equals(JobPriority.LOW) &&
                        (jobPriority.equals(JobPriority.HIGH) || jobPriority.equals(JobPriority.MEDIUM)));
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

        var job = findJobById(jobId);
        if (job.isEmpty()) {
            logger.info("No job found with given id: {}", jobId);
            return CommonResponse.notFound("No job found with given id.");
        }

        return CommonResponse.builder()
                .data(job.map(this::convertToJobResponse))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CommonResponse getJobByStatus(String jobStatus) {
        logger.info("Getting jobs by status: {}", jobStatus);

        try {
            JobStatus status = JobStatus.valueOf(jobStatus.toUpperCase());
            List<JobEntity> jobs = jobRepository.findByStatus(status);

            if (jobs.isEmpty()) {
                logger.info("No jobs found with status: {}", jobStatus);
                return CommonResponse.notFound("No jobs found with status: " + jobStatus);
            }

            var jobResponses = jobs.stream().map(this::convertToJobResponse).toList();
            return CommonResponse.builder().data(jobResponses).build();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid job status: {}", jobStatus);
            return CommonResponse.badRequest("Invalid job status: " + jobStatus);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommonResponse getJobsByType(String jobType) {
        logger.info("Getting jobs by type: {}", jobType);

        try {
            JobType type = JobType.valueOf(jobType.toUpperCase());
            List<JobEntity> jobs = jobRepository.findByType(type);

            if (jobs.isEmpty()) {
                logger.info("No jobs found with type: {}", jobType);
                return CommonResponse.notFound("No jobs found with type: " + jobType);
            }

            var jobResponses = jobs.stream().map(this::convertToJobResponse).toList();
            return CommonResponse.builder().data(jobResponses).build();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid job type: {}", jobType);
            return CommonResponse.badRequest("Invalid job type: " + jobType);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommonResponse getJobsByStatusAndType(String jobStatus, String jobType) {
        logger.info("Getting jobs by status: {} and type: {}", jobStatus, jobType);

        try {
            JobStatus status = JobStatus.valueOf(jobStatus.toUpperCase());
            JobType type = JobType.valueOf(jobType.toUpperCase());
            List<JobEntity> jobs = jobRepository.findByStatusAndType(status, type);

            if (jobs.isEmpty()) {
                logger.info("No jobs found with status: {} and type: {}", jobStatus, jobType);
                return CommonResponse.notFound("No jobs found with status: " + jobStatus + " and type: " + jobType);
            }

            var jobResponses = jobs.stream().map(this::convertToJobResponse).toList();
            return CommonResponse.builder().data(jobResponses).build();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid job status or type: {} / {}", jobStatus, jobType);
            return CommonResponse.badRequest("Invalid job status or type");
        }
    }

    @Override
    @Transactional
    public CommonResponse cancelJob(String jobId) {
        logger.info("Cancelling job: {}", jobId);

        var optionalJob = findJobById(jobId);
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
            markJobAsCanceled(job);
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

    private void markJobAsCanceled(JobEntity jobEntity) {
        jobEntity.setStatus(JobStatus.CANCELED);
        jobRepository.save(jobEntity);

        jobDependencyService.informDependents(jobEntity.getId());

        logger.info("Job cancellation completed, job with ID: {}", jobEntity.getId());
    }

    @Override
    @Transactional
    public CommonResponse reviveAllDeadJobs() {
        logger.info("Starting to revive dead jobs");

        try {
            List<String> revivedJobIds = jobQueueService.consumeDeadLetters();

            if (revivedJobIds.isEmpty()) {
                logger.info("No dead jobs found to revive");
                return CommonResponse.notFound("No dead jobs found to revive");
            }

            logger.info("Reviving dead jobs: {}", revivedJobIds);

            // Convert String IDs to UUIDs
            List<UUID> revivedJobUUIDs = revivedJobIds.stream().map(UUID::fromString).toList();

            List<JobEntity> revivedJobs = jobRepository.findAllById(revivedJobUUIDs);
            revivedJobs.forEach(this::resetJobForRevival);

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

    @Override
    @Transactional
    public CommonResponse reviveDeadJobById(String jobId) {
        logger.info("Reviving dead job by id: {}", jobId);

        try {
            var optionalJob = findJobById(jobId);

            if (optionalJob.isEmpty()) {
                logger.info("Job not found with id: {}", jobId);
                return CommonResponse.notFound("Job not found with id: " + jobId);
            }

            var job = optionalJob.get();
            if (job.getStatus() != JobStatus.DEAD) {
                logger.info("Job {} is not in a dead state, current status: {}", jobId, job.getStatus());
                return CommonResponse.badRequest("Job is not in a dead state. Failed jobs are retried automatically.");
            }

            resetJobForRevival(job);

            jobRepository.save(job);

            logger.info("Successfully revived job with id: {}", jobId);
            return CommonResponse.builder()
                    .message("Job revived successfully")
                    .data(jobId)
                    .code(HttpStatus.OK.value())
                    .build();
        } catch (Exception e) {
            logger.error("Failed to revive job: {}", e.getMessage(), e);
            return CommonResponse.builder()
                    .message("Failed to revive job: " + e.getMessage())
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    private Optional<JobEntity> findJobById(String jobId) {
        return jobRepository.findById(UUID.fromString(jobId));
    }

    private void resetJobForRevival(JobEntity job) {
        job.setStatus(JobStatus.PENDING);
        job.setAttemptCount(Constants.INITIAL_ATTEMPT_COUNT);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setResult(null);
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
                .attemptCount(jobEntity.getAttemptCount())
                .maxAttemptCount(jobEntity.getMaxAttemptCount())
                .startedAt(jobEntity.getStartedAt())
                .completedAt(jobEntity.getCompletedAt())
                .build();
    }
}
