package com.nadimnesar.jobqueue.producer.interceptor;

import com.nadimnesar.jobqueue.common.constant.Constants;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

public class TracingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TracingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String traceId = request.getHeader(Constants.B3_TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = TracingUtils.newTraceId();
        }

        String spanId = TracingUtils.newSpanId();

        MDC.put(Constants.MDC_TRACE_ID, traceId);
        MDC.put(Constants.MDC_SPAN_ID, spanId);

        logger.info("Incoming request - Method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        logger.info("Request completed - Status: {}", response.getStatus());
        MDC.remove(Constants.MDC_TRACE_ID);
        MDC.remove(Constants.MDC_SPAN_ID);
    }
}
