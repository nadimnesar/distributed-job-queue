package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataAccessException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class JobRetryServiceTest {

    private JobQueueService jobQueueService;
    private JobRepository jobRepository;
    private JobDependencyService jobDependencyService;
    private JobRetryService jobRetryService;

    @BeforeEach
    void setUp() {
        jobQueueService = mock(JobQueueService.class);
        jobRepository = mock(JobRepository.class);
        jobDependencyService = mock(JobDependencyService.class);
        jobRetryService = new JobRetryService(jobQueueService, jobRepository, jobDependencyService);
    }

    @Test
    @DisplayName("AC1: retried job with one COMPLETED dependency row has stale row deleted and is republished with attempt count incremented")
    void onRetryMessage_oneCompletedDependencyRow_cleansUpAndRepublishesWithIncrementedAttemptCount() {
        // Given: a FAILED retry job with one stale COMPLETED dependency row
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder()
                .id(jobId)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.FAILED)
                .priority(JobPriority.HIGH)
                .attemptCount(1)
                .maxAttemptCount(3)
                .build();

        Channel channel = mock(Channel.class);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeaders(Map.of(AppConstants.HEADER_TRACE_ID, "trace-1"));
        messageProperties.setDeliveryTag(42L);
        Message message = new Message(jobId.toString().getBytes(), messageProperties);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobDependencyService.cleanupStaleDependencies(jobId)).thenReturn(1);
        when(jobDependencyService.getDependencies(jobId)).thenReturn(Collections.emptySet());
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        jobRetryService.onRetryMessage(jobId.toString(), channel, message);

        // Then: cleanup is invoked before checking remaining dependencies
        InOrder inOrder = inOrder(jobDependencyService);
        inOrder.verify(jobDependencyService).cleanupStaleDependencies(jobId);
        inOrder.verify(jobDependencyService).getDependencies(jobId);

        // And: the job is saved as PENDING with the attempt count incremented
        ArgumentCaptor<JobEntity> savedJobCaptor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository).save(savedJobCaptor.capture());
        JobEntity savedJob = savedJobCaptor.getValue();
        assertEquals(JobStatus.PENDING, savedJob.getStatus());
        assertEquals(2, savedJob.getAttemptCount());
        assertNull(savedJob.getStartedAt());
        assertNull(savedJob.getCompletedAt());
        assertNull(savedJob.getResult());

        // And: the job is published exactly once and the message is acknowledged exactly once
        verify(jobQueueService).publish(savedJob);
        verify(jobQueueService).ack(channel, 42L, jobId.toString());
        verify(jobQueueService, never()).moveToDeadLetter(anyString());
        verifyNoMoreInteractions(jobQueueService);
    }

    @Test
    @DisplayName("AC2: retried job with one orphan dependency row has orphan deleted and is republished with attempt count incremented")
    void onRetryMessage_oneOrphanDependencyRow_cleansUpAndRepublishesWithIncrementedAttemptCount() {
        // Given: a FAILED retry job with one orphan dependency row
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder()
                .id(jobId)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.FAILED)
                .priority(JobPriority.HIGH)
                .attemptCount(1)
                .maxAttemptCount(3)
                .build();

        Channel channel = mock(Channel.class);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeaders(Map.of(AppConstants.HEADER_TRACE_ID, "trace-2"));
        messageProperties.setDeliveryTag(42L);
        Message message = new Message(jobId.toString().getBytes(), messageProperties);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobDependencyService.cleanupStaleDependencies(jobId)).thenReturn(1);
        when(jobDependencyService.getDependencies(jobId)).thenReturn(Collections.emptySet());
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        jobRetryService.onRetryMessage(jobId.toString(), channel, message);

        // Then: cleanup is invoked before checking remaining dependencies
        InOrder inOrder = inOrder(jobDependencyService);
        inOrder.verify(jobDependencyService).cleanupStaleDependencies(jobId);
        inOrder.verify(jobDependencyService).getDependencies(jobId);

        // And: the job is saved as PENDING with the attempt count incremented
        ArgumentCaptor<JobEntity> savedJobCaptor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository).save(savedJobCaptor.capture());
        JobEntity savedJob = savedJobCaptor.getValue();
        assertEquals(JobStatus.PENDING, savedJob.getStatus());
        assertEquals(2, savedJob.getAttemptCount());
        assertNull(savedJob.getStartedAt());
        assertNull(savedJob.getCompletedAt());
        assertNull(savedJob.getResult());

        // And: the job is published exactly once and the message is acknowledged exactly once
        verify(jobQueueService).publish(savedJob);
        verify(jobQueueService).ack(channel, 42L, jobId.toString());
        verify(jobQueueService, never()).moveToDeadLetter(anyString());
        verifyNoMoreInteractions(jobQueueService);
    }

    @Test
    @DisplayName("AC3: retried job with one stale COMPLETED row and one non-stale row deletes only stale and republishes without incrementing attempt count")
    void onRetryMessage_mixedStaleAndNonStaleDependencies_deletesOnlyStaleAndRepublishesWithoutIncrementingAttemptCount() {
        // Given: a FAILED retry job with one stale COMPLETED dependency and one still-pending dependency
        UUID jobId = UUID.randomUUID();
        UUID pendingDependencyId = UUID.randomUUID();
        JobEntity job = JobEntity.builder()
                .id(jobId)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.FAILED)
                .priority(JobPriority.HIGH)
                .attemptCount(1)
                .maxAttemptCount(3)
                .build();

        Channel channel = mock(Channel.class);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeaders(Map.of(AppConstants.HEADER_TRACE_ID, "trace-3"));
        messageProperties.setDeliveryTag(42L);
        Message message = new Message(jobId.toString().getBytes(), messageProperties);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobDependencyService.cleanupStaleDependencies(jobId)).thenReturn(1);
        when(jobDependencyService.getDependencies(jobId)).thenReturn(Set.of(pendingDependencyId));
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        jobRetryService.onRetryMessage(jobId.toString(), channel, message);

        // Then: cleanup is invoked before checking remaining dependencies
        InOrder inOrder = inOrder(jobDependencyService);
        inOrder.verify(jobDependencyService).cleanupStaleDependencies(jobId);
        inOrder.verify(jobDependencyService).getDependencies(jobId);

        // And: the job is saved as PENDING with the attempt count unchanged
        ArgumentCaptor<JobEntity> savedJobCaptor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository).save(savedJobCaptor.capture());
        JobEntity savedJob = savedJobCaptor.getValue();
        assertEquals(JobStatus.PENDING, savedJob.getStatus());
        assertEquals(1, savedJob.getAttemptCount());
        assertNull(savedJob.getStartedAt());
        assertNull(savedJob.getCompletedAt());
        assertNull(savedJob.getResult());

        // And: the job is published exactly once and the message is acknowledged exactly once
        verify(jobQueueService).publish(savedJob);
        verify(jobQueueService).ack(channel, 42L, jobId.toString());
        verify(jobQueueService, never()).moveToDeadLetter(anyString());
        verifyNoMoreInteractions(jobQueueService);
    }

    @Test
    @DisplayName("AC4: retried job with no dependency rows performs harmless cleanup and republishes with attempt count incremented")
    void onRetryMessage_noDependencyRows_harmlessCleanupAndRepublishesWithIncrementedAttemptCount() {
        // Given: a FAILED retry job with no dependency rows
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder()
                .id(jobId)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.FAILED)
                .priority(JobPriority.HIGH)
                .attemptCount(1)
                .maxAttemptCount(3)
                .build();

        Channel channel = mock(Channel.class);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeaders(Map.of(AppConstants.HEADER_TRACE_ID, "trace-4"));
        messageProperties.setDeliveryTag(42L);
        Message message = new Message(jobId.toString().getBytes(), messageProperties);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobDependencyService.cleanupStaleDependencies(jobId)).thenReturn(0);
        when(jobDependencyService.getDependencies(jobId)).thenReturn(Collections.emptySet());
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        jobRetryService.onRetryMessage(jobId.toString(), channel, message);

        // Then: cleanup is invoked before checking remaining dependencies
        InOrder inOrder = inOrder(jobDependencyService);
        inOrder.verify(jobDependencyService).cleanupStaleDependencies(jobId);
        inOrder.verify(jobDependencyService).getDependencies(jobId);

        // And: the job is saved as PENDING with the attempt count incremented
        ArgumentCaptor<JobEntity> savedJobCaptor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository).save(savedJobCaptor.capture());
        JobEntity savedJob = savedJobCaptor.getValue();
        assertEquals(JobStatus.PENDING, savedJob.getStatus());
        assertEquals(2, savedJob.getAttemptCount());
        assertNull(savedJob.getStartedAt());
        assertNull(savedJob.getCompletedAt());
        assertNull(savedJob.getResult());

        // And: the job is published exactly once and the message is acknowledged exactly once
        verify(jobQueueService).publish(savedJob);
        verify(jobQueueService).ack(channel, 42L, jobId.toString());
        verify(jobQueueService, never()).moveToDeadLetter(anyString());
        verifyNoMoreInteractions(jobQueueService);
    }

    @Test
    @DisplayName("AC5: retried job with only non-stale dependency rows does not delete and republishes without incrementing attempt count")
    void onRetryMessage_onlyNonStaleDependencies_noRowsDeletedAndRepublishesWithoutIncrementingAttemptCount() {
        // Given: a FAILED retry job with only non-stale dependency rows
        UUID jobId = UUID.randomUUID();
        UUID pendingDependencyId = UUID.randomUUID();
        JobEntity job = JobEntity.builder()
                .id(jobId)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.FAILED)
                .priority(JobPriority.HIGH)
                .attemptCount(1)
                .maxAttemptCount(3)
                .build();

        Channel channel = mock(Channel.class);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeaders(Map.of(AppConstants.HEADER_TRACE_ID, "trace-5"));
        messageProperties.setDeliveryTag(42L);
        Message message = new Message(jobId.toString().getBytes(), messageProperties);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobDependencyService.cleanupStaleDependencies(jobId)).thenReturn(0);
        when(jobDependencyService.getDependencies(jobId)).thenReturn(Set.of(pendingDependencyId));
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        jobRetryService.onRetryMessage(jobId.toString(), channel, message);

        // Then: cleanup is invoked before checking remaining dependencies
        InOrder inOrder = inOrder(jobDependencyService);
        inOrder.verify(jobDependencyService).cleanupStaleDependencies(jobId);
        inOrder.verify(jobDependencyService).getDependencies(jobId);

        // And: the job is saved as PENDING with the attempt count unchanged
        ArgumentCaptor<JobEntity> savedJobCaptor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository).save(savedJobCaptor.capture());
        JobEntity savedJob = savedJobCaptor.getValue();
        assertEquals(JobStatus.PENDING, savedJob.getStatus());
        assertEquals(1, savedJob.getAttemptCount());
        assertNull(savedJob.getStartedAt());
        assertNull(savedJob.getCompletedAt());
        assertNull(savedJob.getResult());

        // And: the job is published exactly once and the message is acknowledged exactly once
        verify(jobQueueService).publish(savedJob);
        verify(jobQueueService).ack(channel, 42L, jobId.toString());
        verify(jobQueueService, never()).moveToDeadLetter(anyString());
        verifyNoMoreInteractions(jobQueueService);
    }

    @Test
    @DisplayName("AC6: retried job cleanup throws transient DB error and message is NACKed/redelivered without state change")
    void onRetryMessage_cleanupThrowsTransientDbError_jobStateUnchangedAndNotAcked() {
        // Given: a FAILED retry job whose stale dependency cleanup fails with a transient DB error
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder()
                .id(jobId)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .status(JobStatus.FAILED)
                .priority(JobPriority.HIGH)
                .attemptCount(1)
                .maxAttemptCount(3)
                .build();

        Channel channel = mock(Channel.class);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeaders(Map.of(AppConstants.HEADER_TRACE_ID, "trace-6"));
        messageProperties.setDeliveryTag(42L);
        Message message = new Message(jobId.toString().getBytes(), messageProperties);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobDependencyService.cleanupStaleDependencies(jobId))
                .thenThrow(new DataAccessException("transient DB failure") {});

        // When
        jobRetryService.onRetryMessage(jobId.toString(), channel, message);

        // Then: the job is never saved and no queue operation is performed
        verify(jobRepository, never()).save(any(JobEntity.class));
        verify(jobQueueService, never()).publish(any(JobEntity.class));
        verify(jobQueueService, never()).ack(any(Channel.class), anyLong(), anyString());
        verify(jobQueueService, never()).moveToDeadLetter(anyString());
        verifyNoMoreInteractions(jobQueueService);
    }
}
