package com.nadimnesar.jobqueue.worker.service.impl;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.repository.JobDependencyRepository;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.worker.service.JobRetryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobRetryServiceImpl implements JobRetryService {

    private static final Logger logger = LoggerFactory.getLogger(JobRetryServiceImpl.class);

    private final JobQueueService jobQueueService;
    private final JobRepository jobRepository;
    private final JobDependencyRepository jobDependencyRepository;
    private final JobDependencyService jobDependencyService;

    @Override
    @Scheduled(cron = "${schedule.cron.retry}")
    @Transactional
    public void retryJobs() {
        logger.info("JobRetryServiceImpl|Starting job retry process, at: {}", System.currentTimeMillis());

        removeOrphanDependencies();

        try {
            var retryableJobs = jobRepository.findJobsToRetry();

            if (retryableJobs.isEmpty()) {
                logger.info("JobRetryServiceImpl|No jobs to retry");
                return;
            }

            logger.info("JobRetryServiceImpl|Found {} jobs to retry", retryableJobs.size());

            retryableJobs.forEach(job -> {
                logger.info("JobRetryServiceImpl|Retrying job {}", job.getId());

                try {
                    jobQueueService.enqueueJob(job);
                    logger.info("JobRetryServiceImpl|Job {} retried successfully", job.getId());
                } catch (Exception e) {
                    logger.error("JobRetryServiceImpl|Error occurred while retrying job {}", job.getId(), e);
                }
            });

            logger.info("JobRetryServiceImpl|Job retry process completed");
        } catch (Exception e) {
            logger.error("JobRetryServiceImpl|Error occurred while retrying jobs", e);
        }
    }

    private void removeOrphanDependencies() {
        logger.info("JobRetryServiceImpl|Removing orphan dependencies");

        var dependencies = jobDependencyRepository.findAll();
        dependencies.forEach(jobDependency -> {
            var dependentJob = jobRepository.findById(jobDependency.getDependencyId());
            if (dependentJob.isPresent() && dependentJob.get().getStatus().equals(JobStatus.COMPLETED)) {
                jobDependencyService.informDependents(dependentJob.get().getId());
            }
        });
    }
}
