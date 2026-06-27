package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.dto.ConsumedDlqMessage;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class JobRecoveryServiceTest {

    private JobRepository jobRepository;
    private JobQueueService jobQueueService;
    private JobRecoveryService jobRecoveryService;

    @BeforeEach
    void setUp() {
        MDC.clear();
        jobRepository = mock(JobRepository.class);
        jobQueueService = mock(JobQueueService.class);
        jobRecoveryService = new JobRecoveryService(jobRepository, jobQueueService);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private JobEntity jobEntity(String jobIdStr) {
        return JobEntity.builder()
                .id(UUID.fromString(jobIdStr))
                .priority(JobPriority.MEDIUM)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.DEAD)
                .build();
    }

    @Test
    @DisplayName("AC2: publishRevivedJobs sets per-job MDC traceId around each publish and restores caller MDC (BUG-2 caller regression)")
    void publishRevivedJobs_setsPerJobTraceIdAndRestoresCallerMdc() {
        // Given: caller (REST endpoint) has its own tracing context
        MDC.put(AppConstants.MDC_TRACE_ID, "T-REST");
        MDC.put(AppConstants.MDC_SPAN_ID, "S-REST");

        String jobAUuid = "11111111-1111-1111-1111-111111111111";
        String jobBUuid = "22222222-2222-2222-2222-222222222222";
        JobEntity jobA = jobEntity(jobAUuid);
        JobEntity jobB = jobEntity(jobBUuid);

        List<JobEntity> jobs = List.of(jobA, jobB);
        List<ConsumedDlqMessage> dtos = List.of(
                new ConsumedDlqMessage(jobAUuid, "T-A"),
                new ConsumedDlqMessage(jobBUuid, "T-B"));

        // Capture the MDC traceId observed at each publish() invocation
        List<String> traceIdAtPublish = new ArrayList<>();
        doAnswer(_ -> {
            traceIdAtPublish.add(MDC.get(AppConstants.MDC_TRACE_ID));
            return null;
        }).when(jobQueueService).publish(any(JobEntity.class));

        // When: the afterCommit publish loop runs
        jobRecoveryService.publishRevivedJobs(jobs, dtos);

        // Then: each job was published under its OWN traceId
        // (before fix: all published with the caller's T-REST -> test FAILS)
        assertEquals(2, traceIdAtPublish.size());
        assertEquals("T-A", traceIdAtPublish.get(0),
                "jobA must be published under its own traceId T-A (BUG-2 caller)");
        assertEquals("T-B", traceIdAtPublish.get(1),
                "jobB must be published under its own traceId T-B (BUG-2 caller)");

        // and the caller's MDC is restored after the loop
        assertEquals("T-REST", MDC.get(AppConstants.MDC_TRACE_ID),
                "caller traceId must be restored after revive loop");
        assertEquals("S-REST", MDC.get(AppConstants.MDC_SPAN_ID),
                "caller spanId must be restored after revive loop");

        verify(jobQueueService, times(1)).publish(jobA);
        verify(jobQueueService, times(1)).publish(jobB);
    }

    @Test
    @DisplayName("AC2: publishRevivedJobs restores caller MDC even when saved context was null")
    void publishRevivedJobs_restoresNullCallerMdc() {
        // Given: caller has NO MDC context (getCopyOfContextMap() returns null)
        assertNull(MDC.getCopyOfContextMap());

        String jobAUuid = "33333333-3333-3333-3333-333333333333";
        JobEntity jobA = jobEntity(jobAUuid);
        List<JobEntity> jobs = List.of(jobA);
        List<ConsumedDlqMessage> dtos = List.of(new ConsumedDlqMessage(jobAUuid, "T-A"));

        doAnswer(_ -> null).when(jobQueueService).publish(any(JobEntity.class));

        // When
        jobRecoveryService.publishRevivedJobs(jobs, dtos);

        // Then: MDC is cleared (not left polluted with the job's traceId)
        assertNull(MDC.get(AppConstants.MDC_TRACE_ID),
                "MDC must be cleared when caller had no context");
        assertNull(MDC.get(AppConstants.MDC_SPAN_ID),
                "MDC spanId must be cleared when caller had no context");
    }

    @Test
    @DisplayName("AC2: publishRevivedJobs continues publishing remaining jobs if one publish throws")
    void publishRevivedJobs_continuesAfterPublishFailure() {
        MDC.put(AppConstants.MDC_TRACE_ID, "T-REST");

        String jobAUuid = "44444444-4444-4444-4444-444444444444";
        String jobBUuid = "55555555-5555-5555-5555-555555555555";
        JobEntity jobA = jobEntity(jobAUuid);
        JobEntity jobB = jobEntity(jobBUuid);
        List<JobEntity> jobs = List.of(jobA, jobB);
        List<ConsumedDlqMessage> dtos = List.of(
                new ConsumedDlqMessage(jobAUuid, "T-A"),
                new ConsumedDlqMessage(jobBUuid, "T-B"));

        List<String> traceIdAtPublish = new ArrayList<>();
        doThrow(new RuntimeException("broker down for jobA"))
                .doAnswer(_ -> {
                    traceIdAtPublish.add(MDC.get(AppConstants.MDC_TRACE_ID));
                    return null;
                }).when(jobQueueService).publish(any(JobEntity.class));

        // When
        jobRecoveryService.publishRevivedJobs(jobs, dtos);

        // Then: jobB was still published under its own traceId
        assertEquals(1, traceIdAtPublish.size());
        assertEquals("T-B", traceIdAtPublish.getFirst(),
                "jobB must still be published under T-B after jobA failed");

        // and caller MDC restored
        assertEquals("T-REST", MDC.get(AppConstants.MDC_TRACE_ID),
                "caller MDC must be restored even if a publish threw");
    }

    @Test
    @DisplayName("AC2: publishRevivedJobs tolerates a DLQ message with null traceId (no NPE, no header stamp)")
    void publishRevivedJobs_toleratesNullTraceIdDto() {
        MDC.put(AppConstants.MDC_TRACE_ID, "T-REST");

        String jobAUuid = "66666666-6666-6666-6666-666666666666";
        JobEntity jobA = jobEntity(jobAUuid);
        List<JobEntity> jobs = List.of(jobA);
        // DTO carries a null traceId (DLQ message had no traceId header)
        List<ConsumedDlqMessage> dtos = List.of(new ConsumedDlqMessage(jobAUuid, null));

        doAnswer(_ -> null).when(jobQueueService).publish(any(JobEntity.class));

        // When: must not throw NPE (Collectors.toMap would reject null values)
        jobRecoveryService.publishRevivedJobs(jobs, dtos);

        // Then: job was published and caller MDC restored
        verify(jobQueueService, times(1)).publish(jobA);
        assertEquals("T-REST", MDC.get(AppConstants.MDC_TRACE_ID),
                "caller MDC must be restored when a DTO had a null traceId");
    }

}
