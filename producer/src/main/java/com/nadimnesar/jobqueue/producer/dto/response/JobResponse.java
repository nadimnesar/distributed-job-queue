package com.nadimnesar.jobqueue.producer.dto.response;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class JobResponse {
    private UUID id;
    private JobPriority priority;
    private JobType type;
    private JobStatus status;
    private Set<UUID> dependents;
    private Set<UUID> dependencies;
    private String result;
    private Integer attemptCount;
    private Integer maxAttemptCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
