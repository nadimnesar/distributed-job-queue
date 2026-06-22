package com.nadimnesar.jobqueue.producer.interceptor;

import com.nadimnesar.jobqueue.common.constant.AppConstants;
import com.nadimnesar.jobqueue.common.util.TracingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TracingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        // Honor incoming traceId header; generate a new one only when absent or blank.
        String traceId = request.getHeader(AppConstants.HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = TracingUtils.newTraceId();
        }
        String spanId = TracingUtils.newSpanId();

        MDC.put(AppConstants.MDC_TRACE_ID, traceId);
        MDC.put(AppConstants.MDC_SPAN_ID, spanId);

        log.info("Incoming request - Method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        log.info("Request completed - Status: {}", response.getStatus());
        MDC.remove(AppConstants.MDC_TRACE_ID);
        MDC.remove(AppConstants.MDC_SPAN_ID);
    }
}
