package com.nadimnesar.jobqueue.common.constant;

public class AppConstants {
    private AppConstants() {
    }

    public static final Integer DEFAULT_MAXIMUM_ATTEMPT_COUNT = 5;
    public static final Integer INITIAL_ATTEMPT_COUNT = 1;
    public static final Integer STARTUP_CHECK_MAX_ATTEMPTS = 3;
    public static final Integer STARTUP_CHECK_RETRY_SLEEP_MS = 3000;

    // Tracing
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String HEADER_TRACE_ID = "traceId";
}
