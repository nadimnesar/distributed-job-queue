package com.nadimnesar.jobqueue.common.service;

import java.util.Set;
import java.util.UUID;

public interface JobDependencyService {
    Set<UUID> getDependencies(UUID jobId);

    void setDependencies(UUID jobId, Set<UUID> dependencies);

    Set<UUID> getDependents(UUID jobId);

    void informDependents(UUID jobId);
}
