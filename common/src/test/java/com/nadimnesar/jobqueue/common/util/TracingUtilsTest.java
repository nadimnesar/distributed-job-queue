package com.nadimnesar.jobqueue.common.util;

import com.nadimnesar.jobqueue.common.constant.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class TracingUtilsTest {

    @BeforeEach
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void newTraceId_shouldReturn32HexChars() {
        String traceId = TracingUtils.newTraceId();
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertTrue(traceId.matches("[0-9a-f]{32}"), "traceId must be 32 lowercase hex chars");
    }

    @Test
    void newTraceId_shouldBeUnique() {
        String first = TracingUtils.newTraceId();
        String second = TracingUtils.newTraceId();
        assertNotEquals(first, second);
    }

    @Test
    void newSpanId_shouldReturn16HexChars() {
        String spanId = TracingUtils.newSpanId();
        assertNotNull(spanId);
        assertEquals(16, spanId.length());
        assertTrue(spanId.matches("[0-9a-f]{16}"), "spanId must be 16 lowercase hex chars");
    }

    @Test
    void newSpanId_shouldBeUnique() {
        String first = TracingUtils.newSpanId();
        String second = TracingUtils.newSpanId();
        assertNotEquals(first, second);
    }

    @Test
    void getTraceId_shouldReturnNullWhenNotSet() {
        assertNull(TracingUtils.getTraceId());
    }

    @Test
    void getSpanId_shouldReturnNullWhenNotSet() {
        assertNull(TracingUtils.getSpanId());
    }

    @Test
    void setTraceId_shouldStoreAndRetrieveValue() {
        String traceId = "test-trace-id";
        TracingUtils.setTraceId(traceId);
        assertEquals(traceId, TracingUtils.getTraceId());
    }

    @Test
    void setTraceId_shouldIgnoreNull() {
        TracingUtils.setTraceId("some-id");
        TracingUtils.setTraceId(null);
        assertEquals("some-id", TracingUtils.getTraceId());
    }

    @Test
    void setSpanId_shouldStoreAndRetrieveValue() {
        String spanId = "test-span-id";
        TracingUtils.setSpanId(spanId);
        assertEquals(spanId, TracingUtils.getSpanId());
    }

    @Test
    void setSpanId_shouldIgnoreNull() {
        TracingUtils.setSpanId("some-id");
        TracingUtils.setSpanId(null);
        assertEquals("some-id", TracingUtils.getSpanId());
    }

    @Test
    void clearTracing_shouldRemoveBothIds() {
        TracingUtils.setTraceId("trace");
        TracingUtils.setSpanId("span");
        TracingUtils.clearTracing();
        assertNull(TracingUtils.getTraceId());
        assertNull(TracingUtils.getSpanId());
    }

    @Test
    void clearTracing_shouldNotThrowWhenAlreadyEmpty() {
        assertDoesNotThrow(TracingUtils::clearTracing);
    }

    @Test
    void setAndClear_roundTrip() {
        String traceId = TracingUtils.newTraceId();
        String spanId = TracingUtils.newSpanId();

        TracingUtils.setTraceId(traceId);
        TracingUtils.setSpanId(spanId);

        assertEquals(traceId, TracingUtils.getTraceId());
        assertEquals(spanId, TracingUtils.getSpanId());

        TracingUtils.clearTracing();
        assertNull(TracingUtils.getTraceId());
        assertNull(TracingUtils.getSpanId());
    }

    @Test
    void mdcKeys_shouldMatchConstants() {
        TracingUtils.setTraceId("abc");
        TracingUtils.setSpanId("def");

        assertEquals("abc", MDC.get(Constants.MDC_TRACE_ID));
        assertEquals("def", MDC.get(Constants.MDC_SPAN_ID));
    }
}
