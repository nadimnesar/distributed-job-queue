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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

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
}
