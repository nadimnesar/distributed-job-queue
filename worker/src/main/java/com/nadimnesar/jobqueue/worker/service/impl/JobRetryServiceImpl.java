package com.nadimnesar.jobqueue.worker.service.impl;

import com.nadimnesar.jobqueue.common.repository.JobRepository;
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

    @Override
    @Scheduled(cron = "${schedule.cron.retry}")
    @Transactional
    public void retryJobs() {
        logger.info("JobRetryServiceImpl|Starting job retry process");

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
}
