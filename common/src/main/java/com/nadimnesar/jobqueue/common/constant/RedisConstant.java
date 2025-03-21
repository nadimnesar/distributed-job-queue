package com.nadimnesar.jobqueue.common.constant;

public class RedisConstant {
    private RedisConstant() {
    }

    public static final String HIGH_PRIORITY_JOB_QUEUE_KEY = "job:queue:high";
    public static final String MEDIUM_PRIORITY_JOB_QUEUE_KEY = "job:queue:medium";
    public static final String LOW_PRIORITY_JOB_QUEUE_KEY = "job:queue:low";
    public static final String DEAD_LETTER_QUEUE_KEY = "job:queue:dead";
    public static final String JOB_PROCESSING_STATE = "job:processing:";
}
