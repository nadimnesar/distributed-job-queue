package com.nadimnesar.jobqueue.common.repository;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    @Query("SELECT j FROM JobEntity j WHERE j.attemptCount < j.maxAttemptCount " +
            "AND j.status != 'CANCELED' AND j.status != 'COMPLETED' ORDER BY j.createdAt ASC")
    List<JobEntity> findJobsToRetry();

    @Query("SELECT COUNT(j) FROM JobEntity j WHERE j.status = ?1")
    long countByStatus(JobStatus status);

    @Query("SELECT j FROM JobEntity j WHERE j.status = 'PENDING' ORDER BY " +
            "CASE j.priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'LOW' THEN 2 END, " +
            "j.createdAt ASC")
    Optional<JobEntity> findNextPendingJob();

    @Query("SELECT j FROM JobEntity j WHERE j.status = 'FAILED' AND j.attemptCount >= j.maxAttemptCount")
    List<JobEntity> findDeadLetterJobs();

    @Query("SELECT COUNT(j) FROM JobEntity j WHERE j.status = 'PENDING' AND j.priority = ?1")
    long countPendingByPriority(JobPriority priority);

    List<JobEntity> findByStatus(JobStatus status);

    List<JobEntity> findByType(JobType type);

    List<JobEntity> findByStatusAndType(JobStatus status, JobType type);
}
