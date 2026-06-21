package com.nadimnesar.jobqueue.common.constant;

public class Constants {
    private Constants() {
    }

    public static final Integer DEFAULT_MAXIMUM_ATTEMPT_COUNT = 5;
    public static final Integer INITIAL_ATTEMPT_COUNT = 1;

    // Tracing
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
}
