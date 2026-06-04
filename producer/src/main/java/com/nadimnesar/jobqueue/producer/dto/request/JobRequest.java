package com.nadimnesar.jobqueue.producer.dto.request;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotNull(message = "Job priority cannot be null")
    private JobPriority priority;

    @NotNull(message = "Job type cannot be null")
    private JobType type;

    @Builder.Default
    private Set<UUID> dependencies = new HashSet<>();

    @NotBlank(message = "Payload cannot be blank")
    private String payload;

    private Integer maxAttemptCount;
}
