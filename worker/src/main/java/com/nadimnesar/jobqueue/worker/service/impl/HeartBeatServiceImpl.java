package com.nadimnesar.jobqueue.worker.service.impl;

import com.nadimnesar.jobqueue.common.constant.RedisConstant;
import com.nadimnesar.jobqueue.common.dto.WorkerHealth;
import com.nadimnesar.jobqueue.worker.config.AppConfig.WorkerContext;
import com.nadimnesar.jobqueue.worker.service.HeartBeatService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HeartBeatServiceImpl implements HeartBeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartBeatServiceImpl.class);

    private final WorkerContext workerContext;
    private final RedissonClient redissonClient;

    @Override
    @Scheduled(cron = "${schedule.cron.heartbeat}")
    public void sendHeartbeat() {
        var workerId = workerContext.workerId();

        if (workerId == null) {
            logger.warn("Worker ID is not initialized. Skipping heartbeat.");
            return;
        }

        logger.debug("Sending heartbeat for worker: {}, at: {}", workerId, LocalDateTime.now());
        try {
            String key = RedisConstant.REDIS_WORKER_HEALTH_KEY_PREFIX + workerId +
                    RedisConstant.REDIS_WORKER_HEALTH_KEY_SUFFIX;

            RBucket<WorkerHealth> bucket = redissonClient.getBucket(key);

            WorkerHealth health = collectHealthMetrics();
            bucket.set(health, Duration.ofSeconds(10));

            logger.debug("Heartbeat sent for worker: {}", workerId);
        } catch (Exception e) {
            logger.error("Failed to send heartbeat, error: {}", e.getMessage(), e);
        }
    }

    private WorkerHealth collectHealthMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        double cpuLoad = osBean.getSystemLoadAverage();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory;

        return WorkerHealth.builder()
                .id(workerContext.workerId())
                .cpuLoad(cpuLoad)
                .memoryUsagePercentage(memoryUsage * 100)
                .availableProcessors(osBean.getAvailableProcessors())
                .heapMemoryUsage(memoryBean.getHeapMemoryUsage().getUsed())
                .maxHeapMemory(memoryBean.getHeapMemoryUsage().getMax())
                .build();
    }
}
