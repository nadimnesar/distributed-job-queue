package com.nadimnesar.jobqueue.producer.repository;

import com.nadimnesar.jobqueue.common.constant.enums.JobPriority;
import com.nadimnesar.jobqueue.common.constant.enums.JobStatus;
import com.nadimnesar.jobqueue.common.constant.enums.JobType;
import com.nadimnesar.jobqueue.common.entity.JobDependencyEntity;
import com.nadimnesar.jobqueue.common.entity.JobEntity;
import com.nadimnesar.jobqueue.common.repository.JobDependencyRepository;
import com.nadimnesar.jobqueue.common.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class JobDependencyRepositoryTest {

    @Autowired
    private JobDependencyRepository jobDependencyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("deleteStaleDependenciesByJobId removes COMPLETED and orphan rows for the target job only")
    void deleteStaleDependenciesByJobId_removesCompletedAndOrphanRowsForTargetJobOnly() {
        // Given: one target job and one other job, plus COMPLETED, PENDING, and orphan dependencies
        UUID targetJobId = UUID.randomUUID();
        UUID otherJobId = UUID.randomUUID();

        JobEntity completedJob = JobEntity.builder()
                .status(JobStatus.COMPLETED)
                .priority(JobPriority.MEDIUM)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .build();

        JobEntity pendingJob = JobEntity.builder()
                .status(JobStatus.PENDING)
                .priority(JobPriority.MEDIUM)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .build();

        completedJob = jobRepository.save(completedJob);
        pendingJob = jobRepository.save(pendingJob);

        UUID orphanDependencyId = UUID.randomUUID();

        JobDependencyEntity targetCompletedDep = JobDependencyEntity.builder()
                .jobId(targetJobId)
                .dependencyId(completedJob.getId())
                .build();

        JobDependencyEntity targetPendingDep = JobDependencyEntity.builder()
                .jobId(targetJobId)
                .dependencyId(pendingJob.getId())
                .build();

        JobDependencyEntity targetOrphanDep = JobDependencyEntity.builder()
                .jobId(targetJobId)
                .dependencyId(orphanDependencyId)
                .build();

        JobDependencyEntity otherCompletedDep = JobDependencyEntity.builder()
                .jobId(otherJobId)
                .dependencyId(completedJob.getId())
                .build();

        JobDependencyEntity otherOrphanDep = JobDependencyEntity.builder()
                .jobId(otherJobId)
                .dependencyId(UUID.randomUUID())
                .build();

        jobDependencyRepository.saveAll(List.of(
                targetCompletedDep, targetPendingDep, targetOrphanDep,
                otherCompletedDep, otherOrphanDep));

        // When
        int deletedCount = jobDependencyRepository.deleteStaleDependenciesByJobId(targetJobId);

        // Then: only the target job's stale dependencies are deleted
        assertEquals(2, deletedCount,
                "Expected one COMPLETED and one orphan dependency for the target job to be deleted");
        assertEquals(3, jobDependencyRepository.count(),
                "Target PENDING dependency plus both other-job dependencies should remain");
        assertTrue(jobDependencyRepository.existsById(targetPendingDep.getId()),
                "The target job's PENDING dependency must be preserved");
        assertTrue(jobDependencyRepository.existsById(otherCompletedDep.getId()),
                "Other jobs' stale dependencies must not be touched");
        assertTrue(jobDependencyRepository.existsById(otherOrphanDep.getId()),
                "Other jobs' orphan dependencies must not be touched");

        Set<UUID> remainingTargetDeps = jobDependencyRepository.findByJobId(targetJobId);
        assertEquals(1, remainingTargetDeps.size(),
                "Target job should have exactly one remaining dependency");
        assertTrue(remainingTargetDeps.contains(pendingJob.getId()),
                "Target job's remaining dependency must be the PENDING job");
    }

    @Test
    @DisplayName("deleteStaleDependenciesByJobId returns zero when no matching rows exist")
    void deleteStaleDependenciesByJobId_returnsZeroWhenNoRowsMatch() {
        // Given: dependency rows belong to a different job
        UUID targetJobId = UUID.randomUUID();
        UUID otherJobId = UUID.randomUUID();

        JobEntity completedJob = JobEntity.builder()
                .status(JobStatus.COMPLETED)
                .priority(JobPriority.MEDIUM)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .build();
        completedJob = jobRepository.save(completedJob);

        JobDependencyEntity otherCompletedDep = JobDependencyEntity.builder()
                .jobId(otherJobId)
                .dependencyId(completedJob.getId())
                .build();
        jobDependencyRepository.save(otherCompletedDep);

        // When / Then
        int deletedCount = jobDependencyRepository.deleteStaleDependenciesByJobId(targetJobId);
        assertEquals(0, deletedCount, "Expected zero deletions for a job with no stale dependencies");
        assertEquals(1, jobDependencyRepository.count(),
                "Other jobs' dependency rows must remain untouched");
    }

    @Test
    @DisplayName("deleteStaleDependenciesByJobId is idempotent")
    void deleteStaleDependenciesByJobId_isIdempotent() {
        // Given: one target job with one stale dependency
        UUID targetJobId = UUID.randomUUID();

        JobEntity completedJob = JobEntity.builder()
                .status(JobStatus.COMPLETED)
                .priority(JobPriority.MEDIUM)
                .type(JobType.EMAIL_SENDING)
                .payload("{}")
                .build();
        completedJob = jobRepository.save(completedJob);

        JobDependencyEntity completedDep = JobDependencyEntity.builder()
                .jobId(targetJobId)
                .dependencyId(completedJob.getId())
                .build();
        jobDependencyRepository.save(completedDep);

        // When: cleanup runs twice for the same job
        int firstRun = jobDependencyRepository.deleteStaleDependenciesByJobId(targetJobId);
        int secondRun = jobDependencyRepository.deleteStaleDependenciesByJobId(targetJobId);

        // Then
        assertEquals(1, firstRun);
        assertEquals(0, secondRun, "Second cleanup for the same job should be a no-op");
        assertEquals(0, jobDependencyRepository.count());
    }
}
