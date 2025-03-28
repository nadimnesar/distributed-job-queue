package com.nadimnesar.jobqueue.common.repository;

import com.nadimnesar.jobqueue.common.entity.JobDependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;
import java.util.UUID;

public interface JobDependencyRepository extends JpaRepository<JobDependencyEntity, UUID> {

    @Modifying
    @Query("DELETE FROM JobDependencyEntity j WHERE j.jobId = ?1 AND j.dependencyId = ?2")
    void removeDependency(UUID jobId, UUID dependencyId);

    @Query("SELECT j.dependencyId FROM JobDependencyEntity j WHERE j.jobId = ?1")
    Set<UUID> findByJobId(UUID jobId);

    @Query("SELECT j.jobId FROM JobDependencyEntity j WHERE j.dependencyId = ?1")
    Set<UUID> findDependents(UUID jobId);
}
