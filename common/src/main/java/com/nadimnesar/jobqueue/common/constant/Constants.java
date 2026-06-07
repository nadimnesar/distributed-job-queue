package com.nadimnesar.jobqueue.common.constant;

public class Constants {
    private Constants() {
    }

    public static final Integer MAXIMUM_ATTEMPT_COUNT = 5;

    // Tracing
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";
    public static final String B3_SPAN_ID_HEADER = "X-B3-SpanId";
}
