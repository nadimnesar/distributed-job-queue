package com.nadimnesar.jobqueue.common.service;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.constant.RabbitMQConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.dto.ConsumedDlqMessage;
import com.nadimnesar.jobqueue.common.dto.ConsumedMessage;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.rabbitmq.client.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobQueueServiceTest {

    private RabbitTemplate rabbitTemplate;
    private RabbitAdmin rabbitAdmin;
    private ConnectionFactory connectionFactory;
    private JobQueueService jobQueueService;

    @BeforeEach
    void setUp() {
        MDC.clear();
        rabbitTemplate = mock(RabbitTemplate.class);
        rabbitAdmin = mock(RabbitAdmin.class);
        connectionFactory = mock(ConnectionFactory.class);
        jobQueueService = new JobQueueService(rabbitTemplate, rabbitAdmin, connectionFactory);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private Message dlqMessage(String jobId, String traceId) {
        MessageProperties props = new MessageProperties();
        if (traceId != null) {
            props.setHeader(AppConstants.HEADER_TRACE_ID, traceId);
        }
        return new Message(jobId.getBytes(StandardCharsets.UTF_8), props);
    }

    private GetResponse queueGetResponse(String jobId, String traceId, String queue, long deliveryTag) {
        Map<String, Object> headers = new HashMap<>();
        if (traceId != null) {
            headers.put(AppConstants.HEADER_TRACE_ID, traceId);
        }
        AMQP.BasicProperties amqpProps = new AMQP.BasicProperties.Builder().headers(headers).build();
        Envelope envelope = new Envelope(deliveryTag, false, RabbitMQConstants.EXCHANGE, queue);
        return new GetResponse(envelope, amqpProps, jobId.getBytes(StandardCharsets.UTF_8), 0);
    }

    @Test
    @DisplayName("AC1: consumeDeadLetters preserves caller MDC traceId/spanId (BUG-1 regression)")
    void consumeDeadLetters_preservesCallerMdc() {
        // Given: caller (REST endpoint) has its own tracing context
        MDC.put(AppConstants.MDC_TRACE_ID, "T-REST");
        MDC.put(AppConstants.MDC_SPAN_ID, "S-REST");

        // and the DLQ holds one message carrying traceId=T-A
        when(rabbitTemplate.receive(RabbitMQConstants.QUEUE_DLQ))
                .thenReturn(dlqMessage("jobA", "T-A"))
                .thenReturn(null);

        // When
        jobQueueService.consumeDeadLetters();

        // Then: caller's MDC must be intact (before fix: finally{clearTracing()} wiped it)
        assertEquals("T-REST", MDC.get(AppConstants.MDC_TRACE_ID),
                "caller traceId must survive consumeDeadLetters (BUG-1)");
        assertEquals("S-REST", MDC.get(AppConstants.MDC_SPAN_ID),
                "caller spanId must survive consumeDeadLetters (BUG-1)");
    }

    @Test
    @DisplayName("AC2: consumeDeadLetters captures per-job traceId from each DLQ message (BUG-2 regression)")
    void consumeDeadLetters_capturesPerJobTraceId() {
        // Given: two DLQ messages, each with its own traceId
        when(rabbitTemplate.receive(RabbitMQConstants.QUEUE_DLQ))
                .thenReturn(dlqMessage("jobA", "T-A"))
                .thenReturn(dlqMessage("jobB", "T-B"))
                .thenReturn(null);

        // When
        List<ConsumedDlqMessage> result = jobQueueService.consumeDeadLetters();

        // Then: each returned DTO keeps its OWN traceId
        // (before fix: only the first traceId was captured -> all DTOs got T-A)
        assertEquals(2, result.size());
        assertEquals(new ConsumedDlqMessage("jobA", "T-A"), result.get(0),
                "first DLQ message must keep its own traceId (BUG-2)");
        assertEquals(new ConsumedDlqMessage("jobB", "T-B"), result.get(1),
                "second DLQ message must keep its own traceId (BUG-2)");
    }

    @Test
    @DisplayName("AC2: consumeDeadLetters returns empty list when DLQ is empty")
    void consumeDeadLetters_returnsEmptyWhenNoMessages() {
        when(rabbitTemplate.receive(RabbitMQConstants.QUEUE_DLQ)).thenReturn(null);

        List<ConsumedDlqMessage> result = jobQueueService.consumeDeadLetters();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("AC7: consume() propagates traceId from header and regenerates spanId (never reads spanId header)")
    void consume_propagatesTraceIdAndRegeneratesSpanId() throws Exception {
        // Given: a produced message carrying traceId=T and a (hypothetical) spanId=S-prod header.
        // The tracing model says spanId must NOT be read from the header; consume() must
        // regenerate a fresh spanId via setNewSpanId().
        String jobId = "jobX";
        Map<String, Object> headers = new HashMap<>();
        headers.put(AppConstants.HEADER_TRACE_ID, "T");
        headers.put("spanId", "S-prod"); // injected; consume() must ignore this header

        AMQP.BasicProperties amqpProps = new AMQP.BasicProperties.Builder()
                .headers(headers)
                .build();
        Envelope envelope = new Envelope(1L, false, RabbitMQConstants.EXCHANGE, RabbitMQConstants.QUEUE_HIGH);
        GetResponse getResponse = new GetResponse(
                envelope,
                amqpProps,
                jobId.getBytes(StandardCharsets.UTF_8),
                0);

        Connection connection = mock(Connection.class);
        Channel channel = mock(Channel.class);
        lenient().when(connectionFactory.createConnection()).thenReturn(connection);
        lenient().when(connection.createChannel(false)).thenReturn(channel);
        // First queue (HIGH) returns the message; loop returns immediately.
        when(channel.basicGet(RabbitMQConstants.QUEUE_HIGH, false)).thenReturn(getResponse);

        // When
        ConsumedMessage consumed = jobQueueService.consume();

        // Then: a message was consumed
        assertNotNull(consumed, "consume() should return the message from the HIGH queue");
        assertEquals(jobId, consumed.jobId());

        // traceId is propagated end-to-end from the message header
        assertEquals("T", MDC.get(AppConstants.MDC_TRACE_ID),
                "worker MDC must carry the traceId from the message header (AC7)");

        // spanId is regenerated, NOT copied from the header
        String workerSpanId = MDC.get(AppConstants.MDC_SPAN_ID);
        assertNotNull(workerSpanId, "worker must have a fresh spanId (setNewSpanId)");
        assertNotEquals("S-prod", workerSpanId,
                "spanId must be regenerated, never read from a message header (AC7)");

        // The consumed DTO also carries the propagated traceId (and no spanId field exists)
        assertEquals("T", consumed.traceId());
    }

    @Test
    @DisplayName("AC4: consume() does not leak a failed iteration's traceId into the next queue iteration (BUG-4)")
    void consume_doesNotLeakTraceIdAcrossIterations() throws Exception {
        // Given: basicGet(HIGH) returns a message with traceId=T-high, then the
        // success path after setTraceId throws (simulated by stubbing the Lombok
        // builder to throw — this fires AFTER setTraceId/setNewSpanId ran, exactly
        // reproducing the BUG-4 leak window). basicGet(MEDIUM) returns a message
        // with traceId=T-med; we capture the MDC at the moment MEDIUM's basicGet is
        // invoked (i.e. BEFORE MEDIUM's own setTraceId overwrites it). basicGet(LOW)
        // returns null.
        try (MockedStatic<ConsumedMessage> mockedBuilder = Mockito.mockStatic(ConsumedMessage.class)) {
            mockedBuilder.when(ConsumedMessage::builder)
                    .thenThrow(new RuntimeException("builder boom"));

            GetResponse highResp = queueGetResponse("jobHigh", "T-high",
                    RabbitMQConstants.QUEUE_HIGH, 1L);
            GetResponse medResp = queueGetResponse("jobMed", "T-med",
                    RabbitMQConstants.QUEUE_MEDIUM, 2L);

            final String[] capturedTraceAtMediumBasicGet = {null};

            Connection connection = mock(Connection.class);
            Channel channel = mock(Channel.class);
            lenient().when(connectionFactory.createConnection()).thenReturn(connection);
            lenient().when(connection.createChannel(false)).thenReturn(channel);
            when(channel.basicGet(RabbitMQConstants.QUEUE_HIGH, false)).thenReturn(highResp);
            when(channel.basicGet(RabbitMQConstants.QUEUE_MEDIUM, false)).thenAnswer(_ -> {
                // Snapshot the MDC the consume() thread sees when ENTERING the
                // MEDIUM iteration (before MEDIUM's setTraceId runs).
                capturedTraceAtMediumBasicGet[0] = MDC.get(AppConstants.MDC_TRACE_ID);
                return medResp;
            });
            when(channel.basicGet(RabbitMQConstants.QUEUE_LOW, false)).thenReturn(null);

            // When: caller MDC is empty (set in setUp). HIGH sets T-high then throws;
            // the loop must NOT carry T-high into the MEDIUM iteration.
            ConsumedMessage result = jobQueueService.consume();

            // Then: consume() returns null (every queue failed/empty), and the MDC
            // observed at the START of the MEDIUM iteration is NOT the leaked T-high.
            // (Before fix: the catch never restored MDC, so T-high leaked into MEDIUM.)
            assertNull(result, "consume() returns null when every queue fails/empties");
            assertNull(capturedTraceAtMediumBasicGet[0],
                    "MEDIUM iteration must not inherit HIGH's leaked traceId (BUG-4)");
        }
    }

    @Test
    @DisplayName("AC4: consume() returns null without contaminating caller MDC when all queues fail (BUG-4)")
    void consume_allQueuesFail_returnsNullAndPreservesCallerMdc() throws Exception {
        // Given: caller has its own tracing context; every queue returns a message
        // (so setTraceId runs on each iteration) but the success path throws each
        // time (stubbed builder). consume() must return null AND leave the caller's
        // MDC untouched (no leaked T-high/T-med/T-low from the failed iterations).
        MDC.put(AppConstants.MDC_TRACE_ID, "T-caller");
        MDC.put(AppConstants.MDC_SPAN_ID, "S-caller");

        try (MockedStatic<ConsumedMessage> mockedBuilder = Mockito.mockStatic(ConsumedMessage.class)) {
            mockedBuilder.when(ConsumedMessage::builder)
                    .thenThrow(new RuntimeException("builder boom"));

            GetResponse highResp = queueGetResponse("jobHigh", "T-high",
                    RabbitMQConstants.QUEUE_HIGH, 1L);
            GetResponse medResp = queueGetResponse("jobMed", "T-med",
                    RabbitMQConstants.QUEUE_MEDIUM, 2L);
            GetResponse lowResp = queueGetResponse("jobLow", "T-low",
                    RabbitMQConstants.QUEUE_LOW, 3L);

            Connection connection = mock(Connection.class);
            Channel channel = mock(Channel.class);
            lenient().when(connectionFactory.createConnection()).thenReturn(connection);
            lenient().when(connection.createChannel(false)).thenReturn(channel);
            when(channel.basicGet(RabbitMQConstants.QUEUE_HIGH, false)).thenReturn(highResp);
            when(channel.basicGet(RabbitMQConstants.QUEUE_MEDIUM, false)).thenReturn(medResp);
            when(channel.basicGet(RabbitMQConstants.QUEUE_LOW, false)).thenReturn(lowResp);

            // When
            ConsumedMessage result = jobQueueService.consume();

            // Then: consume() returns null AND caller MDC is intact.
            // (Before fix: the last failed iteration's traceId T-low leaked out to
            // the caller, so MDC.traceId == "T-low" != "T-caller".)
            assertNull(result, "consume() returns null when every queue fails");
            assertEquals("T-caller", MDC.get(AppConstants.MDC_TRACE_ID),
                    "caller traceId must survive a fully-failed consume() (BUG-4)");
            assertEquals("S-caller", MDC.get(AppConstants.MDC_SPAN_ID),
                    "caller spanId must survive a fully-failed consume() (BUG-4)");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> traceIdMap() throws Exception {
        var field = JobQueueService.class.getDeclaredField("traceIdMap");
        field.setAccessible(true);
        return (Map<String, String>) field.get(jobQueueService);
    }

    private JobEntity jobWithId(UUID id) {
        JobEntity job = new JobEntity();
        job.setId(id);
        job.setPriority(JobPriority.MEDIUM);
        return job;
    }

    @Test
    @DisplayName("AC6: connection-lost callback evicts orphaned traceIdMap entries (onClose + onShutDown) (BUG-6 regression)")
    void connectionLost_evictsOrphanedTraceIdMapEntries() throws Exception {
        // Given: publisher callbacks initialized (registers the ConnectionListener)
        jobQueueService.initPublisherCallbacks();
        ArgumentCaptor<ConnectionListener> captor = ArgumentCaptor.forClass(ConnectionListener.class);
        verify(connectionFactory).addConnectionListener(captor.capture());
        ConnectionListener listener = captor.getValue();

        // and a job is published with MDC traceId=T1 -> entry recorded in traceIdMap
        MDC.put(AppConstants.MDC_TRACE_ID, "T1");
        UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        jobQueueService.publish(jobWithId(jobId));
        assertEquals("T1", traceIdMap().get(jobId.toString()),
                "publish() should record jobId -> traceId in traceIdMap");
        assertFalse(traceIdMap().isEmpty(),
                "precondition: traceIdMap holds the in-flight entry");

        // When: the broker connection drops (graceful close) before any confirm fires
        listener.onClose(mock(Connection.class));

        // Then: the orphaned entry is evicted.
        // (Before fix: no listener registered -> addConnectionListener never invoked ->
        //  verify() fails -> entry would remain indefinitely -> test FAILS.)
        assertTrue(traceIdMap().isEmpty(),
                "onClose must evict orphaned traceIdMap entries so the map cannot grow unbounded (BUG-6)");

        // And: a force-close (onShutDown) also evicts (covers the force-close path)
        MDC.put(AppConstants.MDC_TRACE_ID, "T2");
        UUID jobId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jobQueueService.publish(jobWithId(jobId2));
        assertEquals("T2", traceIdMap().get(jobId2.toString()),
                "precondition: second publish records a new in-flight entry");
        listener.onShutDown(new ShutdownSignalException(true, false, null, null));
        assertTrue(traceIdMap().isEmpty(),
                "onShutDown must evict orphaned traceIdMap entries on force-close (BUG-6)");
    }

    @Test
    @DisplayName("AC6: traceIdMap does not grow unbounded across repeated connection failures (BUG-6)")
    void connectionLost_repeatedFailuresDoNotGrowTraceIdMapUnbounded() throws Exception {
        // Given: listener registered
        jobQueueService.initPublisherCallbacks();
        ArgumentCaptor<ConnectionListener> captor = ArgumentCaptor.forClass(ConnectionListener.class);
        verify(connectionFactory).addConnectionListener(captor.capture());
        ConnectionListener listener = captor.getValue();

        MDC.put(AppConstants.MDC_TRACE_ID, "T-fixed");

        int rounds = 5;
        int jobsPerRound = 4;
        int maxObservedAfterClear = 0;
        for (int r = 0; r < rounds; r++) {
            for (int j = 0; j < jobsPerRound; j++) {
                jobQueueService.publish(jobWithId(UUID.nameUUIDFromBytes(
                        ("r" + r + "j" + j).getBytes(StandardCharsets.UTF_8))));
            }
            // simulate connection loss after each round (no confirms ever fire)
            listener.onClose(mock(Connection.class));
            maxObservedAfterClear = Math.max(maxObservedAfterClear, traceIdMap().size());
        }

        // Then: entries from each failed round were evicted, not accumulated.
        // (Before fix: no eviction -> map would hold rounds*jobsPerRound = 20 entries.)
        assertTrue(traceIdMap().isEmpty(),
                "traceIdMap must be empty after the final connection loss (BUG-6)");
        assertEquals(0, maxObservedAfterClear,
                "after every connection loss the map must be empty — no accumulation across failures (BUG-6)");
    }

    @Test
    @DisplayName("AC6: publisher confirm still removes traceIdMap entry (non-regression; eviction callback does not interfere)")
    void publisherConfirm_stillRemovesTraceIdMapEntry_nonRegression() throws Exception {
        // Given: callbacks initialized (registers BOTH the confirm callback and the connection listener)
        jobQueueService.initPublisherCallbacks();

        ArgumentCaptor<RabbitTemplate.ConfirmCallback> confirmCaptor =
                ArgumentCaptor.forClass(RabbitTemplate.ConfirmCallback.class);
        verify(rabbitTemplate).setConfirmCallback(confirmCaptor.capture());
        RabbitTemplate.ConfirmCallback confirmCallback = confirmCaptor.getValue();

        ArgumentCaptor<ConnectionListener> listenerCaptor =
                ArgumentCaptor.forClass(ConnectionListener.class);
        verify(connectionFactory).addConnectionListener(listenerCaptor.capture());
        ConnectionListener listener = listenerCaptor.getValue();

        // and a job is published with traceId=T1
        MDC.put(AppConstants.MDC_TRACE_ID, "T1");
        UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        jobQueueService.publish(jobWithId(jobId));
        assertEquals("T1", traceIdMap().get(jobId.toString()),
                "precondition: publish() recorded the in-flight entry");

        // When: a successful publisher confirm fires (ack=true)
        confirmCallback.confirm(new CorrelationData(jobId.toString()), true, null);

        // Then: the confirm path still removes the entry (handlePublisherConfirm unchanged)
        assertFalse(traceIdMap().containsKey(jobId.toString()),
                "successful publisher confirm must remove the traceIdMap entry (non-regression)");

        // And: firing the eviction callback afterwards is a harmless no-op (no double-remove, no interference)
        listener.onClose(mock(Connection.class));
        assertTrue(traceIdMap().isEmpty(),
                "eviction callback after confirm must be a harmless no-op (no interference)");
    }
}
