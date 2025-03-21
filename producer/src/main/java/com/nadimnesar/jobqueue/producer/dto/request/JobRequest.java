package com.nadimnesar.jobqueue.producer.dto.request;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import lombok.Data;

@Data
public class JobRequest {
    private JobType type;
    private JobPriority priority;
    private String payload;
    private Integer maxRetryAttemptCount;
}
