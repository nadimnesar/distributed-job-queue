package com.nadimnesar.jobqueue.common.util;

import com.github.f4b6a3.uuid.UuidCreator;
import com.nadimnesar.jobqueue.common.constant.Constants;
import org.slf4j.MDC;

public final class TracingUtils {
    private TracingUtils() {
    }



    public static String newTraceId() {
        return UuidCreator.getTimeOrderedEpoch().toString().replace("-", "");
    }

    public static String newSpanId() {
        return UuidCreator.getRandomBased().toString().replace("-", "").substring(16);
    }

    public static String getTraceId() {
        return MDC.get(Constants.MDC_TRACE_ID);
    }

    public static String getSpanId() {
        return MDC.get(Constants.MDC_SPAN_ID);
    }

    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(Constants.MDC_TRACE_ID, traceId);
        }
    }

    public static void setSpanId(String spanId) {
        if (spanId != null) {
            MDC.put(Constants.MDC_SPAN_ID, spanId);
        }
    }

    public static void clearTracing() {
        MDC.remove(Constants.MDC_TRACE_ID);
        MDC.remove(Constants.MDC_SPAN_ID);
    }
}
