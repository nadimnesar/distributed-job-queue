package com.nadimnesar.jobqueue.common.util;

import com.github.f4b6a3.uuid.UuidCreator;
import com.nadimnesar.jobqueue.common.constant.AppConstants;
import org.slf4j.MDC;

public final class TracingUtils {
    private TracingUtils() {
    }

    public static String newTraceId() {
        return UuidCreator.getTimeOrderedEpoch().toString().replace("-", "");
    }

    public static String newSpanId() {
        return UuidCreator.getTimeOrderedEpoch().toString().replace("-", "").substring(16);
    }

    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            MDC.remove(AppConstants.MDC_TRACE_ID);
        } else {
            MDC.put(AppConstants.MDC_TRACE_ID, traceId);
        }
    }

    public static void setSpanId(String spanId) {
        if (spanId == null || spanId.isBlank()) {
            MDC.remove(AppConstants.MDC_SPAN_ID);
        } else {
            MDC.put(AppConstants.MDC_SPAN_ID, spanId);
        }
    }

    public static void setNewSpanId() {
        String traceId = getTraceId();
        if (traceId != null && !traceId.isBlank()) {
            String spanId = newSpanId();
            setSpanId(spanId);
        }
    }

    public static String getTraceId() {
        return MDC.get(AppConstants.MDC_TRACE_ID);
    }

    public static void clearTracing() {
        MDC.remove(AppConstants.MDC_TRACE_ID);
        MDC.remove(AppConstants.MDC_SPAN_ID);
    }
}
