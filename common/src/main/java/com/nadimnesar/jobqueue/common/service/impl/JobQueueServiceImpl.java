package com.nadimnesar.jobqueue.common.service.impl;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobQueueServiceImpl implements JobQueueService {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueServiceImpl.class);

    private final JobRepository jobRepository;

    @Override
    public void enqueueJob(JobEntity job) {
        var jobId = job.getId().toString();
        logger.info("Enqueueing job with ID: {}", jobId);

        job.setStatus(JobStatus.PENDING);
        jobRepository.save(job);

        logger.info("Successfully enqueued job with ID: {}", jobId);
    }

    @Override
    public String dequeueJob() {
        logger.info("Attempting to dequeue job");

        var jobOpt = jobRepository.findNextPendingJob();

        if (jobOpt.isEmpty()) {
            logger.info("No jobs found in the queue");
            return null;
        }

        var job = jobOpt.get();
        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);

        var jobId = job.getId().toString();
        logger.info("Successfully dequeued job with ID: {}", jobId);
        return jobId;
    }

    @Override
    public void moveToDeadLetterQueue(String jobId) {
        logger.info("Moving job {} to dead letter queue", jobId);

        var jobOpt = jobRepository.findById(UUID.fromString(jobId));
        jobOpt.ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
        });
    }

    @Override
    public List<String> dequeueDeadLetterJobs() {
        logger.info("Attempting to dequeue dead letter jobs");

        List<JobEntity> deadJobs = jobRepository.findDeadLetterJobs();

        if (!deadJobs.isEmpty()) {
            List<String> jobIds = deadJobs.stream()
                    .map(job -> job.getId().toString())
                    .toList();
            logger.info("Successfully dequeued {} dead letter jobs", jobIds.size());
            return jobIds;
        }

        logger.info("No dead letter jobs available to dequeue");
        return List.of();
    }
}
