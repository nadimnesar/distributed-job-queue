package com.nadimnesar.jobqueue.common.entity;

import com.nadimnesar.jobqueue.common.constant.DbConstant;
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
@Table(name = DbConstant.Job.TABLE_NAME)
public class JobEntity extends BaseEntity {

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "VARCHAR(50)",
            name = DbConstant.Job.PRIORITY)
    @Builder.Default
    private JobPriority priority = JobPriority.MEDIUM;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "VARCHAR(50)",
            name = DbConstant.Job.STATUS)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "VARCHAR(50)",
            name = DbConstant.Job.TYPE)
    private JobType type;

    @Column(nullable = false,
            columnDefinition = "TEXT",
            name = DbConstant.Job.PAYLOAD)
    private String payload;

    @Column(columnDefinition = "TEXT",
            name = DbConstant.Job.RESULT)
    private String result;

    @Column(columnDefinition = "TEXT",
            name = DbConstant.Job.ERROR_MESSAGE)
    private String errorMessage;

    @Column(nullable = false,
            columnDefinition = "INT",
            name = DbConstant.Job.CURRENT_PROGRESS)
    @Builder.Default
    private Integer currentProgress = 0;

    @Column(nullable = false,
            columnDefinition = "INT",
            name = DbConstant.Job.CURRENT_ATTEMPT_COUNT)
    @Builder.Default
    private Integer currentAttemptCount = 0;

    @Column(nullable = false,
            columnDefinition = "INT",
            name = DbConstant.Job.MAX_ATTEMPT_COUNT)
    @Builder.Default
    private Integer maxAttemptCount = 3;

    @Column(columnDefinition = "TIMESTAMP",
            name = DbConstant.Job.STARTED_AT)
    private LocalDateTime startedAt;

    @Column(columnDefinition = "TIMESTAMP",
            name = DbConstant.Job.COMPLETED_AT)
    private LocalDateTime completedAt;
}
