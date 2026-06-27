package com.nadimnesar.jobqueue.producer.service;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceTest {

    private JobQueueService jobQueueService;
    private JobDependencyService jobDependencyService;
    private JobRecoveryService jobRecoveryService;
    private JobRepository jobRepository;
    private JobService jobService;

    private MockedStatic<TransactionSynchronizationManager> transactionSynchronizationManagerMock;

    @BeforeEach
    void setUp() {
        jobQueueService = mock(JobQueueService.class);
        jobDependencyService = mock(JobDependencyService.class);
        jobRecoveryService = mock(JobRecoveryService.class);
        jobRepository = mock(JobRepository.class);
        jobService = new JobService(jobQueueService, jobDependencyService, jobRecoveryService, jobRepository);

        transactionSynchronizationManagerMock = mockStatic(TransactionSynchronizationManager.class);
        transactionSynchronizationManagerMock.when(
                        () -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                .thenAnswer(_ -> null);
    }

    @AfterEach
    void tearDown() {
        transactionSynchronizationManagerMock.close();
    }

    @Test
    @DisplayName("AC1: submitJob with only a COMPLETED dependency does not persist a dependency row")
    void submitJob_withOnlyCompletedDependency_doesNotPersistDependencyRow() {
        // Given
        UUID completedJobId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        JobEntity completedJob = dependencyJob(completedJobId, JobStatus.COMPLETED);
        when(jobRepository.findAllById(Set.of(completedJobId))).thenReturn(List.of(completedJob));

        UUID savedJobId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        JobEntity savedJob = newJob(savedJobId);
        when(jobRepository.save(any(JobEntity.class))).thenReturn(savedJob);

        JobRequest request = jobRequestWithDependencies(Set.of(completedJobId));

        // When
        jobService.submitJob(request);

        // Then
        ArgumentCaptor<Set<UUID>> dependenciesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(jobDependencyService).setDependencies(eq(savedJobId), dependenciesCaptor.capture());
        assertTrue(dependenciesCaptor.getValue().isEmpty(),
                "Expected no dependency rows for an already-COMPLETED dependency");
    }

    @Test
    @DisplayName("AC3: submitJob with a PENDING dependency persists the dependency row")
    void submitJob_withPendingDependency_persistsDependencyRow() {
        // Given
        UUID pendingJobId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        JobEntity pendingJob = dependencyJob(pendingJobId, JobStatus.PENDING);
        when(jobRepository.findAllById(Set.of(pendingJobId))).thenReturn(List.of(pendingJob));

        UUID savedJobId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        JobEntity savedJob = newJob(savedJobId);
        when(jobRepository.save(any(JobEntity.class))).thenReturn(savedJob);

        JobRequest request = jobRequestWithDependencies(Set.of(pendingJobId));

        // When
        jobService.submitJob(request);

        // Then
        ArgumentCaptor<Set<UUID>> dependenciesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(jobDependencyService).setDependencies(eq(savedJobId), dependenciesCaptor.capture());
        assertEquals(Set.of(pendingJobId), dependenciesCaptor.getValue(),
                "Expected PENDING dependency to be persisted");
    }

    @Test
    @DisplayName("AC4: submitJob with mixed COMPLETED and PENDING dependencies persists only the PENDING row")
    void submitJob_withMixedCompletedAndPendingDependencies_persistsOnlyPendingRow() {
        // Given
        UUID completedJobId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID pendingJobId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        JobEntity completedJob = dependencyJob(completedJobId, JobStatus.COMPLETED);
        JobEntity pendingJob = dependencyJob(pendingJobId, JobStatus.PENDING);
        when(jobRepository.findAllById(Set.of(completedJobId, pendingJobId)))
                .thenReturn(List.of(completedJob, pendingJob));

        UUID savedJobId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        JobEntity savedJob = newJob(savedJobId);
        when(jobRepository.save(any(JobEntity.class))).thenReturn(savedJob);

        JobRequest request = jobRequestWithDependencies(Set.of(completedJobId, pendingJobId));

        // When
        jobService.submitJob(request);

        // Then
        ArgumentCaptor<Set<UUID>> dependenciesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(jobDependencyService).setDependencies(eq(savedJobId), dependenciesCaptor.capture());
        assertEquals(Set.of(pendingJobId), dependenciesCaptor.getValue(),
                "Expected only the PENDING dependency to be persisted");
    }

    @ParameterizedTest
    @EnumSource(value = JobStatus.class, names = {"CANCELED", "DEAD"})
    @DisplayName("AC5: submitJob with a CANCELED or DEAD dependency rejects submission with the existing error message")
    void submitJob_withCanceledOrDeadDependency_rejectsSubmission(JobStatus invalidStatus) {
        // Given
        UUID invalidJobId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        JobEntity invalidJob = dependencyJob(invalidJobId, invalidStatus);
        when(jobRepository.findAllById(Set.of(invalidJobId))).thenReturn(List.of(invalidJob));

        JobRequest request = jobRequestWithDependencies(Set.of(invalidJobId));

        // When / Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> jobService.submitJob(request));
        assertEquals("Cannot depend on canceled/dead jobs: [" + invalidJobId + "]", exception.getMessage());
        verify(jobDependencyService, never()).setDependencies(any(), any());
        verify(jobRepository, never()).save(any(JobEntity.class));
    }

    private JobEntity dependencyJob(UUID id, JobStatus status) {
        return JobEntity.builder()
                .id(id)
                .status(status)
                .priority(JobPriority.MEDIUM)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .build();
    }

    private JobEntity newJob(UUID id) {
        return JobEntity.builder()
                .id(id)
                .priority(JobPriority.MEDIUM)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .build();
    }

    private JobRequest jobRequestWithDependencies(Set<UUID> dependencies) {
        return JobRequest.builder()
                .priority(JobPriority.MEDIUM)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .dependencies(dependencies)
                .build();
    }
}
