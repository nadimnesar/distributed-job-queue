package com.nadimnesar.jobqueue.producer.service.impl;

import com.nadimnesar.jobqueue.common.dto.request.JobRequest;
import com.nadimnesar.jobqueue.common.dto.response.JobResponse;
import com.nadimnesar.jobqueue.producer.entity.JobEntity;
import com.nadimnesar.jobqueue.producer.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.producer.service.JobService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    public JobEntity submitJob(JobRequest jobRequest) {
        logger.info("JobServiceImpl|Submitting job {}", jobRequest);

        JobEntity jobEntity = JobEntity.builder()
                .type(jobRequest.getType())
                .priority(jobRequest.getPriority())
                .payload(jobRequest.getPayload())
                .maxRetryAttemptCount(jobRequest.getMaxRetryAttemptCount())
                .build();

        JobEntity savedJob = jobRepository.save(jobEntity);
        jobQueueService.enqueueJob(savedJob.getId().toString());

        return savedJob;
    }

    public List<JobResponse> getAllJobs(int pageNumber, int pageSize) {
        logger.info("JobServiceImpl|Getting all jobs by page number: {}, page size: {}", pageNumber, pageSize);

        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);
        List<JobEntity> jobEntityEntities = jobRepository.findAll(pageable).getContent();

        return jobEntityEntities.stream()
                .map(entity -> JobResponse.builder()
                        .id(entity.getId())
                        .priority(entity.getPriority())
                        .status(entity.getStatus())
                        .type(entity.getType())
                        .result(entity.getResult())
                        .errorMessage(entity.getErrorMessage())
                        .currentProgress(entity.getCurrentProgress())
                        .currentRetryAttemptCount(entity.getCurrentRetryAttemptCount())
                        .maxRetryAttemptCount(entity.getMaxRetryAttemptCount())
                        .startedAt(entity.getStartedAt())
                        .completedAt(entity.getCompletedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public JobResponse getJobById(String jobId) {
        logger.info("JobServiceImpl|Getting job by id: {}", jobId);

        UUID id = UUID.fromString(jobId);
        Optional<JobEntity> job = jobRepository.findById(id);

        return job.map(jobEntity -> JobResponse.builder()
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
                .build()).orElse(null);
    }
}
