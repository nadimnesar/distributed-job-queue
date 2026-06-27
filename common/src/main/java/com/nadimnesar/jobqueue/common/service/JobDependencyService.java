package com.nadimnesar.jobqueue.common.service;

import com.nadimnesar.jobqueue.common.entity.JobDependencyEntity;
import com.nadimnesar.jobqueue.common.repository.JobDependencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobDependencyService {

    private final JobDependencyRepository jobDependencyRepository;

    @Transactional(readOnly = true)
    public Set<UUID> getDependencies(UUID jobId) {
        return jobDependencyRepository.findByJobId(jobId);
    }

    @Transactional
    public void setDependencies(UUID jobId, Set<UUID> dependencies) {
        assertNoCycle(jobId, dependencies);
        jobDependencyRepository.findByJobId(jobId)
                .forEach(dep -> removeDependency(jobId, dep));
        dependencies.forEach(dependencyId -> addDependency(jobId, dependencyId));
    }

    @Transactional(readOnly = true)
    public Set<UUID> getDependents(UUID jobId) {
        return jobDependencyRepository.findDependents(jobId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Set<UUID>> getDependenciesBatch(Collection<UUID> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<JobDependencyEntity> rows = jobDependencyRepository.findAllByJobIdIn(jobIds);
        return rows.stream().collect(Collectors.groupingBy(
                JobDependencyEntity::getJobId,
                Collectors.mapping(JobDependencyEntity::getDependencyId, Collectors.toSet())
        ));
    }

    @Transactional(readOnly = true)
    public Map<UUID, Set<UUID>> getDependentsBatch(Collection<UUID> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<JobDependencyEntity> rows = jobDependencyRepository.findAllByDependencyIdIn(jobIds);
        return rows.stream().collect(Collectors.groupingBy(
                JobDependencyEntity::getDependencyId,
                Collectors.mapping(JobDependencyEntity::getJobId, Collectors.toSet())
        ));
    }

    @Transactional
    public void informDependents(UUID jobId) {
        jobDependencyRepository.deleteByDependencyId(jobId);
    }

    @Transactional
    public int cleanupStaleDependencies(UUID jobId) {
        return jobDependencyRepository.deleteStaleDependenciesByJobId(jobId);
    }

    private void addDependency(UUID jobId, UUID dependencyId) {
        if (jobId.equals(dependencyId)) {
            throw new IllegalArgumentException("A job cannot depend on itself: " + jobId);
        }
        jobDependencyRepository.save(JobDependencyEntity.builder()
                .jobId(jobId)
                .dependencyId(dependencyId)
                .build());
    }

    private void assertNoCycle(UUID jobId, Set<UUID> dependencies) {
        for (UUID dependencyId : dependencies) {
            if (jobId.equals(dependencyId)) {
                throw new IllegalArgumentException("A job cannot depend on itself: " + jobId);
            }
            if (createsCycle(jobId, dependencyId, new HashSet<>())) {
                throw new IllegalArgumentException(
                        "Circular dependency detected: adding dependency " + dependencyId
                                + " to job " + jobId + " would create a cycle");
            }
        }
    }

    private boolean createsCycle(UUID target, UUID start, Set<UUID> visited) {
        if (start.equals(target)) {
            return true;
        }
        if (!visited.add(start)) {
            return false;
        }
        for (UUID next : jobDependencyRepository.findByJobId(start)) {
            if (createsCycle(target, next, visited)) {
                return true;
            }
        }
        return false;
    }

    private void removeDependency(UUID jobId, UUID dependencyId) {
        jobDependencyRepository.removeDependency(jobId, dependencyId);
    }
}
