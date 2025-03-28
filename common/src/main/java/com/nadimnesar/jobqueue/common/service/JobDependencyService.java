package com.nadimnesar.jobqueue.common.service;

import java.util.Set;
import java.util.UUID;

public interface JobDependencyService {
    void addDependency(UUID jobId, UUID dependencyId);

    void removeDependency(UUID jobId, UUID dependencyId);

    Set<UUID> getDependencies(UUID jobId);

    void setDependencies(UUID jobId, Set<UUID> dependencies);

    Set<UUID> getDependents(UUID jobId);
}
