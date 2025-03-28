package com.nadimnesar.jobqueue.common.entity;

import com.nadimnesar.jobqueue.common.constant.DbConstant;
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
@Table(name = DbConstant.JobDependency.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(columnNames = {
                DbConstant.JobDependency.JOB_ID, DbConstant.JobDependency.DEPENDENCY_ID
        })
)
public class JobDependencyEntity extends BaseEntity {
    @Column(name = DbConstant.JobDependency.JOB_ID, nullable = false)
    private UUID jobId;

    @Column(name = DbConstant.JobDependency.DEPENDENCY_ID, nullable = false)
    private UUID dependencyId;
}
