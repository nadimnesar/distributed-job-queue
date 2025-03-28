package com.nadimnesar.jobqueue.producer.dto.request;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class JobRequest {
    private JobPriority priority;
    private JobType type;
    private Set<UUID> dependencies = new HashSet<>();
    private String payload;
    private Integer maxRetryAttemptCount;
}
