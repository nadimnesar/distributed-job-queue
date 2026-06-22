package com.nadimnesar.jobqueue.common.repository;

import com.nadimnesar.jobqueue.common.entity.JobDependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface JobDependencyRepository extends JpaRepository<JobDependencyEntity, UUID> {

    @Modifying
    @Query("DELETE FROM JobDependencyEntity j WHERE j.jobId = ?1 AND j.dependencyId = ?2")
    void removeDependency(UUID jobId, UUID dependencyId);

    @Modifying
    @Query("DELETE FROM JobDependencyEntity j WHERE j.dependencyId = :jobId")
    void deleteByDependencyId(@Param("jobId") UUID jobId);

    @Query("SELECT j.dependencyId FROM JobDependencyEntity j WHERE j.jobId = ?1")
    Set<UUID> findByJobId(UUID jobId);

    @Query("SELECT j.jobId FROM JobDependencyEntity j WHERE j.dependencyId = ?1")
    Set<UUID> findDependents(UUID jobId);

    @Query("SELECT j FROM JobDependencyEntity j WHERE j.jobId IN :jobIds")
    List<JobDependencyEntity> findAllByJobIdIn(@Param("jobIds") Collection<UUID> jobIds);

    @Query("SELECT j FROM JobDependencyEntity j WHERE j.dependencyId IN :dependencyIds")
    List<JobDependencyEntity> findAllByDependencyIdIn(@Param("dependencyIds") Collection<UUID> dependencyIds);
}
