package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobStateService {

    private final JobRepository jobRepository;
    private final JobDependencyService jobDependencyService;

    @Transactional
    public void handleFailedJob(JobEntity job, String reason) {
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(LocalDateTime.now());

        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }

        job.setResult(reason != null ? reason : "Unknown error");
        jobRepository.save(job);
    }

    @Transactional
    public void handleCompletedJob(JobEntity job) {
        job.setStatus(JobStatus.COMPLETED);
        job.setResult("Job completed successfully");

        if (job.getCompletedAt() == null) {
            job.setCompletedAt(LocalDateTime.now());
        }

        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }

        jobRepository.save(job);
        jobDependencyService.informDependents(job.getId());
    }
}
