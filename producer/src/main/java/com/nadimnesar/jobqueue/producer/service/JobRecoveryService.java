package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.dto.ConsumedDlqMessage;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class JobRecoveryService {

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;

    @Transactional
    public CommonResponse resetAndSaveRevivedJobs(List<ConsumedDlqMessage> revivedJobs) {
        List<UUID> revivedJobUUIDs = revivedJobs.stream().map(ConsumedDlqMessage::jobId).map(UUID::fromString).toList();
        List<JobEntity> revivedJobEntities = jobRepository.findAllById(revivedJobUUIDs);
        revivedJobEntities.forEach(this::resetJobForRevival);
        jobRepository.saveAll(revivedJobEntities);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishRevivedJobs(revivedJobEntities, revivedJobs);
            }
        });

        List<String> revivedJobIds = revivedJobs.stream().map(ConsumedDlqMessage::jobId).toList();
        log.info("Successfully revived {} dead jobs", revivedJobs.size());
        return CommonResponse.builder()
                .message(String.format("Successfully revived %d dead jobs", revivedJobs.size()))
                .data(revivedJobIds)
                .code(HttpStatus.OK.value())
                .build();
    }

    void publishRevivedJobs(List<JobEntity> jobs,
                            List<ConsumedDlqMessage> dlqMessageList) {
        Map<String, String> traceIdByJobId = new HashMap<>();
        for (ConsumedDlqMessage dto : dlqMessageList) {
            traceIdByJobId.put(dto.jobId(), dto.traceId());
        }

        Map<String, String> savedContext = MDC.getCopyOfContextMap();
        try {
            for (JobEntity job : jobs) {
                String jobId = job.getId().toString();
                String traceId = traceIdByJobId.get(jobId);
                try {
                    TracingUtils.setTraceId(traceId);
                    TracingUtils.setNewSpanId();
                    jobQueueService.publish(job);
                } catch (Exception e) {
                    log.error("Failed to publish revived job {} after commit. Job is PENDING in DB" +
                            " but not enqueued. Manual re-enqueue required.", job.getId(), e);
                }
            }
        } finally {
            JobQueueService.restoreMdc(savedContext);
        }
    }

    public void resetJobForRevival(JobEntity job) {
        job.setStatus(JobStatus.PENDING);
        job.setAttemptCount(AppConstants.INITIAL_ATTEMPT_COUNT);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setResult(null);
    }
}
