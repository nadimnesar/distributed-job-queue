package com.nadimnesar.jobqueue.common.dto;

public record ConsumedDlqMessage(String jobId, String traceId) {
}
