package com.nadimnesar.jobqueue.common.service.impl;

import com.nadimnesar.jobqueue.common.entity.JobDependencyEntity;
import com.nadimnesar.jobqueue.common.repository.JobDependencyRepository;
import com.nadimnesar.jobqueue.common.service.JobDependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobDependencyServiceImpl implements JobDependencyService {

    public final JobDependencyRepository jobDependencyRepository;

    @Transactional(readOnly = true)
    @Override
    public Set<UUID> getDependencies(UUID jobId) {
        return jobDependencyRepository.findByJobId(jobId);
    }

    @Transactional
    @Override
    public void setDependencies(UUID jobId, Set<UUID> dependencies) {
        dependencies.forEach(dependencyId -> addDependency(jobId, dependencyId));
    }

    @Transactional(readOnly = true)
    @Override
    public Set<UUID> getDependents(UUID jobId) {
        return jobDependencyRepository.findDependents(jobId);
    }

    @Transactional
    @Override
    public void informDependents(UUID jobId) {
        getDependents(jobId).forEach(dependentJobId -> {
            removeDependency(dependentJobId, jobId);
        });
    }

    private void addDependency(UUID jobId, UUID dependencyId) {
        jobDependencyRepository.save(JobDependencyEntity.builder()
                .jobId(jobId)
                .dependencyId(dependencyId)
                .build());
    }

    private void removeDependency(UUID jobId, UUID dependencyId) {
        jobDependencyRepository.removeDependency(jobId, dependencyId);
    }
}
