package com.nadimnesar.jobqueue.producer.dto.response;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JobResponse {
    private UUID id;
    private JobPriority priority;
    private JobStatus status;
    private JobType type;
    private String result;
    private String errorMessage;
    private Integer currentProgress;
    private Integer currentRetryAttemptCount;
    private Integer maxRetryAttemptCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
