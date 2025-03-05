package com.nadimnesar.jobqueue.producer.entity;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.producer.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "job")
public class JobEntity extends BaseEntity {

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobPriority priority = JobPriority.MEDIUM;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Enumerated(value = EnumType.STRING)
    private JobType type;

    @Column(nullable = false)
    private String payload;

    private String result;
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentProgress = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentRetryAttemptCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetryAttemptCount = 3;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
