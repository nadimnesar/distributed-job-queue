package com.nadimnesar.jobqueue.producer.service;

import com.github.f4b6a3.uuid.UuidCreator;
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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.http.HttpStatus;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                .maxAttemptCount(3)
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
}
