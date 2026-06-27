package com.nadimnesar.jobqueue.producer.interceptor;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TracingInterceptorTest {

    private static final String TRACE_ID_FAIL = "T-fail";

    private HttpServletRequest request;
    private HttpServletResponse response;
    private Object handler;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        handler = new Object();
        // Ensure a clean MDC starting state for every test.
        TracingUtils.clearTracing();
    }

    @AfterEach
    void tearDown() {
        // Defensive: never let one test's MDC leak into another.
        TracingUtils.clearTracing();
    }

    @Test
    @DisplayName("AC5: when logIncomingRequest throws, exception propagates and MDC is cleared")
    void preHandle_whenLogThrows_clearsMdcAndRethrows() {
        // Arrange: incoming traceId header present; log seam throws.
        when(request.getHeader(AppConstants.HEADER_TRACE_ID)).thenReturn(TRACE_ID_FAIL);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/jobs");

        TracingInterceptor interceptor = new TracingInterceptor() {
            @Override
            void logIncomingRequest(HttpServletRequest request) {
                throw new IllegalStateException("simulated logging failure");
            }
        };

        // Act + Assert (a): the exception propagates (not swallowed).
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> interceptor.preHandle(request, response, handler));

        assertEquals("simulated logging failure", thrown.getMessage());

        // Assert (b): MDC no longer contains the failed request's traceId/spanId.
        assertNull(MDC.get(AppConstants.MDC_TRACE_ID),
                "traceId must be cleared from MDC after preHandle failure");
        assertNull(MDC.get(AppConstants.MDC_SPAN_ID),
                "spanId must be cleared from MDC after preHandle failure");
        // Sanity: the leaked value would have been T-fail.
        assertNotEquals(TRACE_ID_FAIL, MDC.get(AppConstants.MDC_TRACE_ID));
    }

    @Test
    @DisplayName("AC5 happy-path: successful preHandle sets MDC; afterCompletion clears it")
    void preHandle_success_setsMdc_andAfterCompletionClears() {
        // Arrange: incoming traceId header present.
        String expectedTraceId = "T-happy";
        when(request.getHeader(AppConstants.HEADER_TRACE_ID)).thenReturn(expectedTraceId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/jobs");

        TracingInterceptor interceptor = new TracingInterceptor();

        // Act: successful preHandle.
        boolean proceed = interceptor.preHandle(request, response, handler);

        // Assert: preHandle returned true and MDC is populated (NOT cleared prematurely).
        assertTrue(proceed, "preHandle should return true on the success path");
        assertEquals(expectedTraceId, MDC.get(AppConstants.MDC_TRACE_ID),
                "traceId must be set in MDC after successful preHandle");
        String spanId = MDC.get(AppConstants.MDC_SPAN_ID);
        assertNotEquals(null, spanId, "spanId must be set in MDC after successful preHandle");
        assertNotEquals("", spanId, "spanId must be non-blank");

        // Act: request completion -> afterCompletion clears MDC.
        interceptor.afterCompletion(request, response, handler, null);

        // Assert: MDC cleared on completion.
        assertNull(MDC.get(AppConstants.MDC_TRACE_ID),
                "traceId must be cleared after afterCompletion");
        assertNull(MDC.get(AppConstants.MDC_SPAN_ID),
                "spanId must be cleared after afterCompletion");
    }

    @Test
    @DisplayName("AC5 happy-path: absent traceId header -> generated traceId set in MDC")
    void preHandle_success_generatesTraceId_whenHeaderAbsent() {
        // Arrange: no traceId header.
        when(request.getHeader(AppConstants.HEADER_TRACE_ID)).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/health");

        TracingInterceptor interceptor = new TracingInterceptor();

        // Act
        boolean proceed = interceptor.preHandle(request, response, handler);

        // Assert
        assertTrue(proceed);
        String traceId = MDC.get(AppConstants.MDC_TRACE_ID);
        assertNotEquals(null, traceId, "a traceId should be generated when header is absent");
        assertNotEquals("", traceId, "generated traceId should be non-blank");
        assertNotEquals(null, MDC.get(AppConstants.MDC_SPAN_ID), "spanId should be set");

        interceptor.afterCompletion(request, response, handler, null);

        assertNull(MDC.get(AppConstants.MDC_TRACE_ID));
        assertNull(MDC.get(AppConstants.MDC_SPAN_ID));
    }

    @Test
    @DisplayName("AC5 thread-reuse: throwing preHandle does not leak traceId into next request on same thread")
    void preHandle_threadReuse_failedRequestDoesNotLeakTraceId() {
        // Arrange first (failing) request.
        when(request.getHeader(AppConstants.HEADER_TRACE_ID)).thenReturn(TRACE_ID_FAIL);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/jobs");

        TracingInterceptor failingInterceptor = new TracingInterceptor() {
            @Override
            void logIncomingRequest(HttpServletRequest request) {
                throw new IllegalStateException("simulated logging failure");
            }
        };

        // Act (1): first preHandle throws.
        assertThrows(IllegalStateException.class,
                () -> failingInterceptor.preHandle(request, response, handler));

        // Assert (1): no leak immediately after the failed request.
        assertNull(MDC.get(AppConstants.MDC_TRACE_ID),
                "failed request must not leak traceId into the reused thread's MDC");
        assertNull(MDC.get(AppConstants.MDC_SPAN_ID),
                "failed request must not leak spanId into the reused thread's MDC");

        // Arrange second (successful) request on the SAME thread (MDC is thread-local, so this
        // is the realistic simulation: the same thread runs the next request).
        HttpServletRequest secondRequest = mock(HttpServletRequest.class);
        when(secondRequest.getHeader(AppConstants.HEADER_TRACE_ID)).thenReturn(null);
        when(secondRequest.getMethod()).thenReturn("POST");
        when(secondRequest.getRequestURI()).thenReturn("/jobs");

        TracingInterceptor successInterceptor = new TracingInterceptor();

        // Act (2): second preHandle succeeds.
        boolean proceed = successInterceptor.preHandle(secondRequest, response, handler);

        // Assert (2): the second request's MDC does NOT contain the first request's T-fail.
        assertTrue(proceed);
        assertNotEquals(TRACE_ID_FAIL, MDC.get(AppConstants.MDC_TRACE_ID),
                "second request must not inherit the first request's T-fail traceId");
        String secondTraceId = MDC.get(AppConstants.MDC_TRACE_ID);
        assertNotEquals(null, secondTraceId, "second request should have its own traceId");
        assertNotEquals("", secondTraceId);

        // Cleanup.
        successInterceptor.afterCompletion(secondRequest, response, handler, null);
        assertNull(MDC.get(AppConstants.MDC_TRACE_ID));
        assertNull(MDC.get(AppConstants.MDC_SPAN_ID));
    }
}
