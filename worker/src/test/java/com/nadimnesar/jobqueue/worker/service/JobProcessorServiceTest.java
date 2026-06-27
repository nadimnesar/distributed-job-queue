package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.enums.JobAckStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.worker.handler.JobHandler;
import com.nadimnesar.jobqueue.worker.handler.JobHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobProcessorServiceTest {

    private JobDependencyService jobDependencyService;
    private JobHandlerRegistry jobHandlerRegistry;
    private JobStateService jobStateService;
    private JobRepository jobRepository;
    private JobProcessorService jobProcessorService;

    @BeforeEach
    void setUp() {
        jobDependencyService = mock(JobDependencyService.class);
        jobHandlerRegistry = mock(JobHandlerRegistry.class);
        jobStateService = mock(JobStateService.class);
        jobRepository = mock(JobRepository.class);
        jobProcessorService = new JobProcessorService(jobDependencyService, jobHandlerRegistry, jobStateService, jobRepository);
    }

    @Test
    @DisplayName("AC2: processJob for a PENDING job with no dependencies invokes the handler and returns ACK")
    void processJob_pendingJobWithNoDependencies_invokesHandlerAndReturnsAck() throws Exception {
        // Given: a PENDING job with no unresolved dependency rows
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder()
                .id(jobId)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.PENDING)
                .build();
        JobHandler handler = mock(JobHandler.class);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobDependencyService.getDependencies(jobId)).thenReturn(Collections.emptySet());
        when(jobHandlerRegistry.getHandler(JobType.EMAIL_SENDING)).thenReturn(Optional.of(handler));
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        JobAckStatus result = jobProcessorService.processJob(jobId.toString());

        // Then
        assertEquals(JobAckStatus.ACK, result);
        verify(handler).handle(job);
        verify(jobStateService).handleCompletedJob(job);
        verify(jobStateService, never()).handleFailedJob(any(), any());
    }

    @Test
    @DisplayName("AC2 regression: processJob for a PENDING job with unresolved dependencies returns NACK")
    void processJob_pendingJobWithUnresolvedDependencies_returnsNack() {
        // Given: a PENDING job that still has unresolved dependency rows
        UUID jobId = UUID.randomUUID();
        UUID dependencyId = UUID.randomUUID();
        JobEntity job = JobEntity.builder()
                .id(jobId)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.PENDING)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobDependencyService.getDependencies(jobId)).thenReturn(Set.of(dependencyId));

        // When
        JobAckStatus result = jobProcessorService.processJob(jobId.toString());

        // Then: the worker must NACK so the job is delayed and retried
        assertEquals(JobAckStatus.NACK, result);
        verify(jobHandlerRegistry, never()).getHandler(any());
        verifyNoInteractions(jobHandlerRegistry);
    }
}
