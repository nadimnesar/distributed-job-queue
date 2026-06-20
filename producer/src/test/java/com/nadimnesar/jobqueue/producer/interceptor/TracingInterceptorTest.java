package com.nadimnesar.jobqueue.producer.interceptor;

import com.nadimnesar.jobqueue.common.constant.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TracingInterceptorTest {

    private TracingInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @BeforeEach
    void setUp() {
        interceptor = new TracingInterceptor();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void preHandle_shouldGenerateTraceIdAndSpanId_whenNoB3Headers() {
        when(request.getHeader(Constants.B3_TRACE_ID_HEADER)).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);

        // Verify MDC was populated
        assertNotNull(MDC.get(Constants.MDC_TRACE_ID));
        assertNotNull(MDC.get(Constants.MDC_SPAN_ID));
        assertEquals(32, MDC.get(Constants.MDC_TRACE_ID).length());
        assertEquals(16, MDC.get(Constants.MDC_SPAN_ID).length());
    }

    @Test
    void preHandle_shouldReuseIncomingTraceId_whenB3HeaderPresent() {
        String incomingTraceId = "incoming-trace-id-1234567890abcdef1234567890abcdef";
        when(request.getHeader(Constants.B3_TRACE_ID_HEADER)).thenReturn(incomingTraceId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/job/create");

        interceptor.preHandle(request, response, handler);

        // MDC should use the incoming trace ID, not generate a new one
        assertEquals(incomingTraceId, MDC.get(Constants.MDC_TRACE_ID));

        // Span ID should still be newly generated
        String spanId = MDC.get(Constants.MDC_SPAN_ID);
        assertNotNull(spanId);
        assertEquals(16, spanId.length());
    }

    @Test
    void preHandle_shouldHandleBlankIncomingTraceId() {
        when(request.getHeader(Constants.B3_TRACE_ID_HEADER)).thenReturn("   ");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        interceptor.preHandle(request, response, handler);

        // Should generate a new trace ID when incoming is blank
        String traceId = MDC.get(Constants.MDC_TRACE_ID);
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
    }

    @Test
    void afterCompletion_shouldCleanMdc() {
        // Set up MDC first
        MDC.put(Constants.MDC_TRACE_ID, "test-trace");
        MDC.put(Constants.MDC_SPAN_ID, "test-span");

        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, handler, null);

        // MDC should be cleaned
        assertNull(MDC.get(Constants.MDC_TRACE_ID));
        assertNull(MDC.get(Constants.MDC_SPAN_ID));
    }

    @Test
    void afterCompletion_shouldNotThrow_whenMdcAlreadyEmpty() {
        when(response.getStatus()).thenReturn(200);

        assertDoesNotThrow(() ->
                interceptor.afterCompletion(request, response, handler, null)
        );
    }

    @Test
    void afterCompletion_shouldLogError_whenExceptionPresent() {
        MDC.put(Constants.MDC_TRACE_ID, "trace");
        MDC.put(Constants.MDC_SPAN_ID, "span");
        when(response.getStatus()).thenReturn(500);

        Exception ex = new RuntimeException("test error");
        assertDoesNotThrow(() ->
                interceptor.afterCompletion(request, response, handler, ex)
        );

        assertNull(MDC.get(Constants.MDC_TRACE_ID));
        assertNull(MDC.get(Constants.MDC_SPAN_ID));
    }

    @Test
    void preHandleAndAfterCompletion_shouldBePaired() {
        // Simulate a full request lifecycle
        when(request.getHeader(Constants.B3_TRACE_ID_HEADER)).thenReturn(null);
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/job/123");
        when(response.getStatus()).thenReturn(204);

        // preHandle
        interceptor.preHandle(request, response, handler);
        String traceId = MDC.get(Constants.MDC_TRACE_ID);
        String spanId = MDC.get(Constants.MDC_SPAN_ID);
        assertNotNull(traceId);
        assertNotNull(spanId);

        // afterCompletion
        interceptor.afterCompletion(request, response, handler, null);
        assertNull(MDC.get(Constants.MDC_TRACE_ID));
        assertNull(MDC.get(Constants.MDC_SPAN_ID));
    }
}
