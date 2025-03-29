package com.nadimnesar.jobqueue.common.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class WorkerHealth {
    private UUID workerId;
    private double cpuLoad;
    private double memoryUsagePercentage;
    private int availableProcessors;
    private long heapMemoryUsage;
    private long maxHeapMemory;
    private long timestamp;
}
