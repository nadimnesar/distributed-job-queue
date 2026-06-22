package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobAckStatus;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.worker.handler.JobHandler;
import com.nadimnesar.jobqueue.worker.handler.JobHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobProcessorService {

    private final JobDependencyService jobDependencyService;
    private final JobHandlerRegistry jobHandlerRegistry;
    private final JobStateService jobStateService;
    private final JobRepository jobRepository;

    public JobAckStatus processJob(String jobId) {
        log.info("Processing job with id: {}", jobId);

        UUID jobUuid;
        try {
            jobUuid = UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid job ID format: '{}', acking to remove corrupt message", jobId);
            return JobAckStatus.ACK;
        }

        Optional<JobEntity> optionalJob = jobRepository.findById(jobUuid);
        if (optionalJob.isEmpty()) {
            log.error("Job with ID: {} not found", jobId);
            return JobAckStatus.ACK;
        }

        var job = optionalJob.get();
        switch (job.getStatus()) {
            case JobStatus.CANCELED:
                log.info("Job with ID: {} already canceled, no needs to process it", jobId);
                return JobAckStatus.ACK;
            case JobStatus.COMPLETED:
                log.info("Job with ID: {} already completed", jobId);
                return JobAckStatus.ACK;
            case JobStatus.DEAD:
                log.warn("Job {} is dead, acking to remove from queue", jobId);
                return JobAckStatus.ACK;
        }

        if (!jobDependencyService.getDependencies(job.getId()).isEmpty()) {
            log.info("Job with ID: {} has dependencies, will retry after delay", jobId);
            return JobAckStatus.NACK;
        }

        Optional<JobHandler> handlerOpt = jobHandlerRegistry.getHandler(job.getType());
        if (handlerOpt.isEmpty()) {
            log.error("No handler registered for job type: {}, jobId={}", job.getType(), jobId);
            jobStateService.handleFailedJob(job, "No handler registered for job type: " + job.getType());
            return JobAckStatus.NACK;
        }

        JobHandler handler = handlerOpt.get();

        updateJobAsProcessing(job);

        try {
            handler.handle(job);
            jobStateService.handleCompletedJob(job);
            return JobAckStatus.ACK;
        } catch (Exception e) {
            log.error("Handler failed for job id={}, type={}: {}", job.getId(), job.getType(), e.getMessage(), e);
            jobStateService.handleFailedJob(job, e.getMessage());
            return JobAckStatus.NACK;
        }
    }

    private void updateJobAsProcessing(JobEntity job) {
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);
    }
}
