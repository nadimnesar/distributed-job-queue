package com.nadimnesar.jobqueue.common.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkerHealth implements Serializable {

    @Serial
    private static final long serialVersionUID = 5378180751100751593L;

    private UUID id;
    private double cpuLoad;
    private double memoryUsagePercentage;
    private int availableProcessors;
    private long heapMemoryUsage;
    private long maxHeapMemory;

    @Builder.Default
    private LocalDateTime lastHeartbeatTime = LocalDateTime.now();
}
