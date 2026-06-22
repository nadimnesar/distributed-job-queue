package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class JobRecoveryService {

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;

    @Transactional
    public CommonResponse resetAndSaveRevivedJobs(List<String> revivedJobIds) {
        List<UUID> revivedJobUUIDs = revivedJobIds.stream().map(UUID::fromString).toList();
        List<JobEntity> revivedJobs = jobRepository.findAllById(revivedJobUUIDs);
        revivedJobs.forEach(this::resetJobForRevival);
        jobRepository.saveAll(revivedJobs);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (JobEntity job : revivedJobs) {
                    try {
                        jobQueueService.publish(job);
                    } catch (Exception e) {
                        log.error("Failed to publish revived job {} after commit. Job is PENDING in DB" +
                                " but not enqueued. Manual re-enqueue required.", job.getId(), e);
                    }
                }
            }
        });

        log.info("Successfully revived {} dead jobs", revivedJobIds.size());
        return CommonResponse.builder()
                .message(String.format("Successfully revived %d dead jobs", revivedJobIds.size()))
                .data(revivedJobIds)
                .code(HttpStatus.OK.value())
                .build();
    }

    public void resetJobForRevival(JobEntity job) {
        job.setStatus(JobStatus.PENDING);
        job.setAttemptCount(AppConstants.INITIAL_ATTEMPT_COUNT);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setResult(null);
    }
}
