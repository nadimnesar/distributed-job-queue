package com.nadimnesar.jobqueue.common.repository;

import com.nadimnesar.jobqueue.common.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    @Query("SELECT j FROM JobEntity j WHERE j.currentRetryAttemptCount < j.maxRetryAttemptCount " +
            "AND j.status != 'CANCELLED' AND j.status != 'COMPLETED'")
    List<JobEntity> findJobsToRetry();

}
