package com.nadimnesar.jobqueue.common.entity;

import com.nadimnesar.jobqueue.common.constant.DbConstants;
import com.nadimnesar.jobqueue.common.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = DbConstants.JobDependency.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(columnNames = {
                DbConstants.JobDependency.JOB_ID, DbConstants.JobDependency.DEPENDENCY_ID
        })
)
public class JobDependencyEntity extends BaseEntity {
    @Column(name = DbConstants.JobDependency.JOB_ID, nullable = false)
    private UUID jobId;

    @Column(name = DbConstants.JobDependency.DEPENDENCY_ID, nullable = false)
    private UUID dependencyId;
}
