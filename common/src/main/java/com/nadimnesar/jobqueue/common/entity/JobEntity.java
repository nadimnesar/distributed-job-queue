package com.nadimnesar.jobqueue.common.entity;

import com.nadimnesar.jobqueue.common.constant.Constants;
import com.nadimnesar.jobqueue.common.constant.DbConstants;
import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.base.BaseEntity;
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
@Table(name = DbConstants.Job.TABLE_NAME)
public class JobEntity extends BaseEntity {

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "VARCHAR(50)",
            name = DbConstants.Job.PRIORITY)
    @Builder.Default
    private JobPriority priority = JobPriority.MEDIUM;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "VARCHAR(50)",
            name = DbConstants.Job.TYPE)
    private JobType type;

    @Column(nullable = false,
            columnDefinition = "TEXT",
            name = DbConstants.Job.PAYLOAD)
    private String payload;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "VARCHAR(50)",
            name = DbConstants.Job.STATUS)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(columnDefinition = "TEXT",
            name = DbConstants.Job.RESULT)
    private String result;

    @Column(nullable = false,
            columnDefinition = "INT",
            name = DbConstants.Job.ATTEMPT_COUNT)
    @Builder.Default
    private Integer attemptCount = Constants.INITIAL_ATTEMPT_COUNT;

    @Column(nullable = false,
            columnDefinition = "INT",
            name = DbConstants.Job.MAX_ATTEMPT_COUNT)
    @Builder.Default
    private Integer maxAttemptCount = Constants.DEFAULT_MAXIMUM_ATTEMPT_COUNT;

    @Column(columnDefinition = "TIMESTAMP",
            name = DbConstants.Job.STARTED_AT)
    private LocalDateTime startedAt;

    @Column(columnDefinition = "TIMESTAMP",
            name = DbConstants.Job.COMPLETED_AT)
    private LocalDateTime completedAt;
}
