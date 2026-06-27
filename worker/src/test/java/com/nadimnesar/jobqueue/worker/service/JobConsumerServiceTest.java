package com.nadimnesar.jobqueue.worker.service;

import com.nadimnesar.jobqueue.common.constant.enums.JobAckStatus;
import com.nadimnesar.jobqueue.common.dto.ConsumedMessage;
import com.nadimnesar.jobqueue.common.service.JobQueueService;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobConsumerServiceTest {

    private static final String TRACE_ID = "T1";
    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_SPAN_ID_KEY = "spanId";

    private JobQueueService jobQueueService;
    private JobProcessorService jobProcessorService;
    private ExecutorService virtualThreadParTaskExecutor;
    private JobConsumerService jobConsumerService;

    @BeforeEach
    void setUp() {
        MDC.clear();
        jobQueueService = mock(JobQueueService.class);
        jobProcessorService = mock(JobProcessorService.class);
        virtualThreadParTaskExecutor = mock(ExecutorService.class);
        jobConsumerService = new JobConsumerService(jobQueueService, jobProcessorService, virtualThreadParTaskExecutor);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("AC3: when submit throws RejectedExecutionException, exception propagates and scheduler-thread MDC is cleared")
    void consumeJobs_whenSubmitThrows_clearsMdcAndPropagatesException() {
        // Given: consume() returns a message and (like the real consume()) sets traceId=T1
        // on the calling (scheduler) thread.
        ConsumedMessage message = ConsumedMessage.builder()
                .jobId("job-1")
                .traceId(TRACE_ID)
                .build();
        when(jobQueueService.consume()).thenAnswer(_ -> {
            TracingUtils.setTraceId(TRACE_ID);
            return message;
        });
        // And submit() throws RejectedExecutionException (e.g. shutdown in progress)
        when(virtualThreadParTaskExecutor.submit(any(Runnable.class)))
                .thenThrow(new RejectedExecutionException("shutdown"));

        // When / Then: the exception propagates
        RejectedExecutionException ex = assertThrows(RejectedExecutionException.class,
                () -> jobConsumerService.consumeJobs());

        // And the scheduler thread's MDC is cleared (traceId and spanId absent)
        assertNull(MDC.get(MDC_TRACE_ID_KEY),
                "traceId must be cleared on the scheduler thread after submit throws, but was: " + MDC.get(MDC_TRACE_ID_KEY));
        assertNull(MDC.get(MDC_SPAN_ID_KEY),
                "spanId must be absent on the scheduler thread after submit throws");
        // Sanity: the propagated exception is the one from submit()
        assertEquals("shutdown", ex.getMessage());
        // And the submitted task was never executed
        verifyNoInteractions(jobProcessorService);
    }

    @Test
    @DisplayName("AC3 happy path: submit succeeds -> scheduler-thread MDC cleared and submitted task runs")
    void consumeJobs_whenSubmitSucceeds_clearsSchedulerMdcAndRunsTask() {
        // Given: consume() returns a message and sets traceId=T1 on the scheduler thread
        ConsumedMessage message = ConsumedMessage.builder()
                .jobId("job-1")
                .traceId(TRACE_ID)
                .build();
        when(jobQueueService.consume()).thenAnswer(_ -> {
            TracingUtils.setTraceId(TRACE_ID);
            return message;
        });
        when(jobProcessorService.processJob("job-1")).thenReturn(JobAckStatus.ACK);
        // submit() succeeds and returns a completed future; capture the runnable so we
        // can verify the submitted task body runs.
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        Future<?> doneFuture = CompletableFuture.completedFuture(null);
        when(virtualThreadParTaskExecutor.submit(taskCaptor.capture())).thenAnswer(_ -> doneFuture);

        // When
        jobConsumerService.consumeJobs();

        // Then: scheduler thread MDC is cleared (happy-path behavior preserved)
        assertNull(MDC.get(MDC_TRACE_ID_KEY),
                "traceId must be cleared on the scheduler thread after a successful submit");
        assertNull(MDC.get(MDC_SPAN_ID_KEY),
                "spanId must be absent on the scheduler thread after a successful submit");

        // And the submitted task runs: executing the captured runnable drives the
        // processor + ack path (proving the task body is intact).
        Runnable submittedTask = taskCaptor.getValue();
        assertNotNull(submittedTask, "a task must have been submitted");
        submittedTask.run();
        verify(jobProcessorService).processJob("job-1");
        verify(jobQueueService).ack(message);
        verify(jobQueueService, never()).nack(any());
    }

    @Test
    @DisplayName("AC3 null path: consume returns null -> method returns early and scheduler-thread MDC is untouched")
    void consumeJobs_whenConsumeReturnsNull_leavesMdcUntouched() {
        // Given: a pre-existing MDC sentinel value on the scheduler thread (simulating
        // any prior MDC state). The null path must not clear it.
        MDC.put(MDC_TRACE_ID_KEY, "SENTINEL");
        MDC.put(MDC_SPAN_ID_KEY, "SENTINEL_SPAN");
        when(jobQueueService.consume()).thenReturn(null);

        // When
        jobConsumerService.consumeJobs();

        // Then: MDC is untouched (clearTracing() must not run on the null path)
        assertEquals("SENTINEL", MDC.get(MDC_TRACE_ID_KEY),
                "scheduler-thread traceId must be untouched on the null-return path");
        assertEquals("SENTINEL_SPAN", MDC.get(MDC_SPAN_ID_KEY),
                "scheduler-thread spanId must be untouched on the null-return path");
        // And no task was submitted and no processing occurred
        verify(virtualThreadParTaskExecutor, never()).submit(any(Runnable.class));
        verifyNoInteractions(jobProcessorService);
    }
}
