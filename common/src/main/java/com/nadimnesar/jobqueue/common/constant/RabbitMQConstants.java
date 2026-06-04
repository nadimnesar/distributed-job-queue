package com.nadimnesar.jobqueue.common.constant;

public class RabbitMQConstants {
    private RabbitMQConstants() {
    }

    public static final String EXCHANGE = "job.exchange";
    public static final String QUEUE_HIGH = "queue.high";
    public static final String QUEUE_MEDIUM = "queue.medium";
    public static final String QUEUE_LOW = "queue.low";
    public static final String QUEUE_RETRY = "queue.retry";
    public static final String QUEUE_DLQ = "queue.dlq";

    public static final String QUEUE_TYPE_QUORUM = "quorum";
    public static final String QUEUE_TYPE_CLASSIC = "classic";
    public static final String DEAD_LETTER_STRATEGY_AT_LEAST_ONCE = "at-least-once";
    public static final String OVERFLOW_REJECT_PUBLISH = "reject-publish";

    public static final int QUORUM_INITIAL_GROUP_SIZE = 3;
}
