package com.nadimnesar.jobqueue.common.repository;

import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.projection.JobStatusCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Page<JobEntity> findByStatus(JobStatus status, Pageable pageable);

    Page<JobEntity> findByType(JobType type, Pageable pageable);

    Page<JobEntity> findByStatusAndType(JobStatus status, JobType type, Pageable pageable);

    @Query("SELECT j.status AS status, COUNT(j) AS count " +
            "FROM JobEntity j GROUP BY j.status")
    List<JobStatusCountProjection> countJobsGroupedByStatus();
}
