package com.nadimnesar.jobqueue.producer.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class JobService {

    private final JobQueueService jobQueueService;
    private final JobDependencyService jobDependencyService;
    private final JobRecoveryService jobRecoveryService;
    private final JobRepository jobRepository;

    @Transactional
    public CommonResponse submitJob(JobRequest jobRequest) {
        log.info("Submitting job {}", jobRequest);

        validateJobRequest(jobRequest);

        JobEntity jobEntity = JobEntity.builder()
                .priority(jobRequest.getPriority())
                .type(jobRequest.getType())
                .payload(jobRequest.getPayload())
                .build();

        if (jobRequest.getMaxAttemptCount() != null) {
            jobEntity.setMaxAttemptCount(jobRequest.getMaxAttemptCount());
        }

        JobEntity savedJob = jobRepository.save(jobEntity);

        jobDependencyService.setDependencies(savedJob.getId(), jobRequest.getDependencies());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    jobQueueService.publish(savedJob);
                } catch (Exception e) {
                    log.error("Failed to publish job {} after commit. Job remains PENDING in DB and must be " +
                            "manually re-enqueued.", savedJob.getId(), e);
                }
            }
        });

        return CommonResponse.builder()
                .message("Job is created successfully.")
                .data(convertToJobResponse(savedJob))
                .code(HttpStatus.CREATED.value())
                .build();
    }

    private void validateJobRequest(JobRequest jobRequest) {
        Set<UUID> dependencies = jobRequest.getDependencies();

        if (dependencies == null || dependencies.isEmpty()) {
            log.info("Job request has no dependencies");
            return;
        }

        List<JobEntity> dependencyJobs = jobRepository.findAllById(dependencies);

        Set<UUID> dependencyJobIds = dependencyJobs.stream()
                .map(JobEntity::getId)
                .collect(Collectors.toSet());

        Set<UUID> missingDependencies = dependencies.stream()
                .filter(id -> !dependencyJobIds.contains(id))
                .collect(Collectors.toSet());

        if (!missingDependencies.isEmpty()) {
            log.error("Dependencies not found: {}", missingDependencies);
            throw new IllegalArgumentException("Dependencies not found: " + missingDependencies);
        }

        Set<UUID> invalidDependencies = dependencyJobs.stream()
                .filter(job -> job.getStatus() == JobStatus.CANCELED ||
                        job.getStatus() == JobStatus.DEAD)
                .map(JobEntity::getId)
                .collect(Collectors.toSet());

        if (!invalidDependencies.isEmpty()) {
            log.error("Cannot depend on canceled/dead jobs: {}", invalidDependencies);
            throw new IllegalArgumentException("Cannot depend on canceled/dead jobs: " + invalidDependencies);
        }

        for (JobEntity dependency : dependencyJobs) {
            if (isPriorityViolation(dependency.getPriority(), jobRequest.getPriority())) {
                log.error("Priority hierarchy violation: {} job cannot depend on {} job",
                        jobRequest.getPriority(), dependency.getPriority());
                throw new IllegalArgumentException(
                        String.format("Job priority hierarchy violated: %s job cannot depend on %s job",
                                jobRequest.getPriority(), dependency.getPriority()));
            }
        }
    }

    private boolean isPriorityViolation(JobPriority dependencyPriority, JobPriority jobPriority) {
        return jobPriority.ordinal() < dependencyPriority.ordinal();
    }

    @Transactional(readOnly = true)
    public CommonResponse getAllJobs(int pageNumber, int pageSize) {
        log.info("Getting all jobs by page number: {}, page size: {}", pageNumber, pageSize);

        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);
        Page<JobEntity> jobs = jobRepository.findAll(pageable);

        return getFilteredJobResponse(jobs);
    }

    @Transactional(readOnly = true)
    public CommonResponse getJobById(String jobId) {
        log.info("Getting job by id: {}", jobId);

        var job = findJobById(jobId);
        if (job.isEmpty()) {
            log.info("No job found with given id: {}", jobId);
            return CommonResponse.notFound("No job found with given id.");
        }

        return CommonResponse.builder()
                .data(job.map(this::convertToJobResponse))
                .build();
    }

    @Transactional(readOnly = true)
    public CommonResponse getJobByStatus(String jobStatus, Pageable pageable) {
        log.info("Getting jobs by status: {}, page: {}, size: {}", jobStatus, pageable.getPageNumber(), pageable.getPageSize());

        JobStatus status = JobStatus.valueOf(jobStatus.toUpperCase());
        Page<JobEntity> jobs = jobRepository.findByStatus(status, pageable);

        return getFilteredJobResponse(jobs);
    }

    @Transactional(readOnly = true)
    public CommonResponse getJobsByType(String jobType, Pageable pageable) {
        log.info("Getting jobs by type: {}, page: {}, size: {}",
                jobType, pageable.getPageNumber(), pageable.getPageSize());

        JobType type = JobType.valueOf(jobType.toUpperCase());
        Page<JobEntity> jobs = jobRepository.findByType(type, pageable);

        return getFilteredJobResponse(jobs);
    }

    @Transactional(readOnly = true)
    public CommonResponse getJobsByStatusAndType(String jobStatus, String jobType, Pageable pageable) {
        log.info("Getting jobs by status: {} and type: {}, page: {}, size: {}",
                jobStatus, jobType, pageable.getPageNumber(), pageable.getPageSize());

        JobStatus status = JobStatus.valueOf(jobStatus.toUpperCase());
        JobType type = JobType.valueOf(jobType.toUpperCase());
        Page<JobEntity> jobs = jobRepository.findByStatusAndType(status, type, pageable);

        return getFilteredJobResponse(jobs);
    }

    private CommonResponse getFilteredJobResponse(Page<JobEntity> jobs) {
        List<UUID> jobIds = jobs.getContent().stream().map(JobEntity::getId).toList();
        Map<UUID, Set<UUID>> dependenciesMap = jobDependencyService.getDependenciesBatch(jobIds);
        Map<UUID, Set<UUID>> dependentsMap = jobDependencyService.getDependentsBatch(jobIds);

        var jobResponses = jobs.getContent().stream()
                .map(job -> convertToJobResponseBatch(job, dependenciesMap, dependentsMap))
                .toList();

        return CommonResponse.builder().data(jobResponses).build();
    }

    @Transactional
    public CommonResponse cancelJob(String jobId) {
        log.info("Cancelling job: {}", jobId);

        var optionalJob = findJobById(jobId);
        if (optionalJob.isEmpty()) {
            log.info("Job not found: {}", jobId);
            return CommonResponse.notFound("Job not found");
        }

        var job = optionalJob.get();

        if (job.getStatus() == JobStatus.CANCELED) {
            log.info("Job cancellation failed, job with ID: {} is already canceled", jobId);
            return CommonResponse.badRequest("Job is already cancelled");
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            log.info("Job cancellation failed, job with ID: {} is already completed", jobId);
            return CommonResponse.badRequest("Job is already completed");
        }

        if (job.getStatus() == JobStatus.DEAD) {
            log.info("Job cancellation failed, job with ID: {} is already dead", jobId);
            return CommonResponse.badRequest("Job is already dead");
        }

        if (job.getStatus() == JobStatus.PROCESSING) {
            log.info("Job cancellation failed, job with ID: {} is currently processing", jobId);
            throw new IllegalStateException("Cannot cancel a job that is currently processing");
        }

        markJobAsCanceled(job);
        return CommonResponse.builder()
                .message("Job is cancelled successfully")
                .code(HttpStatus.ACCEPTED.value())
                .build();
    }

    private void markJobAsCanceled(JobEntity jobEntity) {
        jobEntity.setStatus(JobStatus.CANCELED);
        jobDependencyService.informDependents(jobEntity.getId());
        jobRepository.save(jobEntity);
        log.info("Job cancellation completed, job with ID: {}", jobEntity.getId());
    }

    public CommonResponse reviveAllDeadJobs() {
        log.info("Starting to revive dead jobs");

        List<String> revivedJobIds = jobQueueService.consumeDeadLetters();

        if (revivedJobIds.isEmpty()) {
            log.info("No dead jobs found to revive");
            return CommonResponse.notFound("No dead jobs found to revive");
        }

        log.info("Reviving dead jobs: {}", revivedJobIds);

        try {
            return jobRecoveryService.resetAndSaveRevivedJobs(revivedJobIds);
        } catch (Exception e) {
            log.error("Failed to save revived jobs after consuming DLQ. Job IDs {} have been removed from the DLQ" +
                    " but remain DEAD in the database. Manual re-enqueue required.", revivedJobIds, e);
            throw e;
        }
    }

    @Transactional
    public CommonResponse reviveDeadJobById(String jobId) {
        log.info("Reviving dead job by id: {}", jobId);

        var optionalJob = findJobById(jobId);

        if (optionalJob.isEmpty()) {
            log.info("Job not found with id: {}", jobId);
            return CommonResponse.notFound("Job not found with id: " + jobId);
        }

        var job = optionalJob.get();
        if (job.getStatus() != JobStatus.DEAD) {
            log.info("Job {} is not in a dead state, current status: {}", jobId, job.getStatus());
            return CommonResponse.badRequest("Job is not in a dead state. Failed jobs are retried automatically.");
        }

        jobRecoveryService.resetJobForRevival(job);
        jobRepository.save(job);

        final JobEntity jobToPublish = job;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    jobQueueService.publish(jobToPublish);
                } catch (Exception e) {
                    log.error("Failed to publish revived job {} after commit. Job is PENDING in DB but not enqueued." +
                            " Manual re-enqueue required.", jobToPublish.getId(), e);
                }
            }
        });

        log.info("Successfully revived job with id: {}", jobId);
        return CommonResponse.builder()
                .message("Job revived successfully")
                .data(jobId)
                .code(HttpStatus.OK.value())
                .build();
    }

    private Optional<JobEntity> findJobById(String jobId) {
        return jobRepository.findById(UUID.fromString(jobId));
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

    private JobResponse convertToJobResponseBatch(JobEntity jobEntity,
                                                  Map<UUID, Set<UUID>> dependenciesMap,
                                                  Map<UUID, Set<UUID>> dependentsMap) {
        return JobResponse.builder()
                .id(jobEntity.getId())
                .priority(jobEntity.getPriority())
                .status(jobEntity.getStatus())
                .type(jobEntity.getType())
                .dependents(dependentsMap.getOrDefault(jobEntity.getId(), Set.of()))
                .dependencies(dependenciesMap.getOrDefault(jobEntity.getId(), Set.of()))
                .result(jobEntity.getResult())
                .attemptCount(jobEntity.getAttemptCount())
                .maxAttemptCount(jobEntity.getMaxAttemptCount())
                .startedAt(jobEntity.getStartedAt())
                .completedAt(jobEntity.getCompletedAt())
                .build();
    }
}
