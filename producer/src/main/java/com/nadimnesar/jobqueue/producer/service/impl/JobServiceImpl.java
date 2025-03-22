package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import com.nadimnesar.jobqueue.producer.dto.response.JobResponse;
import com.nadimnesar.jobqueue.producer.service.JobService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class JobServiceImpl implements JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;

    @Transactional
    public JobEntity submitJob(JobRequest jobRequest) {
        logger.info("JobServiceImpl|Submitting job {}", jobRequest);

        JobEntity jobEntity = JobEntity.builder()
                .type(jobRequest.getType())
                .priority(jobRequest.getPriority())
                .payload(jobRequest.getPayload())
                .maxRetryAttemptCount(jobRequest.getMaxRetryAttemptCount())
                .build();

        JobEntity savedJob = jobRepository.save(jobEntity);

        // Enqueue job only after transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobQueueService.enqueueJob(savedJob);
            }
        });

        return savedJob;
    }

    public List<JobResponse> getAllJobs(int pageNumber, int pageSize) {
        logger.info("JobServiceImpl|Getting all jobs by page number: {}, page size: {}", pageNumber, pageSize);

        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);
        List<JobEntity> jobEntityEntities = jobRepository.findAll(pageable).getContent();

        return jobEntityEntities.stream()
                .map(this::convertToJobResponse)
                .collect(Collectors.toList());
    }

    @Override
    public JobResponse getJobById(String jobId) {
        logger.info("JobServiceImpl|Getting job by id: {}", jobId);

        Optional<JobEntity> job = jobRepository.findById(UUID.fromString(jobId));

        return job.map(this::convertToJobResponse).orElse(null);
    }

    private JobResponse convertToJobResponse(JobEntity jobEntity) {
        return JobResponse.builder()
                .id(jobEntity.getId())
                .priority(jobEntity.getPriority())
                .status(jobEntity.getStatus())
                .type(jobEntity.getType())
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
