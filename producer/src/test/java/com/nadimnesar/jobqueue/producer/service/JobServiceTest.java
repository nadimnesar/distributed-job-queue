package com.nadimnesar.jobqueue.producer.service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.nadimnesar.jobqueue.common.constant.Constants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.producer.config.MockTransactionSupport;
import com.nadimnesar.jobqueue.producer.config.TransactionSupportExtension;
import com.nadimnesar.jobqueue.producer.dto.CommonResponse;
import com.nadimnesar.jobqueue.producer.dto.request.JobRequest;
import com.nadimnesar.jobqueue.producer.service.impl.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith({MockitoExtension.class, TransactionSupportExtension.class})
public class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobDependencyService jobDependencyService;

    @Mock
    private JobQueueService jobQueueService;

    @InjectMocks
    private JobServiceImpl jobService;

    private JobRequest jobRequest;
    private JobEntity jobEntity;

    @BeforeEach
    void setUp() {
        var jobId = UuidCreator.getTimeOrderedEpoch();
        jobRequest = JobRequest.builder()
                .priority(JobPriority.HIGH)
                .type(JobType.EMAIL_SENDING)
                .payload("test payload")
                .maxAttemptCount(Constants.DEFAULT_MAXIMUM_ATTEMPT_COUNT)
                .dependencies(new HashSet<>())
                .build();

        jobEntity = JobEntity.builder()
                .id(jobId)
                .priority(JobPriority.HIGH)
                .type(JobType.EMAIL_SENDING)
                .status(JobStatus.PENDING)
                .payload("test payload")
                .maxAttemptCount(3)
                .build();
    }

    @Test
    @MockTransactionSupport
    void submitJob_Success() {
        when(jobRepository.save(any(JobEntity.class))).thenReturn(jobEntity);

        CommonResponse response = jobService.submitJob(jobRequest);

        assertEquals(HttpStatus.CREATED.value(), response.getCode());
        verify(jobRepository).save(any(JobEntity.class));
        verify(jobDependencyService).setDependencies(any(), any());
    }

    @Test
    void submitJob_WithValidPriorityHierarchy() {
        var dependencyId = UuidCreator.getTimeOrderedEpoch();
        var dependencyJob = JobEntity.builder()
                .id(dependencyId)
                .priority(JobPriority.HIGH)
                .status(JobStatus.PENDING)
                .build();

        jobRequest.setPriority(JobPriority.MEDIUM);
        jobRequest.setDependencies(Set.of(dependencyId));

        when(jobRepository.findAllById(any())).thenReturn(List.of(dependencyJob));
        when(jobRepository.save(any())).thenReturn(jobEntity);

        CommonResponse response = jobService.submitJob(jobRequest);

        assertEquals(HttpStatus.CREATED.value(), response.getCode());
        verify(jobDependencyService).setDependencies(any(), any());
    }

    @Test
    void getAllJobs_Success() {
        var jobs = List.of(jobEntity);
        var pageable = Pageable.ofSize(10).withPage(0);

        when(jobRepository.findAll(pageable)).thenReturn(new PageImpl<>(jobs));
        when(jobDependencyService.getDependents(any())).thenReturn(new HashSet<>());
        when(jobDependencyService.getDependencies(any())).thenReturn(new HashSet<>());

        CommonResponse response = jobService.getAllJobs(0, 10);

        assertEquals(HttpStatus.OK.value(), response.getCode());
        assertEquals(1, ((List<?>) response.getData()).size());
    }

    @Test
    void getAllJobs_NoJobsFound() {
        var pageable = Pageable.ofSize(10).withPage(0);
        when(jobRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        CommonResponse response = jobService.getAllJobs(0, 10);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getCode());
        assertEquals("No jobs found", response.getMessage());
    }

    @Test
    void getJobById_Success() {
        when(jobRepository.findById(jobEntity.getId())).thenReturn(Optional.of(jobEntity));
        when(jobDependencyService.getDependents(any())).thenReturn(new HashSet<>());
        when(jobDependencyService.getDependencies(any())).thenReturn(new HashSet<>());

        CommonResponse response = jobService.getJobById(jobEntity.getId().toString());

        assertEquals(HttpStatus.OK.value(), response.getCode());
    }

    @Test
    void getJobById_NotFound() {
        when(jobRepository.findById(any())).thenReturn(Optional.empty());

        CommonResponse response = jobService.getJobById(UuidCreator.getTimeOrderedEpoch().toString());

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getCode());
        assertEquals("No job found with given id.", response.getMessage());
    }

    @Test
    void cancelJob_Success() {
        jobEntity.setStatus(JobStatus.PENDING);
        when(jobRepository.findById(jobEntity.getId())).thenReturn(Optional.of(jobEntity));

        CommonResponse response = jobService.cancelJob(jobEntity.getId().toString());

        assertEquals(HttpStatus.ACCEPTED.value(), response.getCode());
        assertEquals("Job is cancelled successfully", response.getMessage());
        verify(jobRepository).save(any());
        verify(jobDependencyService).informDependents(jobEntity.getId());
    }

    @Test
    void cancelJob_AlreadyCanceled() {
        jobEntity.setStatus(JobStatus.CANCELED);
        when(jobRepository.findById(jobEntity.getId())).thenReturn(Optional.of(jobEntity));

        CommonResponse response = jobService.cancelJob(jobEntity.getId().toString());

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getCode());
        assertEquals("Job is already cancelled", response.getMessage());
    }

    @Test
    void cancelJob_AlreadyCompleted() {
        jobEntity.setStatus(JobStatus.COMPLETED);
        when(jobRepository.findById(jobEntity.getId())).thenReturn(Optional.of(jobEntity));

        CommonResponse response = jobService.cancelJob(jobEntity.getId().toString());

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getCode());
        assertEquals("Job is already completed", response.getMessage());
    }

    @Test
    void reviveAllDeadJobs_Success() {
        jobEntity.setStatus(JobStatus.DEAD);
        var revivedJobIds = List.of(jobEntity.getId().toString());
        var revivedJobs = List.of(jobEntity);

        when(jobQueueService.consumeDeadLetters()).thenReturn(revivedJobIds);
        when(jobRepository.findAllById(any())).thenReturn(revivedJobs);
        when(jobRepository.saveAll(any())).thenReturn(revivedJobs);

        CommonResponse response = jobService.reviveAllDeadJobs();

        assertEquals(HttpStatus.OK.value(), response.getCode());
        assertEquals("Successfully revived 1 dead jobs", response.getMessage());
        verify(jobRepository).saveAll(any());
    }

    @Test
    void reviveDeadJobById_Success() {
        jobEntity.setStatus(JobStatus.DEAD);
        when(jobRepository.findById(jobEntity.getId())).thenReturn(Optional.of(jobEntity));

        CommonResponse response = jobService.reviveDeadJobById(jobEntity.getId().toString());

        assertEquals(HttpStatus.OK.value(), response.getCode());
        assertEquals("Job revived successfully", response.getMessage());
        verify(jobRepository).save(any());
    }

    @Test
    void reviveDeadJobById_NotDeadState() {
        jobEntity.setStatus(JobStatus.FAILED);
        when(jobRepository.findById(jobEntity.getId())).thenReturn(Optional.of(jobEntity));

        CommonResponse response = jobService.reviveDeadJobById(jobEntity.getId().toString());

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getCode());
        assertEquals("Job is not in a dead state. Failed jobs are retried automatically.", response.getMessage());
    }

    @Test
    void reviveDeadJobs_NoAllDeadJobs() {
        when(jobQueueService.consumeDeadLetters()).thenReturn(List.of());

        CommonResponse response = jobService.reviveAllDeadJobs();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getCode());
        assertEquals("No dead jobs found to revive", response.getMessage());
    }

    @Test
    void submitJob_WithInvalidPriorityHierarchy() {
        var dependencyId = UuidCreator.getTimeOrderedEpoch();
        var dependencyJob = JobEntity.builder()
                .id(dependencyId)
                .priority(JobPriority.LOW)
                .status(JobStatus.PENDING)
                .build();

        jobRequest.setDependencies(Set.of(dependencyId));
        when(jobRepository.findAllById(any())).thenReturn(List.of(dependencyJob));

        assertThrows(IllegalArgumentException.class, () -> jobService.submitJob(jobRequest),
                "Job priority hierarchy violated: HIGH job cannot depend on LOW job");
    }

    @Test
    void submitJob_WithCanceledDependency() {
        var dependencyId = UuidCreator.getTimeOrderedEpoch();
        var dependencyJob = JobEntity.builder()
                .id(dependencyId)
                .priority(JobPriority.HIGH)
                .status(JobStatus.CANCELED)
                .build();

        jobRequest.setDependencies(Set.of(dependencyId));
        when(jobRepository.findAllById(any())).thenReturn(List.of(dependencyJob));

        assertThrows(IllegalArgumentException.class, () -> jobService.submitJob(jobRequest),
                "Cannot depend on canceled jobs");
    }

    @Test
    void submitJob_WithNonExistentDependency() {
        var nonExistentId = UuidCreator.getTimeOrderedEpoch();
        jobRequest.setDependencies(Set.of(nonExistentId));
        when(jobRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> jobService.submitJob(jobRequest),
                "Dependencies not found");
    }

    @Test
    void cancelJob_WithNonExistentId() {
        when(jobRepository.findById(any())).thenReturn(Optional.empty());

        CommonResponse response = jobService.cancelJob(UuidCreator.getTimeOrderedEpoch().toString());

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getCode());
        assertEquals("Job not found", response.getMessage());
    }

    @Test
    void getJobById_WithInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () -> jobService.getJobById("invalid-uuid"));
    }
}
